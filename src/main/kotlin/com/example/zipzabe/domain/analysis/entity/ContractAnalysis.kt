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

@Entity
@Table(name = "contract_analysis")
class ContractAnalysis(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    val request: AnalysisRequest,

    @Column(nullable = false)
    val hasProxy: Boolean,

    @Column(nullable = false)
    val isPenaltyClauseMissing: Boolean,

    @Column(nullable = false)
    val isReturnClauseMissing: Boolean,

    @Column(nullable = false)
    val isRepairClauseMissing: Boolean,

    // JSON 배열: [{"clause": "...", "reason": "..."}]
    @Column(nullable = false, columnDefinition = "JSON")
    val riskClauses: String,

    // JSON 배열: [{"clause": "...", "reason": "..."}]
    @Column(nullable = false, columnDefinition = "JSON")
    val recommendedClauses: String,

    // SHA-256 해시: 64자 고정
    @Column(nullable = false, length = 64)
    val sourceHash: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var parseStatus: ParseStatus = ParseStatus.PENDING,

    @Column(nullable = false)
    val riskScore: Int,

    @Column(nullable = false, columnDefinition = "TEXT")
    val riskReason: String,

    @Column(nullable = false, updatable = false)
    val analyzedAt: LocalDateTime = LocalDateTime.now(),
) : BaseEntity()
