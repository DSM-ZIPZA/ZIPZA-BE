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
@Table(name = "registry_restriction")
class RegistryRestriction(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registry_raw_id", nullable = false)
    val registryRaw: RegistryRaw,

    @Column(nullable = false)
    val rankNumber: Int,

    // 압류/가압류/신탁 등
    @Column(nullable = false, length = 100)
    val registrationPurpose: String,

    val receptionDate: LocalDate? = null,

    @Column(length = 200)
    val registrationCause: String? = null,

    @Column(length = 100)
    val rightHolderName: String? = null,

    @Column(columnDefinition = "TEXT")
    val detail: String? = null,

    @Column(nullable = false)
    val isErased: Boolean = false,

    val eraseDate: LocalDate? = null,
) : BaseEntity()
