package com.example.zipzabe.domain.building.dto

import java.time.LocalDate
import java.util.UUID

data class BuildingLedgerFetchResponse(
    val buildingLedgerId: UUID,
    val mainPurposeName: String,
    val totalFloorArea: Double,
    val exclusiveArea: Double,
    val approvalDate: LocalDate,
    val isViolationBuilding: Boolean,
)
