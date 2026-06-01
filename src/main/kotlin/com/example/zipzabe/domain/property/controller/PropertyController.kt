package com.example.zipzabe.domain.property.controller

import com.example.zipzabe.domain.property.dto.PropertyDetailResponse
import com.example.zipzabe.domain.property.dto.PropertyListingResponse
import com.example.zipzabe.domain.property.service.PropertyQueryService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@Tag(name = "매물", description = "프론트 카드/지도 표시용 매물 조회 API")
@RestController
@RequestMapping("/api/properties")
class PropertyController(
    private val propertyQueryService: PropertyQueryService,
) {
    @Operation(summary = "매물 목록 조회", description = "카드/지도 표시용 매물 목록을 조회합니다. 금액은 만원, 면적은 ㎡ 단위입니다.")
    @GetMapping
    fun getListings(
        @RequestParam(required = false) lat: Double?,
        @RequestParam(required = false) lng: Double?,
        @RequestParam(required = false, defaultValue = "5000") radiusMeters: Double?,
        @RequestParam(required = false) query: String?,
        @RequestParam(required = false) transactionType: String?,
        @RequestParam(required = false) depositMin: Long?,
        @RequestParam(required = false) depositMax: Long?,
        @RequestParam(required = false) monthlyRentMin: Long?,
        @RequestParam(required = false) monthlyRentMax: Long?,
        @RequestParam(required = false) sort: String?,
    ): List<PropertyListingResponse> =
        propertyQueryService.getListings(
            lat = lat,
            lng = lng,
            radiusMeters = radiusMeters,
            query = query,
            transactionType = transactionType,
            depositMin = depositMin,
            depositMax = depositMax,
            monthlyRentMin = monthlyRentMin,
            monthlyRentMax = monthlyRentMax,
            sort = sort,
        )

    @Operation(summary = "매물 상세 조회", description = "선택한 매물의 상세/분석 진입 데이터를 조회합니다.")
    @GetMapping("/{propertyId}")
    fun getDetail(@PathVariable propertyId: UUID): PropertyDetailResponse =
        propertyQueryService.getDetail(propertyId)
}
