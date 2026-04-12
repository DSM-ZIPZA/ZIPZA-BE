package com.example.zipzabe.domain.building.entity

import com.example.zipzabe.domain.property.entity.Property
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
@Table(name = "building_ledger")
class BuildingLedger(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    val property: Property,

    @Column(nullable = false, length = 20)
    val mainPurposeCode: String,

    @Column(nullable = false, length = 100)
    val mainPurposeName: String,

    @Column(nullable = false)
    val totalFloorArea: Double,

    @Column(nullable = false)
    val buildingArea: Double,

    @Column(nullable = false)
    val buildingCoverageRatio: Double,

    @Column(nullable = false)
    val floorAreaRatio: Double,

    @Column(nullable = false, length = 100)
    val structureName: String,

    @Column(nullable = false)
    val floorsAboveGround: Int,

    @Column(nullable = false)
    val floorsUnderground: Int,

    @Column(nullable = false)
    val householdCount: Int,

    @Column(nullable = false)
    val approvalDate: LocalDate,

    @Column(nullable = false)
    val isEarthquakeResistant: Boolean,

    @Column(nullable = false)
    val exclusiveArea: Double,

    @Column(nullable = false, updatable = false)
    val fetchedAt: LocalDateTime = LocalDateTime.now(),
) : BaseEntity()
