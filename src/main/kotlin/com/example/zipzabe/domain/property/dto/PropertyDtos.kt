package com.example.zipzabe.domain.property.dto

import com.example.zipzabe.domain.analysis.entity.AnalysisRequest
import com.example.zipzabe.domain.analysis.entity.ContractType
import com.example.zipzabe.domain.property.entity.Property
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime
import java.util.UUID

@Schema(description = "프론트 매물 카드/지도 표시용 응답")
data class PropertyListingResponse(
    val id: UUID,
    val title: String,
    val address: String,
    val roadAddress: String,
    val jibunAddress: String,
    val detailAddress: String?,
    val buildingManagementNumber: String,
    val postalCode: String,
    val administrativeCode: String,
    val city: String,
    val district: String,
    val neighborhood: String,
    val latitude: Double,
    val longitude: Double,
    val propertyType: PropertyTypeResponse,
    val transactionType: TransactionTypeResponse?,
    @Schema(description = "보증금 또는 대표 가격. 만원 단위")
    val priceManwon: Long?,
    @Schema(description = "보증금. 만원 단위")
    val depositAmountManwon: Long?,
    @Schema(description = "월세. 만원 단위")
    val monthlyRentManwon: Long?,
    @Schema(description = "전용면적. ㎡ 단위")
    val exclusiveAreaM2: Double?,
    val floor: Int?,
    val totalFloors: Int?,
    val createdAt: LocalDateTime,
) {
    companion object {
        fun from(
            property: Property,
            request: AnalysisRequest? = null,
            totalFloors: Int? = null,
            resolvedFloor: Int? = request?.floor?.takeIf { it != 0 },
            resolvedExclusiveAreaM2: Double? = request?.exclusiveArea?.takeIf { it > 0.0 },
        ): PropertyListingResponse {
            val title = property.buildingName
                ?: property.detailAddress
                ?: property.roadAddress.ifBlank { property.jibunAddress }
            return PropertyListingResponse(
                id = requireNotNull(property.id),
                title = title,
                address = property.roadAddress.ifBlank { property.jibunAddress },
                roadAddress = property.roadAddress,
                jibunAddress = property.jibunAddress,
                detailAddress = property.detailAddress,
                buildingManagementNumber = property.buildingManagementNumber,
                postalCode = property.postalCode,
                administrativeCode = property.administrativeCode,
                city = property.city,
                district = property.district,
                neighborhood = property.neighborhood,
                latitude = property.latitude,
                longitude = property.longitude,
                propertyType = if (property.isApartment) PropertyTypeResponse.APARTMENT else PropertyTypeResponse.MULTI_FAMILY,
                transactionType = request?.contractType?.toTransactionType(),
                priceManwon = request?.depositAmount,
                depositAmountManwon = request?.depositAmount,
                monthlyRentManwon = request?.monthlyRent,
                exclusiveAreaM2 = resolvedExclusiveAreaM2,
                floor = resolvedFloor,
                totalFloors = totalFloors,
                createdAt = property.createdAt,
            )
        }

        private fun ContractType.toTransactionType(): TransactionTypeResponse =
            when (this) {
                ContractType.JEONSE -> TransactionTypeResponse.LEASE
                ContractType.MONTHLY_RENT -> TransactionTypeResponse.RENT
            }
    }
}

data class PropertyDetailResponse(
    val listing: PropertyListingResponse,
)

enum class PropertyTypeResponse {
    APARTMENT,
    MULTI_FAMILY,
}

enum class TransactionTypeResponse {
    RENT,
    LEASE,
}
