package com.example.zipzabe.domain.trade.entity

import com.example.zipzabe.domain.property.entity.Property
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
@Table(name = "trade_record")
class TradeRecord(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    val property: Property,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    val buildingType: BuildingType,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val contractType: ContractType,

    @Column(nullable = false)
    val depositAmount: Long,

    // 전세일 경우 null
    val monthlyRent: Long? = null,

    @Column(nullable = false)
    val exclusiveArea: Double,

    @Column(nullable = false)
    val floor: Int,

    @Column(nullable = false)
    val contractDate: LocalDate,

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    val contractClassification: ContractClassification? = null,

    @Column(length = 50)
    val contractTerm: String? = null,

    // 갱신 계약일 경우에만 존재
    val previousDeposit: Long? = null,

    val previousMonthlyRent: Long? = null,

    @Column(nullable = false, updatable = false)
    val fetchedAt: LocalDateTime = LocalDateTime.now(),
) : BaseEntity()
