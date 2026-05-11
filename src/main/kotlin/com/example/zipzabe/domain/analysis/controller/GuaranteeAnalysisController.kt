package com.example.zipzabe.domain.analysis.controller

import com.example.zipzabe.domain.analysis.dto.GuaranteeAnalysisResponse
import com.example.zipzabe.domain.analysis.service.GuaranteeAnalysisService
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

@Tag(name = "보증가입 가능 여부 분석", description = "HUG 전세보증보험 가입 가능 여부 조회 API")
@RestController
@RequestMapping("/api/analysis-requests/{requestId}/guarantee-analysis")
class GuaranteeAnalysisController(
    private val guaranteeAnalysisService: GuaranteeAnalysisService,
) {

    @Operation(summary = "보증가입 가능 여부 분석 실행", description = "전세가율 및 지역별 한도 기준으로 HUG 보증 가입 가능 여부를 분석합니다.")
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "분석 성공",
            content = [Content(schema = Schema(implementation = GuaranteeAnalysisResponse::class))]),
        ApiResponse(responseCode = "404", description = "분석 요청을 찾을 수 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
    )
    @PostMapping
    fun analyze(
        @Parameter(description = "분석 요청 ID", required = true) @PathVariable requestId: UUID,
    ): ResponseEntity<GuaranteeAnalysisResponse> {
        val response = guaranteeAnalysisService.analyze(requestId)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @Operation(summary = "보증가입 가능 여부 분석 결과 조회", description = "저장된 보증가입 분석 결과를 조회합니다.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "조회 성공",
            content = [Content(schema = Schema(implementation = GuaranteeAnalysisResponse::class))]),
        ApiResponse(responseCode = "404", description = "분석 결과를 찾을 수 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
    )
    @GetMapping
    fun getGuaranteeAnalysis(
        @Parameter(description = "분석 요청 ID", required = true) @PathVariable requestId: UUID,
    ): GuaranteeAnalysisResponse =
        guaranteeAnalysisService.getGuaranteeAnalysis(requestId)
}
