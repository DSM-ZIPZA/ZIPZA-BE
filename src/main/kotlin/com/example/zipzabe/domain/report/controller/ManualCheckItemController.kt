package com.example.zipzabe.domain.report.controller

import com.example.zipzabe.domain.report.dto.ManualCheckItemResponse
import com.example.zipzabe.domain.report.dto.ManualCheckListResponse
import com.example.zipzabe.domain.report.service.ManualCheckItemService
import com.example.zipzabe.global.error.exception.ErrorResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@Tag(name = "수동 확인 항목", description = "전입세대확인서·미납국세·임금채권 등 직접 확인이 필요한 안내 항목 API")
@RestController
@RequestMapping("/api/analysis-requests/{requestId}/manual-checks")
class ManualCheckItemController(
    private val manualCheckItemService: ManualCheckItemService,
) {

    @Operation(summary = "수동 확인 항목 목록 조회", description = "분석 요청에 대한 전입세대확인서·미납국세·임금채권 안내 항목을 조회합니다.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "조회 성공",
            content = [Content(schema = Schema(implementation = ManualCheckListResponse::class))]),
        ApiResponse(responseCode = "404", description = "분석 요청을 찾을 수 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
    )
    @GetMapping
    fun list(
        @Parameter(description = "분석 요청 ID", required = true) @PathVariable requestId: UUID,
    ): ManualCheckListResponse = manualCheckItemService.list(requestId)

    @Operation(summary = "확인 완료 처리", description = "수동 확인 항목을 완료 상태로 변경합니다.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "처리 성공",
            content = [Content(schema = Schema(implementation = ManualCheckItemResponse::class))]),
        ApiResponse(responseCode = "404", description = "항목을 찾을 수 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
    )
    @PatchMapping("/{itemId}/complete")
    fun complete(
        @Parameter(description = "분석 요청 ID", required = true) @PathVariable requestId: UUID,
        @Parameter(description = "항목 ID", required = true) @PathVariable itemId: UUID,
    ): ManualCheckItemResponse = manualCheckItemService.markCompleted(requestId, itemId)
}
