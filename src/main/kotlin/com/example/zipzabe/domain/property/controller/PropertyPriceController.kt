package com.example.zipzabe.domain.property.controller

import com.example.zipzabe.domain.property.dto.AverageSalePriceResponse
import com.example.zipzabe.domain.property.service.PropertyPriceService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "매매가", description = "지도 주거 건물 평균 매매가 조회 API")
@RestController
@RequestMapping("/api/property-prices")
class PropertyPriceController(
    private val propertyPriceService: PropertyPriceService,
) {
    @Operation(summary = "평균 매매가 조회", description = "주소/좌표로 매칭되는 기존 분석 결과의 추정 매매가 평균을 조회합니다. 금액은 만원 단위입니다.")
    @GetMapping("/average-sale")
    fun getAverageSalePrice(
        @RequestParam(required = false) query: String?,
        @RequestParam(required = false) latitude: Double?,
        @RequestParam(required = false) longitude: Double?,
        @RequestParam(required = false, defaultValue = "200") radiusMeters: Double,
    ): AverageSalePriceResponse =
        propertyPriceService.getAverageSalePrice(query, latitude, longitude, radiusMeters)
}
