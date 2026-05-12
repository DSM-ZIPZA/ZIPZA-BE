package com.example.zipzabe.domain.analysis.controller

import com.example.zipzabe.domain.analysis.dto.PublicLedgerSummaryResponse
import com.example.zipzabe.domain.analysis.service.PublicLedgerSummaryService
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

@Tag(name = "공시지가 요약 분석", description = "부동산 분석 요청에 대한 공시지가 요약 분석 API")
@RestController
@RequestMapping("/api/analysis-requests/{requestId}/public-ledger-summary")
class PublicLedgerSummaryController(
    private val publicLedgerSummaryService: PublicLedgerSummaryService,
) {

    @Operation(summary = "공시지가 요약 분석 실행", description = "분석 요청 ID에 해당하는 등기부등본과 건축물대장을 비교 분석합니다.")
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "분석 성공",
            content = [Content(schema = Schema(implementation = PublicLedgerSummaryResponse::class))]),
        ApiResponse(responseCode = "404", description = "분석 요청을 찾을 수 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
        ApiResponse(responseCode = "500", description = "서버 내부 오류",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
    )
    @PostMapping
    fun analyze(
        @Parameter(description = "분석 요청 ID", required = true) @PathVariable requestId: UUID,
    ): ResponseEntity<PublicLedgerSummaryResponse> {
        val response = publicLedgerSummaryService.analyze(requestId)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @Operation(summary = "공시지가 요약 분석 결과 조회", description = "이미 분석된 공시지가 요약 결과를 조회합니다.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "조회 성공",
            content = [Content(schema = Schema(implementation = PublicLedgerSummaryResponse::class))]),
        ApiResponse(responseCode = "404", description = "분석 결과를 찾을 수 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
        ApiResponse(responseCode = "500", description = "서버 내부 오류",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
    )
    @GetMapping
    fun getSummary(
        @Parameter(description = "분석 요청 ID", required = true) @PathVariable requestId: UUID,
    ): PublicLedgerSummaryResponse =
        publicLedgerSummaryService.getSummary(requestId)
}
