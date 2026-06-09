package com.example.zipzabe.domain.property.service

import com.example.zipzabe.domain.property.dto.AverageSalePriceResponse
import com.example.zipzabe.domain.trade.service.RentTradeService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToLong

@Service
class PropertyPriceService(
    private val rentTradeService: RentTradeService,
) {
    private val log = LoggerFactory.getLogger(PropertyPriceService::class.java)
    private val molitJeonseCache = ConcurrentHashMap<MolitJeonseCacheKey, MolitJeonseCacheValue>()

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

        return AverageSalePriceResponse(query, latitude, longitude, null, 0)
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
                if (prices.isNotEmpty()) {
                    molitJeonseCache[cacheKey] = MolitJeonseCacheValue(prices, Instant.now())
                } else {
                    molitJeonseCache.remove(cacheKey)
                }
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

    companion object {
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
