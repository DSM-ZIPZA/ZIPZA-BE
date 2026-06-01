package com.example.zipzabe.domain.building.dto

import java.time.LocalDate
import java.util.UUID
import io.swagger.v3.oas.annotations.media.Schema

data class BuildingLedgerFetchResponse(
    val buildingLedgerId: UUID,
    val mainPurposeName: String,
    @Schema(description = "연면적. ㎡ 단위")
    val totalFloorArea: Double,
    @Schema(description = "전용면적. ㎡ 단위")
    val exclusiveArea: Double,
    val approvalDate: LocalDate,
    val isViolationBuilding: Boolean,
)
