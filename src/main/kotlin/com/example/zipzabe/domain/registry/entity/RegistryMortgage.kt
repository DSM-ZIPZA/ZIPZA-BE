package com.example.zipzabe.domain.registry.entity

import com.example.zipzabe.global.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDate

@Entity
@Table(name = "registry_mortgage")
class RegistryMortgage(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registry_raw_id", nullable = false)
    val registryRaw: RegistryRaw,

    @Column(nullable = false)
    val rankNumber: Int,

    // 근저당/전세권/임차권
    @Column(nullable = false, length = 100)
    val registrationPurpose: String,

    val receptionDate: LocalDate? = null,

    @Column(length = 200)
    val registrationCause: String? = null,

    // 채권최고액/전세금/보증금 (만원 단위), 계약 종류에 따라 null 가능
    val claimAmount: Long? = null,

    @Column(length = 100)
    val debtorName: String? = null,

    @Column(length = 100)
    val creditorName: String? = null,

    @Column(nullable = false)
    val isErased: Boolean = false,

    val eraseDate: LocalDate? = null,
) : BaseEntity()
