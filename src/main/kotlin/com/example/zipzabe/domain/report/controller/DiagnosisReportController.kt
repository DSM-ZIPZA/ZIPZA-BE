package com.example.zipzabe.domain.report.controller

import com.example.zipzabe.domain.report.dto.DiagnosisReportResponse
import com.example.zipzabe.domain.report.dto.DiagnosisSupplementRequest
import com.example.zipzabe.domain.report.service.DiagnosisReportService
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
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@Tag(name = "종합 위험도 리포트", description = "분석 결과와 보조 입력을 종합해 거래 위험 권고를 생성하는 API")
@RestController
@RequestMapping("/api/analysis-requests/{requestId}/diagnosis-report")
class DiagnosisReportController(
    private val diagnosisReportService: DiagnosisReportService,
) {

    @Operation(summary = "종합 위험도 리포트 생성", description = "시세, 권리, 보증, 회수가능성, 특약, 임대인, 점유 정보를 종합합니다.")
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "리포트 생성 성공",
            content = [Content(schema = Schema(implementation = DiagnosisReportResponse::class))]),
        ApiResponse(responseCode = "404", description = "분석 요청을 찾을 수 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
    )
    @PostMapping
    fun createReport(
        @Parameter(description = "분석 요청 ID", required = true) @PathVariable requestId: UUID,
        @RequestBody(required = false) request: DiagnosisSupplementRequest?,
    ): ResponseEntity<DiagnosisReportResponse> {
        val response = diagnosisReportService.createReport(requestId, request ?: DiagnosisSupplementRequest())
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @Operation(summary = "종합 위험도 리포트 조회", description = "최신 종합 위험도 리포트를 조회합니다.")
    @GetMapping
    fun getReport(
        @Parameter(description = "분석 요청 ID", required = true) @PathVariable requestId: UUID,
    ): DiagnosisReportResponse =
        diagnosisReportService.getReport(requestId)
}
