package com.example.zipzabe.domain.analysis.dto

import com.example.zipzabe.domain.analysis.entity.AnalysisRequest
import com.example.zipzabe.domain.analysis.entity.AnalysisStatus
import com.example.zipzabe.domain.analysis.entity.ContractType
import com.example.zipzabe.domain.property.entity.Property
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class AnalysisRequestCreateRequest(
    val property: PropertyCreateRequest,
    val contractType: ContractType,
    val depositAmount: Long,
    val monthlyRent: Long? = null,
    val floor: Int,
    val exclusiveArea: Double,
    val contractDate: LocalDate,
    val balanceDate: LocalDate,
    val expiryDate: LocalDate,
)

data class PropertyCreateRequest(
    val roadAddress: String,
    val jibunAddress: String,
    val detailAddress: String? = null,
    val buildingManagementNumber: String,
    val postalCode: String,
    val administrativeCode: String,
    val city: String,
    val district: String,
    val neighborhood: String,
    val buildingName: String? = null,
    val isApartment: Boolean,
    val longitude: Double,
    val latitude: Double,
)

data class AnalysisRequestResponse(
    val requestId: UUID,
    val property: PropertyResponse,
    val contractType: ContractType,
    val depositAmount: Long,
    val monthlyRent: Long?,
    val floor: Int,
    val exclusiveArea: Double,
    val contractDate: LocalDate,
    val balanceDate: LocalDate,
    val expiryDate: LocalDate,
    val status: AnalysisStatus,
    val requestedAt: LocalDateTime,
    val completedAt: LocalDateTime?,
) {
    companion object {
        fun from(request: AnalysisRequest): AnalysisRequestResponse =
            AnalysisRequestResponse(
                requestId = requireNotNull(request.id),
                property = PropertyResponse.from(request.property),
                contractType = request.contractType,
                depositAmount = request.depositAmount,
                monthlyRent = request.monthlyRent,
                floor = request.floor,
                exclusiveArea = request.exclusiveArea,
                contractDate = request.contractDate,
                balanceDate = request.balanceDate,
                expiryDate = request.expiryDate,
                status = request.status,
                requestedAt = request.requestedAt,
                completedAt = request.completedAt,
            )
    }
}

data class PropertyResponse(
    val propertyId: UUID,
    val roadAddress: String,
    val jibunAddress: String,
    val detailAddress: String?,
    val buildingManagementNumber: String,
    val postalCode: String,
    val administrativeCode: String,
    val city: String,
    val district: String,
    val neighborhood: String,
    val buildingName: String?,
    val isApartment: Boolean,
    val longitude: Double,
    val latitude: Double,
) {
    companion object {
        fun from(property: Property): PropertyResponse =
            PropertyResponse(
                propertyId = requireNotNull(property.id),
                roadAddress = property.roadAddress,
                jibunAddress = property.jibunAddress,
                detailAddress = property.detailAddress,
                buildingManagementNumber = property.buildingManagementNumber,
                postalCode = property.postalCode,
                administrativeCode = property.administrativeCode,
                city = property.city,
                district = property.district,
                neighborhood = property.neighborhood,
                buildingName = property.buildingName,
                isApartment = property.isApartment,
                longitude = property.longitude,
                latitude = property.latitude,
            )
    }
}

data class AnalysisRequestSummaryResponse(
    val request: AnalysisRequestResponse,
    val documents: DocumentCollectionSummary,
    val analyses: AnalysisResultSummary,
    val diagnosisReport: ResultAvailability,
    val readyToStartAnalysis: Boolean,
)

data class DocumentCollectionSummary(
    val buildingLedger: ResultAvailability,
    val registryOcr: ResultAvailability,
    val rentTrades: CountedResultAvailability,
)

data class AnalysisResultSummary(
    val priceAnalysis: ResultAvailability,
    val publicLedgerSummary: ResultAvailability,
    val guaranteeAnalysis: ResultAvailability,
    val recoveryAnalysis: ResultAvailability,
    val fraudPatternAnalysis: ResultAvailability,
)

data class ResultAvailability(
    val completed: Boolean,
    val id: UUID? = null,
    val updatedAt: LocalDateTime? = null,
)

data class CountedResultAvailability(
    val completed: Boolean,
    val count: Long,
)
