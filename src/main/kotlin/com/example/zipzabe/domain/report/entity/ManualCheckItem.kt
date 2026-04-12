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
@Table(name = "manual_check_item")
class ManualCheckItem(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    val request: AnalysisRequest,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    val checkType: CheckType,

    @Column(nullable = false, columnDefinition = "TEXT")
    val guideText: String,

    @Column(length = 500)
    val officialUrl: String? = null,

    @Column(nullable = false)
    var isCompleted: Boolean = false,

    var checkedAt: LocalDateTime? = null,
) : BaseEntity()
