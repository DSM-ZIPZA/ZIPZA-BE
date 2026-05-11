package com.example.zipzabe.domain.analysis.dto

import com.example.zipzabe.domain.analysis.entity.RecoveryGrade
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime
import java.util.UUID

@Schema(description = "회수가능성 분석 결과")
data class RecoveryAnalysisResponse(
    @Schema(description = "분석 요청 ID")
    val requestId: UUID,

    @Schema(description = "추정 매매가 (만원 단위, 전세가율 70% 역산)")
    val estimatedPropertyValue: Long,

    @Schema(description = "선순위 부담액 합계 (근저당 + 전세권, 만원 단위)")
    val totalEncumbrance: Long,

    @Schema(description = "분석 요청 보증금 (만원 단위)")
    val depositAmount: Long,

    @Schema(description = "경매 낙찰 후 임차인 회수 가능액 (만원 단위)")
    val availableForTenant: Long,

    @Schema(description = "보증금 대비 회수율 (0.0 ~ 100.0 %)")
    val recoveryRate: Double,

    @Schema(description = "회수 위험 등급 (SAFE: 80% 이상 / CAUTION: 60~80% / DANGER: 60% 미만)")
    val recoveryGrade: RecoveryGrade,

    @Schema(description = "위험 점수 (0~100, SAFE=0 / CAUTION=40 / DANGER=80)")
    val riskScore: Int,

    @Schema(description = "위험 사유 설명")
    val riskReason: String,

    @Schema(description = "분석 시각")
    val analyzedAt: LocalDateTime,
)
