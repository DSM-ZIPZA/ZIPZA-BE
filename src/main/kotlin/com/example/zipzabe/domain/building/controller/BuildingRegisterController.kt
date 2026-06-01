package com.example.zipzabe.domain.building.controller

import com.example.zipzabe.domain.building.dto.BuildingRegisterListResponse
import com.example.zipzabe.domain.building.service.BuildingService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "건축물대장 목록", description = "주소 기반 건축물대장 동/호 목록 조회 API")
@RestController
@RequestMapping("/api/building-registers")
class BuildingRegisterController(
    private val buildingService: BuildingService,
) {
    @Operation(summary = "건축물대장 목록 조회", description = "주소로 건축물대장 후보 동/호 목록을 조회합니다.")
    @GetMapping
    fun getBuildingRegisters(@RequestParam address: String): BuildingRegisterListResponse =
        buildingService.getBuildingRegisterList(address)
}
