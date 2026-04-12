package com.example.zipzabe.domain.report.entity

import com.example.zipzabe.domain.analysis.entity.AnalysisRequest
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

@Entity
@Table(name = "diagnosis_report")
class DiagnosisReport(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    val request: AnalysisRequest,

    @Column(nullable = false)
    val priceScore: Int,

    @Column(nullable = false)
    val rightsScore: Int,

    @Column(nullable = false)
    val buildingScore: Int,

    @Column(nullable = false)
    val contractScore: Int,

    @Column(nullable = false)
    val confidenceScore: Int,

    @Column(nullable = false)
    val totalScore: Int,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    val verdict: Verdict,

    // JSON 배열: 핵심 위험 사유 최대 3개 [{"title": "...", "detail": "..."}]
    @Column(nullable = false, columnDefinition = "JSON")
    val topRisks: String,

    // JSON 배열: 다음 행동 가이드 [{"action": "...", "priority": "..."}]
    @Column(nullable = false, columnDefinition = "JSON")
    val nextActions: String,

    @Column(length = 100)
    var shareToken: String? = null,

    @Column(nullable = false)
    var isShared: Boolean = false,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
) : BaseEntity()
