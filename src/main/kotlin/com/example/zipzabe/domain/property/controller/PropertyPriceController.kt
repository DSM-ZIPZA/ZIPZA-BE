package com.example.zipzabe.domain.property.controller

import com.example.zipzabe.domain.property.dto.AverageSalePriceResponse
import com.example.zipzabe.domain.property.service.PropertyPriceService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "전세가", description = "지도 주거 건물 평균 전세가 조회 API")
@RestController
@RequestMapping("/api/property-prices")
class PropertyPriceController(
    private val propertyPriceService: PropertyPriceService,
) {
    @Operation(summary = "평균 전세가 조회", description = "주소/건물명으로 국토부 전월세 실거래 최근 자료를 조회하고, 없으면 기존 분석/실거래 저장값으로 보강합니다. 금액은 만원 단위입니다.")
    @GetMapping("/average-sale")
    fun getAverageSalePrice(
        @RequestParam(required = false) query: String?,
        @RequestParam(required = false) latitude: Double?,
        @RequestParam(required = false) longitude: Double?,
        @RequestParam(required = false, defaultValue = "200") radiusMeters: Double,
        @RequestParam(required = false) buildingName: String?,
        @RequestParam(required = false) isApartment: Boolean?,
        @RequestParam(required = false, defaultValue = "12") months: Int,
    ): AverageSalePriceResponse =
        propertyPriceService.getAverageSalePrice(query, latitude, longitude, radiusMeters, buildingName, isApartment, months)
}
