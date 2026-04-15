package com.example.zipzabe.domain.analysis.entity

import com.example.zipzabe.domain.registry.entity.RegistryRaw
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
@Table(name = "rights_analysis")
class RightsAnalysis(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    val request: AnalysisRequest,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registry_raw_id", nullable = false)
    val registryRaw: RegistryRaw,

    @Column(nullable = false, length = 100)
    val currentOwner: String,

    @Column(nullable = false)
    val isOwnerMatched: Boolean,

    @Column(nullable = false)
    val isRecentlyOwnerChanged: Boolean,

    @Column(nullable = false)
    val ownerChangeCount: Int,

    // 말소 제외 근저당 합계 (만원)
    @Column(nullable = false)
    val totalMortgageAmount: Long,

    @Column(nullable = false)
    val totalJeonseRightAmount: Long,

    @Column(nullable = false)
    val totalLeaseRightAmount: Long,

    @Column(nullable = false)
    val hasSeizure: Boolean,

    @Column(nullable = false)
    val hasProvisionalSeizure: Boolean,

    @Column(nullable = false)
    val hasForcedAuction: Boolean,

    @Column(nullable = false)
    val hasTrust: Boolean,

    @Column(nullable = false)
    val hasLeaseRegistration: Boolean,

    @Column(nullable = false)
    val registryDate: LocalDate,

    @Column(nullable = false)
    val riskScore: Int,

    @Column(nullable = false, columnDefinition = "TEXT")
    val riskReason: String,

    val latestOwnerChangeDate: LocalDate? = null,

    @Column(nullable = false)
    val trustEntryCount: Int = 0,

    @Column(nullable = false, columnDefinition = "TEXT")
    val summaryText: String = "",

    @Column(nullable = false, updatable = false)
    val analyzedAt: LocalDateTime = LocalDateTime.now(),
) : BaseEntity()
