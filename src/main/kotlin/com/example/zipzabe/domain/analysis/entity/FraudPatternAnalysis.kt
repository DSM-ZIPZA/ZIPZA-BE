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

enum class FraudSuspicionLevel { LOW, MEDIUM, HIGH }

@Entity
@Table(name = "fraud_pattern_analysis")
class FraudPatternAnalysis(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    val request: AnalysisRequest,

    @Column(nullable = false)
    val hasFrequentMortgageChange: Boolean,

    @Column(nullable = false)
    val hasPostOwnershipMortgage: Boolean,

    @Column(nullable = false)
    val hasOverLeveraged: Boolean,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    val suspicionLevel: FraudSuspicionLevel,

    @Column(nullable = false)
    val riskScore: Int,

    @Column(nullable = false, columnDefinition = "TEXT")
    val riskReason: String,

    @Column(nullable = false, updatable = false)
    val analyzedAt: LocalDateTime = LocalDateTime.now(),
) : BaseEntity()
