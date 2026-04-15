package com.example.zipzabe.domain.building.repository

import com.example.zipzabe.domain.building.entity.BuildingLedger
import com.example.zipzabe.domain.property.entity.Property
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface BuildingLedgerRepository : JpaRepository<BuildingLedger, UUID> {
    fun findTopByPropertyOrderByFetchedAtDesc(property: Property): BuildingLedger?
}
