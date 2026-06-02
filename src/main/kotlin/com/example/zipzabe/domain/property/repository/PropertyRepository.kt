package com.example.zipzabe.domain.property.repository

import com.example.zipzabe.domain.property.entity.Property
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface PropertyRepository : JpaRepository<Property, UUID> {
    fun findByRoadAddressContainingOrJibunAddressContainingOrBuildingNameContaining(
        roadAddress: String,
        jibunAddress: String,
        buildingName: String,
    ): List<Property>

    fun findByLatitudeBetweenAndLongitudeBetween(
        minLatitude: Double,
        maxLatitude: Double,
        minLongitude: Double,
        maxLongitude: Double,
    ): List<Property>
}
