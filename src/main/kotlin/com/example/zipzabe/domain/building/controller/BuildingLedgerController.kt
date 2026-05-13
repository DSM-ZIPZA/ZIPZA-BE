package com.example.zipzabe.domain.building.controller

import com.example.zipzabe.domain.building.dto.BuildingLedgerFetchRequest
import com.example.zipzabe.domain.building.dto.BuildingLedgerFetchResponse
import com.example.zipzabe.domain.building.service.BuildingLedgerImportService
import com.example.zipzabe.global.error.exception.ErrorResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@Tag(name = "건축물대장", description = "APICK 건축물대장 PDF를 OCR해 저장하는 API")
@RestController
@RequestMapping("/api/analysis-requests/{requestId}/building-ledger")
class BuildingLedgerController(
    private val buildingLedgerImportService: BuildingLedgerImportService,
) {

    @Operation(
        summary = "건축물대장 조회 및 저장",
        description = "APICK에서 건축물대장 PDF를 가져와 OCR 후 파싱하여 저장합니다.",
    )
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "건축물대장 저장 성공",
            content = [Content(schema = Schema(implementation = BuildingLedgerFetchResponse::class))]),
        ApiResponse(responseCode = "400", description = "잘못된 요청 또는 PDF 파싱 실패",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
        ApiResponse(responseCode = "404", description = "분석 요청을 찾을 수 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
        ApiResponse(responseCode = "502", description = "APICK 또는 Google Cloud Vision 호출 실패",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
    )
    @PostMapping
    fun fetchBuildingLedger(
        @Parameter(description = "분석 요청 ID", required = true) @PathVariable requestId: UUID,
        @RequestBody request: BuildingLedgerFetchRequest,
    ): ResponseEntity<BuildingLedgerFetchResponse> {
        val response = buildingLedgerImportService.fetchAndSave(requestId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }
}
