package com.example.zipzabe.domain.registry.entity

import com.example.zipzabe.domain.property.entity.Property
import com.example.zipzabe.global.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "registry_candidate")
class RegistryCandidate(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    val property: Property,

    @Column(nullable = false, length = 30)
    val uniqueNumber: String,

    @Column(nullable = false, length = 50)
    val realEstateType: String,

    // 소재지번은 길 수 있어 TEXT 사용
    @Column(nullable = false, columnDefinition = "TEXT")
    val location: String,

    @Column(nullable = false)
    var isSelected: Boolean = false,

    @Column(nullable = false, updatable = false)
    val fetchedAt: LocalDateTime = LocalDateTime.now(),
) : BaseEntity()
