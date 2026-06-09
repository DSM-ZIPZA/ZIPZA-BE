package com.example.zipzabe.domain.property.service

import com.example.zipzabe.domain.analysis.repository.AnalysisRequestRepository
import com.example.zipzabe.domain.building.repository.BuildingLedgerRepository
import com.example.zipzabe.domain.property.dto.PropertyDetailResponse
import com.example.zipzabe.domain.property.dto.PropertyListingResponse
import com.example.zipzabe.domain.property.dto.TransactionTypeResponse
import com.example.zipzabe.domain.property.entity.Property
import com.example.zipzabe.domain.property.repository.PropertyRepository
import com.example.zipzabe.domain.registry.repository.RegistryRawRepository
import com.example.zipzabe.domain.registry.repository.RegistryTitleRepository
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
    private val registryRawRepository: RegistryRawRepository,
    private val registryTitleRepository: RegistryTitleRepository,
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
                val registryValues = request?.let(::resolveRegistryValues)
                PropertyListingResponse.from(
                    property = property,
                    request = request,
                    totalFloors = ledger?.floorsAboveGround,
                    resolvedFloor = registryValues?.first ?: request?.floor?.takeIf { it != 0 },
                    resolvedExclusiveAreaM2 = registryValues?.second
                        ?: request?.exclusiveArea?.takeIf { it > 0.0 },
                )
            }
            .filter { listing ->
                val contractMatched = transactionType.isNullOrBlank() ||
                    listing.transactionType == parseTransactionType(transactionType)
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

    private fun parseTransactionType(value: String): TransactionTypeResponse? =
        when (value.trim().uppercase()) {
            "JEONSE", "LEASE" -> TransactionTypeResponse.LEASE
            "MONTHLY_RENT", "RENT" -> TransactionTypeResponse.RENT
            else -> null
        }

    @Transactional(readOnly = true)
    fun getDetail(propertyId: UUID): PropertyDetailResponse {
        val user = userFacade.getCurrentUser()
        val property = propertyRepository.findById(propertyId).orElseThrow { AnalysisRequestNotFoundException() }
        val request = analysisRequestRepository.findTopByPropertyAndUserOrderByRequestedAtDesc(property, user)
        val ledger = buildingLedgerRepository.findTopByPropertyOrderByFetchedAtDesc(property)
        val registryValues = request?.let(::resolveRegistryValues)
        return PropertyDetailResponse(
            PropertyListingResponse.from(
                property = property,
                request = request,
                totalFloors = ledger?.floorsAboveGround,
                resolvedFloor = registryValues?.first ?: request?.floor?.takeIf { it != 0 },
                resolvedExclusiveAreaM2 = registryValues?.second
                    ?: request?.exclusiveArea?.takeIf { it > 0.0 },
            )
        )
    }

    private fun resolveRegistryValues(
        request: com.example.zipzabe.domain.analysis.entity.AnalysisRequest,
    ): Pair<Int?, Double?> {
        val raw = registryRawRepository.findTopByRequestOrderByFetchedAtDesc(request)
            ?: return null to null
        val titles = registryTitleRepository.findByRegistryRaw(raw)
        val floor = titles.firstNotNullOfOrNull { title ->
            val floorInfo = title.floorInfo
            floorInfo?.let(FLOOR_PATTERN::find)?.value?.toIntOrNull()?.let { value ->
                if (floorInfo.contains("지하")) -value else value
            }
        }
        val area = titles.firstNotNullOfOrNull { it.exclusiveArea?.takeIf { value -> value > 0.0 } }
        return floor to area
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

    companion object {
        private val FLOOR_PATTERN = Regex("\\d+")
    }
}
