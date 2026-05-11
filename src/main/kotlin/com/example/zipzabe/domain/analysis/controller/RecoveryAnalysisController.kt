package com.example.zipzabe.domain.analysis.controller

import com.example.zipzabe.domain.analysis.dto.RecoveryAnalysisResponse
import com.example.zipzabe.domain.analysis.service.RecoveryAnalysisService
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
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@Tag(name = "회수가능성 분석", description = "경매 시 보증금 회수가능성 분석 API")
@RestController
@RequestMapping("/api/analysis-requests/{requestId}/recovery-analysis")
class RecoveryAnalysisController(
    private val recoveryAnalysisService: RecoveryAnalysisService,
) {

    @Operation(summary = "회수가능성 분석 실행", description = "근저당 및 실거래가 기반으로 경매 시 보증금 회수율을 분석합니다.")
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "분석 성공",
            content = [Content(schema = Schema(implementation = RecoveryAnalysisResponse::class))]),
        ApiResponse(responseCode = "404", description = "분석 요청 또는 권리 분석 결과를 찾을 수 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
    )
    @PostMapping
    fun analyze(
        @Parameter(description = "분석 요청 ID", required = true) @PathVariable requestId: UUID,
    ): ResponseEntity<RecoveryAnalysisResponse> {
        val response = recoveryAnalysisService.analyze(requestId)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @Operation(summary = "회수가능성 분석 결과 조회", description = "저장된 회수가능성 분석 결과를 조회합니다.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "조회 성공",
            content = [Content(schema = Schema(implementation = RecoveryAnalysisResponse::class))]),
        ApiResponse(responseCode = "404", description = "분석 결과를 찾을 수 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
    )
    @GetMapping
    fun getRecoveryAnalysis(
        @Parameter(description = "분석 요청 ID", required = true) @PathVariable requestId: UUID,
    ): RecoveryAnalysisResponse =
        recoveryAnalysisService.getRecoveryAnalysis(requestId)
}
