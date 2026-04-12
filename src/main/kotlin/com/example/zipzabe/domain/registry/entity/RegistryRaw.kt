package com.example.zipzabe.domain.registry.entity

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
@Table(name = "registry_raw")
class RegistryRaw(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registry_candidate_id", nullable = false)
    val registryCandidate: RegistryCandidate,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    val request: AnalysisRequest,

    @Column(nullable = false, length = 30)
    val uniqueNumber: String,

    // SHA-256 해시: 64자 고정
    @Column(nullable = false, length = 64)
    val sourceHash: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var parseStatus: ParseStatus = ParseStatus.PENDING,

    @Column(nullable = false, updatable = false)
    val fetchedAt: LocalDateTime = LocalDateTime.now(),

    val expiresAt: LocalDateTime? = null,
) : BaseEntity()
