package com.example.zipzabe.domain.analysis.entity

import com.example.zipzabe.domain.building.entity.BuildingLedger
import com.example.zipzabe.global.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "building_analysis")
class BuildingAnalysis(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    val request: AnalysisRequest,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "building_ledger_id", nullable = false)
    val buildingLedger: BuildingLedger,

    @Column(nullable = false)
    val isPurposeMatched: Boolean,

    @Column(nullable = false)
    val isAreaMatched: Boolean,

    @Column(nullable = false)
    val areaDifference: Double,

    @Column(nullable = false)
    val isFloorInRange: Boolean,

    @Column(nullable = false)
    val buildingAge: Int,

    @Column(nullable = false)
    val riskScore: Int,

    @Column(nullable = false, columnDefinition = "TEXT")
    val riskReason: String,

    @Column(nullable = false)
    val isAddressMatched: Boolean,

    @Column(nullable = false)
    val isUsageMatched: Boolean,

    @Column(nullable = false)
    val hasViolationBuilding: Boolean,

    @Column(length = 1000)
    val violationMessage: String? = null,

    @Column(nullable = false, columnDefinition = "TEXT")
    val comparisonWarnings: String = "[]",

    @Column(nullable = false, updatable = false)
    val analyzedAt: LocalDateTime = LocalDateTime.now(),
) : BaseEntity()
