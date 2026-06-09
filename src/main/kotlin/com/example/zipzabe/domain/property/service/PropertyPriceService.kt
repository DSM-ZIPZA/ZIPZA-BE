package com.example.zipzabe.domain.property.service

import com.example.zipzabe.domain.property.dto.AverageSalePriceResponse
import com.example.zipzabe.domain.trade.service.RentTradeService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import kotlin.math.roundToLong

@Service
class PropertyPriceService(
    private val rentTradeService: RentTradeService,
    private val jeonsePriceCacheService: JeonsePriceCacheService,
) {
    private val log = LoggerFactory.getLogger(PropertyPriceService::class.java)

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
        val normalizedMonths = months.coerceIn(MIN_JEONSE_MONTHS, MAX_JEONSE_MONTHS)
        jeonsePriceCacheService.get(
            query = query,
            buildingName = buildingName,
            isApartment = isApartment,
            months = normalizedMonths,
        )?.let { return it }

        return runCatching {
            rentTradeService.fetchRecentJeonseDepositsByAddress(
                query = query,
                buildingName = buildingName,
                isApartment = isApartment,
                months = normalizedMonths,
            ).also { prices ->
                jeonsePriceCacheService.put(
                    query = query,
                    buildingName = buildingName,
                    isApartment = isApartment,
                    months = normalizedMonths,
                    prices = prices,
                )
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
    }
}
