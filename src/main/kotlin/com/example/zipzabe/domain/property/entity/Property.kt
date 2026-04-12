package com.example.zipzabe.domain.property.entity

import com.example.zipzabe.global.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "property")
class Property(
    @Column(nullable = false, length = 200)
    val roadAddress: String,

    @Column(nullable = false, length = 200)
    val jibunAddress: String,

    @Column(length = 100)
    val detailAddress: String? = null,

    @Column(nullable = false, length = 50)
    val buildingManagementNumber: String,

    @Column(nullable = false, length = 10)
    val postalCode: String,

    @Column(nullable = false, length = 20)
    val administrativeCode: String,

    @Column(nullable = false, length = 30)
    val city: String,

    @Column(nullable = false, length = 30)
    val district: String,

    @Column(nullable = false, length = 30)
    val neighborhood: String,

    @Column(length = 100)
    val buildingName: String? = null,

    @Column(nullable = false)
    val isApartment: Boolean,

    // 경도/위도는 GPS 정밀도를 위해 Double 사용
    @Column(nullable = false)
    val longitude: Double,

    @Column(nullable = false)
    val latitude: Double,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
) : BaseEntity()
