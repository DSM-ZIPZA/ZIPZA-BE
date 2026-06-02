package com.example.zipzabe.domain.property.service

import com.example.zipzabe.domain.analysis.repository.AnalysisRequestRepository
import com.example.zipzabe.domain.property.dto.AverageSalePriceResponse
import com.example.zipzabe.domain.property.entity.Property
import com.example.zipzabe.domain.property.repository.PropertyRepository
import com.example.zipzabe.domain.trade.entity.ContractType
import com.example.zipzabe.domain.trade.repository.TradeRecordRepository
import com.example.zipzabe.domain.trade.service.RentTradeService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToLong

@Service
class PropertyPriceService(
    private val propertyRepository: PropertyRepository,
    private val analysisRequestRepository: AnalysisRequestRepository,
    private val tradeRecordRepository: TradeRecordRepository,
    private val rentTradeService: RentTradeService,
) {
    private val log = LoggerFactory.getLogger(PropertyPriceService::class.java)
    private val molitJeonseCache = ConcurrentHashMap<MolitJeonseCacheKey, MolitJeonseCacheValue>()

    @Transactional(readOnly = true)
    fun getAverageSalePrice(
        query: String?,
        latitude: Double?,
        longitude: Double?,
        radiusMeters: Double,
        buildingName: String? = null,
        isApartment: Boolean? = null,
        months: Int = DEFAULT_JEONSE_MONTHS,
    ): AverageSalePriceResponse {
        val molitJeonsePrices = fetchMolitJeonsePrices(query, buildingName, isApartment, months)
        if (molitJeonsePrices.isNotEmpty()) {
            return AverageSalePriceResponse(
                query = query,
                latitude = latitude,
                longitude = longitude,
                averageSalePriceManwon = molitJeonsePrices.average().roundToLong(),
                sampleCount = molitJeonsePrices.size,
            )
        }

        val candidates = findCandidateProperties(query, latitude, longitude, radiusMeters)
        if (candidates.isEmpty()) {
            return AverageSalePriceResponse(query, latitude, longitude, null, 0)
        }

        val requests = analysisRequestRepository.findByPropertyIn(candidates)
        if (requests.isEmpty()) {
            return AverageSalePriceResponse(query, latitude, longitude, null, 0)
        }

        val prices = requests
            .mapNotNull { request ->
                averageStoredJeonseDeposit(request.property) ?: request.depositAmount.takeIf { it > 0L }
            }
            .filter { it > 0L }

        if (prices.isEmpty()) {
            return AverageSalePriceResponse(query, latitude, longitude, null, 0)
        }

        return AverageSalePriceResponse(
            query = query,
            latitude = latitude,
            longitude = longitude,
            averageSalePriceManwon = prices.average().roundToLong(),
            sampleCount = prices.size,
        )
    }

    private fun findCandidateProperties(
        query: String?,
        latitude: Double?,
        longitude: Double?,
        radiusMeters: Double,
    ): List<Property> {
        val byQuery = if (query.isNullOrBlank()) {
            emptyList()
        } else {
            propertyRepository.findByRoadAddressContainingOrJibunAddressContainingOrBuildingNameContaining(
                query,
                query,
                query,
            )
        }

        val byLocation = if (latitude != null && longitude != null) {
            val latDelta = radiusMeters / METERS_PER_LATITUDE_DEGREE
            val lngDelta = radiusMeters / (METERS_PER_LATITUDE_DEGREE * kotlin.math.cos(Math.toRadians(latitude)))
            propertyRepository.findByLatitudeBetweenAndLongitudeBetween(
                latitude - latDelta,
                latitude + latDelta,
                longitude - lngDelta,
                longitude + lngDelta,
            )
        } else {
            emptyList()
        }

        return (byQuery + byLocation).distinctBy { it.id }
    }

    private fun fetchMolitJeonsePrices(
        query: String?,
        buildingName: String?,
        isApartment: Boolean?,
        months: Int,
    ): List<Long> {
        if (query.isNullOrBlank()) return emptyList()
        val cacheKey = MolitJeonseCacheKey(
            query = query.trim(),
            buildingName = buildingName?.trim().orEmpty(),
            isApartment = isApartment,
            months = months.coerceIn(MIN_JEONSE_MONTHS, MAX_JEONSE_MONTHS),
        )
        molitJeonseCache[cacheKey]
            ?.takeIf { Duration.between(it.cachedAt, Instant.now()) < MOLIT_JEONSE_CACHE_TTL }
            ?.let { return it.prices }

        return runCatching {
            rentTradeService.fetchRecentJeonseDepositsByAddress(
                query = query,
                buildingName = buildingName,
                isApartment = isApartment,
                months = months,
            ).also { prices ->
                molitJeonseCache[cacheKey] = MolitJeonseCacheValue(prices, Instant.now())
            }
        }.getOrElse { e ->
            log.warn(
                "Failed to fetch MOLIT jeonse prices. query={} buildingName={} reason={}",
                query,
                buildingName,
                e.message ?: e::class.simpleName,
            )
            emptyList()
        }
    }

    private fun averageStoredJeonseDeposit(property: Property): Long? {
        val jeonseDeposits = tradeRecordRepository
            .findByPropertyOrderByContractDateDesc(property)
            .filter { it.contractType == ContractType.JEONSE }
            .map { it.depositAmount }
        return jeonseDeposits.takeIf { it.isNotEmpty() }?.average()?.roundToLong()
    }

    companion object {
        private const val METERS_PER_LATITUDE_DEGREE = 111_320.0
        private const val DEFAULT_JEONSE_MONTHS = 12
        private const val MIN_JEONSE_MONTHS = 1
        private const val MAX_JEONSE_MONTHS = 60
        private val MOLIT_JEONSE_CACHE_TTL: Duration = Duration.ofHours(1)
    }

    private data class MolitJeonseCacheKey(
        val query: String,
        val buildingName: String,
        val isApartment: Boolean?,
        val months: Int,
    )

    private data class MolitJeonseCacheValue(
        val prices: List<Long>,
        val cachedAt: Instant,
    )
}
