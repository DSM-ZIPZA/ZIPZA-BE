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
@Table(name = "registry_ownership")
class RegistryOwnership(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registry_raw_id", nullable = false)
    val registryRaw: RegistryRaw,

    @Column(nullable = false)
    val rankNumber: Int,

    @Column(nullable = false, length = 100)
    val registrationPurpose: String,

    val receptionDate: LocalDate? = null,

    @Column(length = 100)
    val registrationCause: String? = null,

    val registrationCauseDate: LocalDate? = null,

    @Column(nullable = false, length = 100)
    val ownerName: String,

    // 마스킹된 식별번호 ex) 123456-*******
    @Column(length = 50)
    val ownerIdMasked: String? = null,

    @Column(length = 50)
    val shareRatio: String? = null,

    @Column(nullable = false)
    val isCurrent: Boolean,
) : BaseEntity()
