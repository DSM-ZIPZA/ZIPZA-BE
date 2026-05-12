package com.example.zipzabe.domain.analysis.controller

import com.example.zipzabe.domain.analysis.dto.PriceAnalysisResponse
import com.example.zipzabe.domain.analysis.service.PriceAnalysisService
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
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@Tag(name = "시세 부풀리기 분석", description = "저장된 전월세 실거래가 기반 시세 부풀리기 탐지 API")
@RestController
@RequestMapping("/api/analysis-requests/{requestId}/price-analysis")
class PriceAnalysisController(
    private val priceAnalysisService: PriceAnalysisService,
) {

    @Operation(summary = "시세 부풀리기 분석 실행", description = "유사 전세 실거래 중앙값 대비 보증금 수준을 분석합니다.")
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "분석 성공",
            content = [Content(schema = Schema(implementation = PriceAnalysisResponse::class))]),
        ApiResponse(responseCode = "404", description = "분석 요청을 찾을 수 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
    )
    @PostMapping
    fun analyze(
        @Parameter(description = "분석 요청 ID", required = true) @PathVariable requestId: UUID,
        @Parameter(description = "조회 개월 수. 1~60 사이로 보정됩니다.")
        @RequestParam(defaultValue = "24") months: Int,
    ): ResponseEntity<PriceAnalysisResponse> {
        val response = priceAnalysisService.analyze(requestId, months)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @Operation(summary = "시세 부풀리기 분석 결과 조회", description = "저장된 시세 분석 결과를 조회합니다.")
    @GetMapping
    fun getPriceAnalysis(
        @Parameter(description = "분석 요청 ID", required = true) @PathVariable requestId: UUID,
    ): PriceAnalysisResponse =
        priceAnalysisService.getPriceAnalysis(requestId)
}
