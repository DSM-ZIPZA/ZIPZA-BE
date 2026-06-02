package com.example.zipzabe.domain.property.service

import com.example.zipzabe.domain.analysis.repository.AnalysisRequestRepository
import com.example.zipzabe.domain.analysis.repository.RecoveryAnalysisRepository
import com.example.zipzabe.domain.property.dto.AverageSalePriceResponse
import com.example.zipzabe.domain.property.entity.Property
import com.example.zipzabe.domain.property.repository.PropertyRepository
import com.example.zipzabe.domain.trade.entity.ContractType
import com.example.zipzabe.domain.trade.repository.TradeRecordRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.math.roundToLong

@Service
class PropertyPriceService(
    private val propertyRepository: PropertyRepository,
    private val analysisRequestRepository: AnalysisRequestRepository,
    private val recoveryAnalysisRepository: RecoveryAnalysisRepository,
    private val tradeRecordRepository: TradeRecordRepository,
) {
    @Transactional(readOnly = true)
    fun getAverageSalePrice(
        query: String?,
        latitude: Double?,
        longitude: Double?,
        radiusMeters: Double,
    ): AverageSalePriceResponse {
        val candidates = findCandidateProperties(query, latitude, longitude, radiusMeters)
        if (candidates.isEmpty()) {
            return AverageSalePriceResponse(query, latitude, longitude, null, 0)
        }

        val requests = analysisRequestRepository.findByPropertyIn(candidates)
        if (requests.isEmpty()) {
            return AverageSalePriceResponse(query, latitude, longitude, null, 0)
        }

        val recoveryPricesByRequestId = recoveryAnalysisRepository.findByRequestIn(requests)
            .groupBy { it.request.id }
            .mapValues { (_, analyses) ->
                analyses.maxByOrNull { it.analyzedAt }?.estimatedPropertyValue
            }

        val prices = requests
            .mapNotNull { request ->
                recoveryPricesByRequestId[request.id]
                    ?: estimatePropertyValue(request.property, request.depositAmount)
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

    private fun estimatePropertyValue(property: Property, depositAmount: Long): Long? {
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

    companion object {
        private const val METERS_PER_LATITUDE_DEGREE = 111_320.0
        private const val JEONSE_RATE = 0.70
    }
}
