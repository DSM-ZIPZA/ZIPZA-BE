package com.example.zipzabe.domain.property.service

import com.example.zipzabe.domain.analysis.repository.AnalysisRequestRepository
import com.example.zipzabe.domain.analysis.repository.RecoveryAnalysisRepository
import com.example.zipzabe.domain.property.dto.AverageSalePriceResponse
import com.example.zipzabe.domain.property.entity.Property
import com.example.zipzabe.domain.property.repository.PropertyRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.math.roundToLong

@Service
class PropertyPriceService(
    private val propertyRepository: PropertyRepository,
    private val analysisRequestRepository: AnalysisRequestRepository,
    private val recoveryAnalysisRepository: RecoveryAnalysisRepository,
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

        val prices = recoveryAnalysisRepository.findByRequestIn(requests)
            .map { it.estimatedPropertyValue }
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
            propertyRepository.findByRoadAddressContainingOrJibunAddressContaining(query, query)
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

    companion object {
        private const val METERS_PER_LATITUDE_DEGREE = 111_320.0
    }
}
