package com.example.zipzabe.domain.analysis.entity

import com.example.zipzabe.domain.property.entity.Property
import com.example.zipzabe.domain.user.entity.User
import com.example.zipzabe.global.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "analysis_request")
class AnalysisRequest(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    val property: Property,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val contractType: ContractType,

    @Column(nullable = false)
    val depositAmount: Long,

    // 월세일 경우에만 존재
    val monthlyRent: Long? = null,

    @Column(nullable = false)
    val floor: Int,

    @Column(nullable = false)
    val exclusiveArea: Double,

    @Column(nullable = false)
    val contractDate: LocalDate,

    @Column(nullable = false)
    val balanceDate: LocalDate,

    @Column(nullable = false)
    val expiryDate: LocalDate,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: AnalysisStatus = AnalysisStatus.PENDING,

    @Column(nullable = false, updatable = false)
    val requestedAt: LocalDateTime = LocalDateTime.now(),

    var completedAt: LocalDateTime? = null,
) : BaseEntity()
