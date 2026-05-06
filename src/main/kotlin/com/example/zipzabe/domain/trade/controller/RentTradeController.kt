package com.example.zipzabe.domain.trade.controller

import com.example.zipzabe.domain.trade.dto.RentTradeFetchResponse
import com.example.zipzabe.domain.trade.dto.RentTradeListResponse
import com.example.zipzabe.domain.trade.entity.BuildingType
import com.example.zipzabe.domain.trade.service.RentTradeService
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

@Tag(name = "전월세 실거래가", description = "국토교통부 전월세 실거래가 조회 API")
@RestController
@RequestMapping("/api/analysis-requests/{requestId}/rent-trades")
class RentTradeController(
    private val rentTradeService: RentTradeService,
) {

    @Operation(
        summary = "전월세 실거래가 수집",
        description = "분석 요청의 매물 정보로 국토교통부 전월세 실거래가를 조회하고 저장합니다."
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "201",
            description = "수집 성공",
            content = [Content(schema = Schema(implementation = RentTradeFetchResponse::class))]
        ),
        ApiResponse(
            responseCode = "404",
            description = "분석 요청을 찾을 수 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "502",
            description = "외부 API 오류",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
    )
    @PostMapping("/fetch")
    fun fetchRentTrades(
        @Parameter(description = "분석 요청 ID", required = true)
        @PathVariable requestId: UUID,
        @Parameter(description = "조회 개월 수. 1~60 사이로 보정됩니다.")
        @RequestParam(defaultValue = "24") months: Int,
        @Parameter(description = "건물 유형. 생략하면 매물/건축물대장 정보로 추론합니다.")
        @RequestParam(required = false) buildingType: BuildingType?,
    ): ResponseEntity<RentTradeFetchResponse> {
        val response = rentTradeService.fetchRentTrades(
            requestId = requestId,
            months = months,
            buildingType = buildingType,
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @Operation(summary = "저장된 전월세 실거래가 조회", description = "분석 요청 매물에 저장된 전월세 실거래가를 조회합니다.")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "조회 성공",
            content = [Content(schema = Schema(implementation = RentTradeListResponse::class))]
        ),
        ApiResponse(
            responseCode = "404",
            description = "분석 요청을 찾을 수 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
    )
    @GetMapping
    fun getRentTrades(
        @Parameter(description = "분석 요청 ID", required = true)
        @PathVariable requestId: UUID,
    ): RentTradeListResponse =
        rentTradeService.getRentTrades(requestId)
}
