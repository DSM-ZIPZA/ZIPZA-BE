package com.example.zipzabe.domain.analysis.entity

import com.example.zipzabe.global.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "price_analysis")
class PriceAnalysis(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    val request: AnalysisRequest,

    // 과거 실거래 기준 (만원)
    @Column(nullable = false)
    val referenceMinimum: Long,

    @Column(nullable = false)
    val referenceMaximum: Long,

    @Column(nullable = false)
    val referenceMedian: Long,

    @Column(nullable = false)
    val sampleCount: Int,

    @Column(nullable = false)
    val latestTradeDate: LocalDate,

    @Column(nullable = false)
    val isOverpriced: Boolean,

    @Column(nullable = false)
    val riskScore: Int,

    @Column(nullable = false, columnDefinition = "TEXT")
    val riskReason: String,

    @Column(nullable = false, updatable = false)
    val analyzedAt: LocalDateTime = LocalDateTime.now(),
) : BaseEntity()
