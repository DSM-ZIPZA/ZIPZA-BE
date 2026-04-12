package com.example.zipzabe.domain.registry.entity

import com.example.zipzabe.global.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "registry_title")
class RegistryTitle(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registry_raw_id", nullable = false)
    val registryRaw: RegistryRaw,

    @Column(nullable = false, length = 50)
    val realEstateType: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    val locationAddress: String,

    @Column(length = 100)
    val buildingName: String? = null,

    @Column(length = 50)
    val floorInfo: String? = null,

    val exclusiveArea: Double? = null,

    val commonArea: Double? = null,

    @Column(length = 100)
    val purpose: String? = null,

    @Column(length = 50)
    val landRightType: String? = null,

    // 비율은 "3/100" 형태 문자열로 저장
    @Column(length = 100)
    val landRightRatio: String? = null,
) : BaseEntity()
