package com.example.zipzabe.domain.analysis.dto

import com.example.zipzabe.domain.analysis.entity.GuaranteeResult
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime
import java.util.UUID

@Schema(description = "보증가입 가능 여부 분석 결과")
data class GuaranteeAnalysisResponse(
    @Schema(description = "분석 요청 ID")
    val requestId: UUID,

    @Schema(description = "분석 요청 보증금 (만원 단위)")
    val depositAmount: Long,

    @Schema(description = "추정 매매가 (만원 단위, 전세가율 70% 역산)")
    val estimatedPropertyValue: Long,

    @Schema(description = "전세가율 (보증금 / 추정 매매가 × 100, %)")
    val jeonseRate: Double,

    @Schema(description = "지역별 보증 한도 (수도권 70,000만원 / 그 외 50,000만원)")
    val regionMaxDeposit: Long,

    @Schema(description = "보증 가입 가능 여부 (POSSIBLE: 가능 / NEEDS_ADDITIONAL_CHECK: 추가확인 필요 / NOT_POSSIBLE: 불가)")
    val guaranteeResult: GuaranteeResult,

    @Schema(description = "위험 점수 (0~100, POSSIBLE=0 / NEEDS_ADDITIONAL_CHECK=20 / NOT_POSSIBLE=50)")
    val riskScore: Int,

    @Schema(description = "판정 사유 설명")
    val riskReason: String,

    @Schema(description = "분석 시각")
    val analyzedAt: LocalDateTime,
)
