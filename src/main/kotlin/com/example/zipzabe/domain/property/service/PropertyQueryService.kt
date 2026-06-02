package com.example.zipzabe.domain.property.service

import com.example.zipzabe.domain.analysis.repository.AnalysisRequestRepository
import com.example.zipzabe.domain.building.repository.BuildingLedgerRepository
import com.example.zipzabe.domain.property.dto.PropertyDetailResponse
import com.example.zipzabe.domain.property.dto.PropertyListingResponse
import com.example.zipzabe.domain.property.entity.Property
import com.example.zipzabe.domain.property.repository.PropertyRepository
import com.example.zipzabe.domain.user.facade.UserFacade
import com.example.zipzabe.global.error.exception.AnalysisRequestNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Service
class PropertyQueryService(
    private val userFacade: UserFacade,
    private val propertyRepository: PropertyRepository,
    private val analysisRequestRepository: AnalysisRequestRepository,
    private val buildingLedgerRepository: BuildingLedgerRepository,
) {
    @Transactional(readOnly = true)
    fun getListings(
        lat: Double?,
        lng: Double?,
        radiusMeters: Double?,
        query: String?,
        transactionType: String?,
        depositMin: Long?,
        depositMax: Long?,
        monthlyRentMin: Long?,
        monthlyRentMax: Long?,
        sort: String?,
    ): List<PropertyListingResponse> {
        val user = userFacade.getCurrentUser()
        val properties = if (query.isNullOrBlank()) {
            propertyRepository.findAll()
        } else {
            propertyRepository.findByRoadAddressContainingOrJibunAddressContainingOrBuildingNameContaining(
                query,
                query,
                query,
            )
        }
        val listings = properties
            .asSequence()
            .filter { property ->
                lat == null || lng == null || radiusMeters == null ||
                    distanceMeters(lat, lng, property.latitude, property.longitude) <= radiusMeters
            }
            .map { property ->
                val request = analysisRequestRepository.findTopByPropertyAndUserOrderByRequestedAtDesc(property, user)
                val ledger = buildingLedgerRepository.findTopByPropertyOrderByFetchedAtDesc(property)
                PropertyListingResponse.from(property, request, ledger?.floorsAboveGround)
            }
            .filter { listing ->
                val contractMatched = transactionType.isNullOrBlank() ||
                    listing.transactionType?.name?.equals(transactionType, ignoreCase = true) == true
                val deposit = listing.depositAmountManwon ?: listing.priceManwon ?: 0L
                val rent = listing.monthlyRentManwon ?: 0L
                contractMatched &&
                    (depositMin == null || deposit >= depositMin) &&
                    (depositMax == null || deposit <= depositMax) &&
                    (monthlyRentMin == null || rent >= monthlyRentMin) &&
                    (monthlyRentMax == null || rent <= monthlyRentMax)
            }
            .toList()
        return when (sort) {
            "price-low" -> listings.sortedBy { it.priceManwon ?: Long.MAX_VALUE }
            "price-high" -> listings.sortedByDescending { it.priceManwon ?: 0L }
            else -> listings
        }
    }

    @Transactional(readOnly = true)
    fun getDetail(propertyId: UUID): PropertyDetailResponse {
        val user = userFacade.getCurrentUser()
        val property = propertyRepository.findById(propertyId).orElseThrow { AnalysisRequestNotFoundException() }
        val request = analysisRequestRepository.findTopByPropertyAndUserOrderByRequestedAtDesc(property, user)
        val ledger = buildingLedgerRepository.findTopByPropertyOrderByFetchedAtDesc(property)
        return PropertyDetailResponse(PropertyListingResponse.from(property, request, ledger?.floorsAboveGround))
    }

    private fun distanceMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val earthRadius = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLng / 2) * sin(dLng / 2)
        return earthRadius * 2 * atan2(sqrt(a), sqrt(1 - a))
    }
}
