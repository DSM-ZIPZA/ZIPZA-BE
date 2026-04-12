package com.example.zipzabe.domain.user.entity

import com.example.zipzabe.global.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "`user`")
class User(
    @Column(nullable = false, unique = true, length = 100)
    val email: String,

    @Column(nullable = false, length = 50)
    var nickname: String,

    @Column(nullable = false, length = 20)
    val provider: String,

    @Column(nullable = false, length = 100)
    val providerId: String,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    var deletedAt: LocalDateTime? = null,
) : BaseEntity()
