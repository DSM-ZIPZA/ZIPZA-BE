package com.example.zipzabe.domain.analysis.dto

import com.example.zipzabe.domain.analysis.entity.FraudSuspicionLevel
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime
import java.util.UUID

@Schema(description = "대출사기 의심 패턴 분석 결과")
data class FraudPatternAnalysisResponse(
    @Schema(description = "분석 요청 ID")
    val requestId: UUID,

    @Schema(description = "반복 대출 패턴 탐지 여부 (180일 이내 근저당 2건 이상 설정)")
    val hasFrequentMortgageChange: Boolean,

    @Schema(description = "소유권 이전 직후 근저당 탐지 여부 (이전 30일 이내 설정, 허위 임차인 간접 지표)")
    val hasPostOwnershipMortgage: Boolean,

    @Schema(description = "과다 담보 여부 (총 근저당이 추정 매매가의 80% 초과)")
    val hasOverLeveraged: Boolean,

    @Schema(description = "의심 수준 (LOW: 이상 없음 / MEDIUM: 주의 / HIGH: 강한 의심)")
    val suspicionLevel: FraudSuspicionLevel,

    @Schema(description = "위험 점수 (0~100, LOW=0 / MEDIUM=25 / HIGH=50)")
    val riskScore: Int,

    @Schema(description = "탐지된 패턴 상세 설명")
    val riskReason: String,

    @Schema(description = "분석 시각")
    val analyzedAt: LocalDateTime,
)
