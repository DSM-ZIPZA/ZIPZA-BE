package com.example.zipzabe.domain.analysis.controller

import com.example.zipzabe.domain.analysis.dto.AnalysisStartRequest
import com.example.zipzabe.domain.analysis.dto.AnalysisStartResponse
import com.example.zipzabe.domain.analysis.service.AnalysisStartService
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

@Tag(name = "분석 시작", description = "분석에 필요한 문서 수집/OCR부터 종합 리포트 생성까지 실행하는 API")
@RestController
@RequestMapping("/api/analysis-requests/{requestId}/analysis")
class AnalysisStartController(
    private val analysisStartService: AnalysisStartService,
) {

    @Operation(
        summary = "분석 전체 실행",
        description = "건축물대장과 등기부등본을 자동 발급/OCR/저장한 뒤 실거래가 수집, 개별 분석, 종합 리포트 생성을 순서대로 실행합니다.",
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "201",
            description = "분석 전체 실행 성공",
            content = [Content(schema = Schema(implementation = AnalysisStartResponse::class))],
        ),
        ApiResponse(
            responseCode = "400",
            description = "잘못된 문서 발급/OCR 요청",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))],
        ),
        ApiResponse(
            responseCode = "404",
            description = "분석 요청 또는 필요한 분석 데이터를 찾을 수 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))],
        ),
        ApiResponse(
            responseCode = "502",
            description = "외부 API 호출 실패",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))],
        ),
    )
    @PostMapping("/start")
    fun start(
        @Parameter(description = "분석 요청 ID", required = true) @PathVariable requestId: UUID,
        @RequestBody request: AnalysisStartRequest,
    ): ResponseEntity<AnalysisStartResponse> {
        val response = analysisStartService.start(requestId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }
}
