package com.example.zipzabe.domain.report.dto

import com.example.zipzabe.domain.report.entity.CheckType
import java.time.LocalDateTime
import java.util.UUID

data class ManualCheckItemResponse(
    val id: UUID,
    val checkType: CheckType,
    val title: String,
    val badgeText: String?,
    val severity: RiskSeverity,
    val guideText: String,
    val procedureSteps: List<String>,
    val officialUrl: String?,
    val expertConsult: Boolean,
    val isCompleted: Boolean,
    val checkedAt: LocalDateTime?,
)

data class ManualCheckListResponse(
    val requestId: UUID,
    val items: List<ManualCheckItemResponse>,
)
