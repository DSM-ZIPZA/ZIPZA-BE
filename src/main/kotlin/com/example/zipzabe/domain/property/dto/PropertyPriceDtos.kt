package com.example.zipzabe.domain.property.dto

import io.swagger.v3.oas.annotations.media.Schema

data class AverageSalePriceResponse(
    val query: String?,
    val latitude: Double?,
    val longitude: Double?,
    @Schema(description = "평균 추정 매매가. 만원 단위")
    val averageSalePriceManwon: Long?,
    val sampleCount: Int,
)
