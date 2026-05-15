package com.example.zipzabe.domain.analysis.controller

import com.example.zipzabe.domain.analysis.dto.AnalysisRequestCreateRequest
import com.example.zipzabe.domain.analysis.dto.AnalysisRequestResponse
import com.example.zipzabe.domain.analysis.dto.AnalysisRequestSummaryResponse
import com.example.zipzabe.domain.analysis.service.AnalysisRequestService
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

@Tag(name = "분석 요청", description = "분석 요청 생성과 대시보드용 통합 상태 조회 API")
@RestController
@RequestMapping("/api/analysis-requests")
class AnalysisRequestController(
    private val analysisRequestService: AnalysisRequestService,
) {

    @Operation(summary = "분석 요청 생성", description = "매물 정보와 계약 정보를 저장하고 분석 요청을 생성합니다.")
    @ApiResponses(
        ApiResponse(
            responseCode = "201",
            description = "분석 요청 생성 성공",
            content = [Content(schema = Schema(implementation = AnalysisRequestResponse::class))],
        ),
        ApiResponse(
            responseCode = "401",
            description = "인증 실패",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))],
        ),
    )
    @PostMapping
    fun create(
        @RequestBody request: AnalysisRequestCreateRequest,
    ): ResponseEntity<AnalysisRequestResponse> {
        val response = analysisRequestService.create(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @Operation(summary = "내 분석 요청 목록 조회", description = "현재 로그인한 사용자의 분석 요청 목록을 최신순으로 조회합니다.")
    @GetMapping
    fun getAll(): List<AnalysisRequestResponse> =
        analysisRequestService.getAll()

    @Operation(summary = "분석 요청 상세 조회", description = "현재 로그인한 사용자의 특정 분석 요청을 조회합니다.")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "조회 성공",
            content = [Content(schema = Schema(implementation = AnalysisRequestResponse::class))],
        ),
        ApiResponse(
            responseCode = "404",
            description = "분석 요청을 찾을 수 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))],
        ),
    )
    @GetMapping("/{requestId}")
    fun get(
        @Parameter(description = "분석 요청 ID", required = true) @PathVariable requestId: UUID,
    ): AnalysisRequestResponse =
        analysisRequestService.get(requestId)

    @Operation(summary = "분석 통합 상태 조회", description = "문서 수집, OCR, 실거래가 수집, 개별 분석, 종합 리포트 상태를 한 번에 조회합니다.")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "통합 상태 조회 성공",
            content = [Content(schema = Schema(implementation = AnalysisRequestSummaryResponse::class))],
        ),
        ApiResponse(
            responseCode = "404",
            description = "분석 요청을 찾을 수 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))],
        ),
    )
    @GetMapping("/{requestId}/summary")
    fun getSummary(
        @Parameter(description = "분석 요청 ID", required = true) @PathVariable requestId: UUID,
    ): AnalysisRequestSummaryResponse =
        analysisRequestService.getSummary(requestId)
}
