package com.example.zipzabe.domain.analysis.entity

import com.example.zipzabe.global.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDateTime

enum class RecoveryGrade { SAFE, CAUTION, DANGER }

@Entity
@Table(name = "recovery_analysis")
class RecoveryAnalysis(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    val request: AnalysisRequest,

    @Column(nullable = false)
    val estimatedPropertyValue: Long,

    @Column(nullable = false)
    val totalEncumbrance: Long,

    @Column(nullable = false)
    val depositAmount: Long,

    @Column(nullable = false)
    val availableForTenant: Long,

    @Column(nullable = false)
    val recoveryRate: Double,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    val recoveryGrade: RecoveryGrade,

    @Column(nullable = false)
    val riskScore: Int,

    @Column(nullable = false, columnDefinition = "TEXT")
    val riskReason: String,

    @Column(nullable = false, updatable = false)
    val analyzedAt: LocalDateTime = LocalDateTime.now(),
) : BaseEntity()
