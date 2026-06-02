package com.example.zipzabe.domain.analysis.service

import com.example.zipzabe.domain.analysis.dto.AnalysisDetailResponse
import com.example.zipzabe.domain.analysis.dto.BuildingInfoResponse
import com.example.zipzabe.domain.analysis.dto.BuildingLandAnalysisDetailResponse
import com.example.zipzabe.domain.analysis.dto.OverallAnalysisDetailResponse
import com.example.zipzabe.domain.analysis.dto.PricePointResponse
import com.example.zipzabe.domain.analysis.dto.RegistrationRecordResponse
import com.example.zipzabe.domain.analysis.dto.RegistrationSectionResponse
import com.example.zipzabe.domain.analysis.repository.AnalysisRequestRepository
import com.example.zipzabe.domain.analysis.repository.BuildingAnalysisRepository
import com.example.zipzabe.domain.analysis.repository.GuaranteeAnalysisRepository
import com.example.zipzabe.domain.analysis.repository.PriceAnalysisRepository
import com.example.zipzabe.domain.analysis.repository.RecoveryAnalysisRepository
import com.example.zipzabe.domain.analysis.repository.RightsAnalysisRepository
import com.example.zipzabe.domain.building.repository.BuildingLedgerRepository
import com.example.zipzabe.domain.property.dto.PropertyListingResponse
import com.example.zipzabe.domain.property.service.PropertyPriceService
import com.example.zipzabe.domain.registry.repository.RegistryMortgageRepository
import com.example.zipzabe.domain.registry.repository.RegistryOwnershipRepository
import com.example.zipzabe.domain.registry.repository.RegistryRawRepository
import com.example.zipzabe.domain.registry.repository.RegistryRestrictionRepository
import com.example.zipzabe.domain.registry.repository.RegistryTitleRepository
import com.example.zipzabe.domain.report.dto.NextActionResponse
import com.example.zipzabe.domain.report.dto.RiskItemResponse
import com.example.zipzabe.domain.report.repository.DiagnosisReportRepository
import com.example.zipzabe.domain.trade.entity.ContractType
import com.example.zipzabe.domain.trade.repository.TradeRecordRepository
import com.example.zipzabe.domain.user.facade.UserFacade
import com.example.zipzabe.global.error.exception.AnalysisRequestNotFoundException
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.YearMonth
import java.util.UUID
import kotlin.math.roundToLong

@Service
class AnalysisDetailService(
    private val userFacade: UserFacade,
    private val objectMapper: ObjectMapper,
    private val analysisRequestRepository: AnalysisRequestRepository,
    private val buildingLedgerRepository: BuildingLedgerRepository,
    private val registryRawRepository: RegistryRawRepository,
    private val registryTitleRepository: RegistryTitleRepository,
    private val registryOwnershipRepository: RegistryOwnershipRepository,
    private val registryRestrictionRepository: RegistryRestrictionRepository,
    private val registryMortgageRepository: RegistryMortgageRepository,
    private val tradeRecordRepository: TradeRecordRepository,
    private val priceAnalysisRepository: PriceAnalysisRepository,
    private val buildingAnalysisRepository: BuildingAnalysisRepository,
    private val rightsAnalysisRepository: RightsAnalysisRepository,
    private val recoveryAnalysisRepository: RecoveryAnalysisRepository,
    private val guaranteeAnalysisRepository: GuaranteeAnalysisRepository,
    private val diagnosisReportRepository: DiagnosisReportRepository,
    private val propertyPriceService: PropertyPriceService,
) {
    @Transactional(readOnly = true)
    fun getDetail(requestId: UUID): AnalysisDetailResponse {
        val user = userFacade.getCurrentUser()
        val request = analysisRequestRepository.findByIdAndUser(requestId, user)
            ?: throw AnalysisRequestNotFoundException()
        val property = request.property
        val ledger = buildingLedgerRepository.findTopByPropertyOrderByFetchedAtDesc(property)
        val registryRaw = registryRawRepository.findTopByRequestOrderByFetchedAtDesc(request)
        val priceAnalysis = priceAnalysisRepository.findTopByRequestOrderByAnalyzedAtDesc(request)
        val buildingAnalysis = buildingAnalysisRepository.findTopByRequestOrderByAnalyzedAtDesc(request)
        val rightsAnalysis = rightsAnalysisRepository.findTopByRequestOrderByAnalyzedAtDesc(request)
        val recoveryAnalysis = recoveryAnalysisRepository.findTopByRequestOrderByAnalyzedAtDesc(request)
        val guaranteeAnalysis = guaranteeAnalysisRepository.findTopByRequestOrderByAnalyzedAtDesc(request)
        val diagnosisReport = diagnosisReportRepository.findTopByRequestOrderByCreatedAtDesc(request)
        val averageSalePrice = propertyPriceService.getAverageSalePrice(
            query = property.roadAddress.ifBlank { property.jibunAddress },
            latitude = property.latitude.takeIf { it != 0.0 },
            longitude = property.longitude.takeIf { it != 0.0 },
            radiusMeters = DETAIL_AVERAGE_PRICE_RADIUS_METERS,
        ).averageSalePriceManwon
        val estimatedPropertyValue = averageSalePrice
            ?: recoveryAnalysis?.estimatedPropertyValue
            ?: guaranteeAnalysis?.estimatedPropertyValue
            ?: estimatePropertyValue(property, request.depositAmount)

        val topRisks = diagnosisReport?.topRisks?.let { readList<RiskItemResponse>(it) }.orEmpty()
        val nextActions = diagnosisReport?.nextActions?.let { readList<NextActionResponse>(it) }.orEmpty()
        val publicWarnings = listOfNotNull(
            buildingAnalysis?.violationMessage,
            rightsAnalysis?.riskReason,
            priceAnalysis?.riskReason,
            recoveryAnalysis?.riskReason,
        ) + topRisks.map { it.detail }

        return AnalysisDetailResponse(
            requestId = requestId,
            property = PropertyListingResponse.from(property, request, ledger?.floorsAboveGround),
            buildingInfo = BuildingInfoResponse(
                name = property.buildingName ?: property.roadAddress,
                address = property.roadAddress.ifBlank { property.jibunAddress },
                buildingManagementNumber = property.buildingManagementNumber,
                floor = request.floor,
                totalFloors = ledger?.floorsAboveGround,
                exclusiveAreaM2 = request.exclusiveArea,
                estimatedPropertyValueManwon = estimatedPropertyValue,
            ),
            priceHistory = buildPriceHistory(request),
            registrationSections = buildRegistrationSections(registryRaw),
            buildingLandAnalysis = BuildingLandAnalysisDetailResponse(
                usage = ledger?.mainPurposeName,
                dongHo = property.detailAddress.orEmpty(),
                illegalBuilding = if (ledger?.isViolationBuilding == true) "있음" else "없음",
                warnings = listOfNotNull(buildingAnalysis?.violationMessage),
            ),
            overallAnalysis = OverallAnalysisDetailResponse(
                totalRiskScore = diagnosisReport?.totalScore,
                priceScore = diagnosisReport?.priceScore ?: priceAnalysis?.riskScore,
                registrationScore = diagnosisReport?.rightsScore ?: rightsAnalysis?.riskScore,
                buildingLandScore = diagnosisReport?.buildingScore ?: buildingAnalysis?.riskScore,
                contractScore = diagnosisReport?.contractScore,
                confidenceScore = diagnosisReport?.confidenceScore,
                warningMessages = publicWarnings.distinct(),
                topRisks = topRisks,
            ),
            nextActions = nextActions,
            aiSummary = diagnosisReport?.aiSummary,
        )
    }

    private fun buildPriceHistory(request: com.example.zipzabe.domain.analysis.entity.AnalysisRequest): List<PricePointResponse> {
        val records = tradeRecordRepository.findByPropertyOrderByContractDateDesc(request.property).asReversed()
        val history = records
            .groupBy { YearMonth.from(it.contractDate).toString() }
            .map { (month, items) ->
                val values = items.map { it.depositAmount }
                PricePointResponse(
                    date = month,
                    open = items.first().depositAmount,
                    high = values.maxOrNull() ?: 0L,
                    low = values.minOrNull() ?: 0L,
                    close = items.last().depositAmount,
                    volume = items.size,
                )
            }
        if (history.isNotEmpty()) return history

        return listOf(
            PricePointResponse(
                date = YearMonth.from(request.contractDate).toString(),
                open = request.depositAmount,
                high = request.depositAmount,
                low = request.depositAmount,
                close = request.depositAmount,
                volume = 1,
            )
        )
    }

    private fun estimatePropertyValue(
        property: com.example.zipzabe.domain.property.entity.Property,
        depositAmount: Long,
    ): Long? {
        val jeonseDeposits = tradeRecordRepository
            .findByPropertyOrderByContractDateDesc(property)
            .filter { it.contractType == ContractType.JEONSE }
            .map { it.depositAmount }
        val baseDeposit = medianOrNull(jeonseDeposits) ?: depositAmount.takeIf { it > 0L }
        return baseDeposit?.let { (it / JEONSE_RATE).roundToLong() }
    }

    private fun medianOrNull(values: List<Long>): Long? {
        if (values.isEmpty()) return null
        val sorted = values.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 0) {
            (sorted[mid - 1] + sorted[mid]) / 2
        } else {
            sorted[mid]
        }
    }

    private fun buildRegistrationSections(
        registryRaw: com.example.zipzabe.domain.registry.entity.RegistryRaw?,
    ): List<RegistrationSectionResponse> {
        if (registryRaw == null) return emptyList()
        val titles = registryTitleRepository.findByRegistryRaw(registryRaw).mapIndexed { index, item ->
            RegistrationRecordResponse(
                rank = index + 1,
                purpose = item.realEstateType,
                registrationDate = null,
                registrationCause = item.purpose,
                rightsAndNotes = listOfNotNull(item.locationAddress, item.buildingName, item.floorInfo).joinToString(" "),
            )
        }
        val ownerships = registryOwnershipRepository.findByRegistryRawOrderByRankNumberAsc(registryRaw).map {
            RegistrationRecordResponse(
                rank = it.rankNumber,
                purpose = it.registrationPurpose,
                registrationDate = it.receptionDate,
                registrationCause = it.registrationCause,
                rightsAndNotes = listOfNotNull(it.ownerName, it.ownerIdMasked, it.shareRatio).joinToString(" "),
            )
        }
        val restrictions = registryRestrictionRepository.findByRegistryRawOrderByRankNumberAsc(registryRaw).map {
            RegistrationRecordResponse(
                rank = it.rankNumber,
                purpose = it.registrationPurpose,
                registrationDate = it.receptionDate,
                registrationCause = it.registrationCause,
                rightsAndNotes = listOfNotNull(it.rightHolderName, it.detail).joinToString(" "),
            )
        }
        val mortgages = registryMortgageRepository.findByRegistryRawOrderByRankNumberAsc(registryRaw).map {
            RegistrationRecordResponse(
                rank = it.rankNumber,
                purpose = it.registrationPurpose,
                registrationDate = it.receptionDate,
                registrationCause = it.registrationCause,
                rightsAndNotes = listOfNotNull(it.creditorName, it.debtorName, it.claimAmount?.let { amount -> "${amount}만원" }).joinToString(" "),
            )
        }
        return listOf(
            RegistrationSectionResponse("표제부", titles),
            RegistrationSectionResponse("갑구", ownerships + restrictions),
            RegistrationSectionResponse("을구", mortgages),
        )
    }

    private inline fun <reified T> readList(json: String): List<T> =
        runCatching {
            objectMapper.readValue(json, object : TypeReference<List<T>>() {})
        }.getOrDefault(emptyList())

    companion object {
        private const val JEONSE_RATE = 0.70
        private const val DETAIL_AVERAGE_PRICE_RADIUS_METERS = 250.0
    }
}
