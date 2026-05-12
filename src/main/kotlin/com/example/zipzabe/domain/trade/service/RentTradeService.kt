package com.example.zipzabe.domain.trade.service

import com.example.zipzabe.domain.analysis.repository.AnalysisRequestRepository
import com.example.zipzabe.domain.building.repository.BuildingLedgerRepository
import com.example.zipzabe.domain.property.entity.Property
import com.example.zipzabe.domain.trade.dto.MolitRentApiResponse
import com.example.zipzabe.domain.trade.dto.RentTradeFetchResponse
import com.example.zipzabe.domain.trade.dto.RentTradeListResponse
import com.example.zipzabe.domain.trade.dto.RentTradeRecordResponse
import com.example.zipzabe.domain.trade.entity.BuildingType
import com.example.zipzabe.domain.trade.entity.ContractClassification
import com.example.zipzabe.domain.trade.entity.ContractType
import com.example.zipzabe.domain.trade.entity.TradeRecord
import com.example.zipzabe.domain.trade.repository.TradeRecordRepository
import com.example.zipzabe.global.error.exception.AnalysisRequestNotFoundException
import com.example.zipzabe.global.error.exception.ExternalApiBadRequestException
import com.example.zipzabe.global.error.exception.ExternalApiException
import com.example.zipzabe.global.feign.client.MolitRentClient
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.UUID

@Service
class RentTradeService(
    private val analysisRequestRepository: AnalysisRequestRepository,
    private val buildingLedgerRepository: BuildingLedgerRepository,
    private val tradeRecordRepository: TradeRecordRepository,
    private val molitRentClient: MolitRentClient,
    @Value("\${molit.rent.service-key}") private val serviceKey: String,
) {
    private val xmlMapper: XmlMapper = XmlMapper.builder()
        .addModule(KotlinModule.Builder().build())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
        .build()

    @Transactional
    fun fetchRentTrades(
        requestId: UUID,
        months: Int = DEFAULT_SEARCH_MONTHS,
        buildingType: BuildingType? = null,
    ): RentTradeFetchResponse {
        val request = analysisRequestRepository.findById(requestId)
            .orElseThrow(::AnalysisRequestNotFoundException)
        val property = request.property
        val lawdCd = extractLawdCd(property.administrativeCode)
        val targetBuildingType = resolveBuildingType(property, buildingType)
        val dealMonths = buildDealMonths(request.contractDate, months)

        val fetchedRecords = dealMonths
            .flatMap { dealYm ->
                fetchMonthlyItems(targetBuildingType, lawdCd, dealYm)
                    .filter { matchesProperty(it, property) }
                    .mapNotNull {
                        it.toTradeRecord(
                            property = property,
                            buildingType = targetBuildingType,
                            fallbackFloor = request.floor,
                            fallbackExclusiveArea = request.exclusiveArea,
                        )
                    }
            }
            .distinctBy(::toKey)

        val existingRecords = tradeRecordRepository.findByProperty(property)
        val existingKeys = existingRecords.map(::toKey).toSet()
        val recordsToSave = fetchedRecords.filterNot { toKey(it) in existingKeys }
        val savedRecords = if (recordsToSave.isEmpty()) {
            emptyList()
        } else {
            tradeRecordRepository.saveAll(recordsToSave).toList()
        }

        val fetchedKeys = fetchedRecords.map(::toKey).toSet()
        val responseRecords = (savedRecords + existingRecords.filter { toKey(it) in fetchedKeys })
            .distinctBy(::toKey)
            .sortedWith(compareByDescending<TradeRecord> { it.contractDate }.thenByDescending { it.depositAmount })
            .map(::toResponse)

        return RentTradeFetchResponse(
            requestId = requestId,
            lawdCd = lawdCd,
            dealYmFrom = dealMonths.first(),
            dealYmTo = dealMonths.last(),
            buildingType = targetBuildingType,
            fetchedCount = fetchedRecords.size,
            savedCount = savedRecords.size,
            records = responseRecords,
        )
    }

    @Transactional(readOnly = true)
    fun getRentTrades(requestId: UUID): RentTradeListResponse {
        val request = analysisRequestRepository.findById(requestId)
            .orElseThrow(::AnalysisRequestNotFoundException)
        val records = tradeRecordRepository.findByPropertyOrderByContractDateDesc(request.property)
            .map(::toResponse)

        return RentTradeListResponse(
            requestId = requestId,
            count = records.size,
            records = records,
        )
    }

    private fun fetchMonthlyItems(
        buildingType: BuildingType,
        lawdCd: String,
        dealYm: String,
    ): List<MolitRentApiResponse.Item> {
        val items = mutableListOf<MolitRentApiResponse.Item>()
        var pageNo = 1
        var totalCount: Int

        do {
            val response = callMolitRent(buildingType, lawdCd, dealYm, pageNo)
            validateResponse(response)
            val body = response.body
            items += body?.allItems().orEmpty()
            totalCount = body?.totalCount ?: items.size
            pageNo += 1
        } while ((pageNo - 1) * NUM_OF_ROWS < totalCount)

        return items
    }

    private fun callMolitRent(
        buildingType: BuildingType,
        lawdCd: String,
        dealYm: String,
        pageNo: Int,
    ): MolitRentApiResponse {
        if (serviceKey.isBlank()) {
            throw ExternalApiBadRequestException()
        }

        val xml = when (buildingType) {
            BuildingType.APARTMENT -> molitRentClient.getApartmentRent(serviceKey, lawdCd, dealYm, pageNo, NUM_OF_ROWS)
            BuildingType.ROW_HOUSE,
            BuildingType.MULTI_FAMILY,
            BuildingType.STUDIO -> molitRentClient.getRowHouseRent(serviceKey, lawdCd, dealYm, pageNo, NUM_OF_ROWS)
            BuildingType.OFFICETEL -> molitRentClient.getOfficetelRent(serviceKey, lawdCd, dealYm, pageNo, NUM_OF_ROWS)
            BuildingType.DETACHED_HOUSE -> molitRentClient.getDetachedHouseRent(serviceKey, lawdCd, dealYm, pageNo, NUM_OF_ROWS)
            BuildingType.ETC -> throw ExternalApiBadRequestException()
        }

        return runCatching {
            xmlMapper.readValue(xml, MolitRentApiResponse::class.java)
        }.getOrElse {
            throw ExternalApiException()
        }
    }

    private fun validateResponse(response: MolitRentApiResponse) {
        val resultCode = response.header?.resultCode?.trim()
        if (resultCode != null && resultCode !in SUCCESS_RESULT_CODES) {
            if (resultCode in NO_DATA_RESULT_CODES) {
                return
            }
            throw ExternalApiException()
        }
    }

    private fun resolveBuildingType(property: Property, requestedBuildingType: BuildingType?): BuildingType {
        if (requestedBuildingType != null) {
            return requestedBuildingType
        }
        if (property.isApartment) {
            return BuildingType.APARTMENT
        }

        val ledgerPurpose = buildingLedgerRepository.findTopByPropertyOrderByFetchedAtDesc(property)?.mainPurposeName.orEmpty()
        val source = normalize("${property.buildingName.orEmpty()} $ledgerPurpose")

        return when {
            source.contains("오피스텔") -> BuildingType.OFFICETEL
            source.contains("연립") -> BuildingType.ROW_HOUSE
            source.contains("다세대") || source.contains("빌라") -> BuildingType.MULTI_FAMILY
            source.contains("단독") || source.contains("다가구") -> BuildingType.DETACHED_HOUSE
            else -> BuildingType.ETC
        }
    }

    private fun buildDealMonths(contractDate: LocalDate, months: Int): List<String> {
        val normalizedMonths = months.coerceIn(MIN_SEARCH_MONTHS, MAX_SEARCH_MONTHS)
        val today = LocalDate.now()
        val endDate = if (contractDate.isAfter(today)) today else contractDate
        val endMonth = YearMonth.from(endDate)

        return (normalizedMonths - 1 downTo 0)
            .map { endMonth.minusMonths(it.toLong()).format(YEAR_MONTH_FORMATTER) }
    }

    private fun extractLawdCd(administrativeCode: String): String {
        val digits = administrativeCode.filter(Char::isDigit)
        if (digits.length < LAWD_CD_LENGTH) {
            throw ExternalApiBadRequestException()
        }
        return digits.take(LAWD_CD_LENGTH)
    }

    private fun matchesProperty(item: MolitRentApiResponse.Item, property: Property): Boolean {
        val propertyDong = normalize(property.neighborhood)
        val itemDong = normalize(item.legalDong.orEmpty())
        if (itemDong.isNotBlank() && propertyDong.isNotBlank() &&
            !itemDong.contains(propertyDong) && !propertyDong.contains(itemDong)
        ) {
            return false
        }

        val propertyBuildingName = normalizeBuildingName(property.buildingName.orEmpty())
        val itemBuildingName = normalizeBuildingName(item.buildingName.orEmpty())
        val buildingNameMatched = propertyBuildingName.isNotBlank() &&
            itemBuildingName.isNotBlank() &&
            (itemBuildingName.contains(propertyBuildingName) || propertyBuildingName.contains(itemBuildingName))

        val propertyJibunAddress = normalize(property.jibunAddress)
        val itemJibun = normalize(item.jibun.orEmpty())
        val jibunMatched = itemJibun.isNotBlank() && propertyJibunAddress.contains(itemJibun)

        return if (propertyBuildingName.isNotBlank()) {
            buildingNameMatched || jibunMatched
        } else {
            jibunMatched
        }
    }

    private fun MolitRentApiResponse.Item.toTradeRecord(
        property: Property,
        buildingType: BuildingType,
        fallbackFloor: Int,
        fallbackExclusiveArea: Double,
    ): TradeRecord? {
        val contractDate = parseContractDate() ?: return null
        val deposit = parseLongAmount(depositAmount) ?: return null
        val rent = parseLongAmount(monthlyRent) ?: 0L
        val area = parseDoubleAmount(exclusiveArea) ?: fallbackExclusiveArea
        val parsedFloor = parseIntAmount(floor) ?: fallbackFloor

        return TradeRecord(
            property = property,
            buildingType = buildingType,
            contractType = if (rent > 0L) ContractType.MONTHLY_RENT else ContractType.JEONSE,
            depositAmount = deposit,
            monthlyRent = rent.takeIf { it > 0L },
            exclusiveArea = area,
            floor = parsedFloor,
            contractDate = contractDate,
            contractClassification = parseContractClassification(contractClassification),
            contractTerm = contractTerm?.trim()?.takeIf(String::isNotBlank),
            previousDeposit = parseLongAmount(previousDeposit),
            previousMonthlyRent = parseLongAmount(previousMonthlyRent),
        )
    }

    private fun MolitRentApiResponse.Item.parseContractDate(): LocalDate? {
        val year = parseIntAmount(dealYear) ?: return null
        val month = parseIntAmount(dealMonth) ?: return null
        val day = parseIntAmount(dealDay) ?: return null

        return runCatching { LocalDate.of(year, month, day) }.getOrNull()
    }

    private fun parseContractClassification(value: String?): ContractClassification? =
        when (normalize(value.orEmpty())) {
            "신규" -> ContractClassification.NEW
            "갱신" -> ContractClassification.RENEWAL
            else -> null
        }

    private fun parseLongAmount(value: String?): Long? =
        value?.trim()
            ?.replace(",", "")
            ?.takeIf { it.isNotBlank() && it != "-" }
            ?.toLongOrNull()

    private fun parseIntAmount(value: String?): Int? =
        value?.trim()
            ?.takeIf { it.isNotBlank() && it != "-" }
            ?.let { INT_PATTERN.find(it)?.value }
            ?.toIntOrNull()

    private fun parseDoubleAmount(value: String?): Double? =
        value?.trim()
            ?.replace(",", "")
            ?.takeIf { it.isNotBlank() && it != "-" }
            ?.toDoubleOrNull()

    private fun toResponse(record: TradeRecord): RentTradeRecordResponse =
        RentTradeRecordResponse(
            id = record.id,
            buildingType = record.buildingType,
            contractType = record.contractType,
            depositAmount = record.depositAmount,
            monthlyRent = record.monthlyRent,
            exclusiveArea = record.exclusiveArea,
            floor = record.floor,
            contractDate = record.contractDate,
            contractClassification = record.contractClassification,
            contractTerm = record.contractTerm,
            previousDeposit = record.previousDeposit,
            previousMonthlyRent = record.previousMonthlyRent,
            fetchedAt = record.fetchedAt,
        )

    private fun toKey(record: TradeRecord): TradeRecordKey =
        TradeRecordKey(
            buildingType = record.buildingType,
            contractType = record.contractType,
            depositAmount = record.depositAmount,
            monthlyRent = record.monthlyRent,
            exclusiveArea = record.exclusiveArea,
            floor = record.floor,
            contractDate = record.contractDate,
            contractClassification = record.contractClassification,
            contractTerm = record.contractTerm,
        )

    private fun normalize(value: String): String =
        value.lowercase().replace(NORMALIZE_PATTERN, "")

    private fun normalizeBuildingName(value: String): String =
        normalize(value)
            .replace("아파트", "")
            .replace("오피스텔", "")
            .replace("연립", "")
            .replace("다세대", "")
            .replace("빌라", "")

    private data class TradeRecordKey(
        val buildingType: BuildingType,
        val contractType: ContractType,
        val depositAmount: Long,
        val monthlyRent: Long?,
        val exclusiveArea: Double,
        val floor: Int,
        val contractDate: LocalDate,
        val contractClassification: ContractClassification?,
        val contractTerm: String?,
    )

    companion object {
        private const val NUM_OF_ROWS = 1000
        private const val LAWD_CD_LENGTH = 5
        private const val DEFAULT_SEARCH_MONTHS = 24
        private const val MIN_SEARCH_MONTHS = 1
        private const val MAX_SEARCH_MONTHS = 60
        private val YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM")
        private val SUCCESS_RESULT_CODES = setOf("00", "000")
        private val NO_DATA_RESULT_CODES = setOf("03")
        private val NORMALIZE_PATTERN = Regex("[\\s\\-_,.]")
        private val INT_PATTERN = Regex("-?\\d+")
    }
}
