package com.example.zipzabe.domain.analysis.dto

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

enum class PriceAnalysisStatus {
    NORMAL,
    CAUTION,
    DANGER,
    INSUFFICIENT_DATA,
}

data class PriceAnalysisResponse(
    val requestId: UUID,
    val depositAmount: Long,
    val referenceMinimum: Long,
    val referenceMaximum: Long,
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
