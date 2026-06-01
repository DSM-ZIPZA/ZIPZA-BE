package com.example.zipzabe.domain.analysis.dto

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import io.swagger.v3.oas.annotations.media.Schema

enum class PriceAnalysisStatus {
    NORMAL,
    CAUTION,
    DANGER,
    INSUFFICIENT_DATA,
}

data class PriceAnalysisResponse(
    val requestId: UUID,
    @Schema(description = "보증금. 만원 단위")
    val depositAmount: Long,
    @Schema(description = "유사 거래 최솟값. 만원 단위")
    val referenceMinimum: Long,
    @Schema(description = "유사 거래 최댓값. 만원 단위")
    val referenceMaximum: Long,
    @Schema(description = "유사 거래 중앙값. 만원 단위")
    val referenceMedian: Long,
    val sampleCount: Int,
    val latestTradeDate: LocalDate,
    val priceRatio: Double?,
    val status: PriceAnalysisStatus,
    val isOverpriced: Boolean,
    val riskScore: Int,
    val riskReason: String,
    val analyzedAt: LocalDateTime,
)
