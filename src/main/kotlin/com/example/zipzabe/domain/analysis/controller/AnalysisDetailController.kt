package com.example.zipzabe.domain.analysis.controller

import com.example.zipzabe.domain.analysis.dto.AnalysisDetailResponse
import com.example.zipzabe.domain.analysis.service.AnalysisDetailService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@Tag(name = "분석 상세", description = "프론트 상세 패널용 통합 조회 API")
@RestController
@RequestMapping("/api/analysis-requests/{requestId}/property-detail")
class AnalysisDetailController(
    private val analysisDetailService: AnalysisDetailService,
) {
    @Operation(summary = "분석 상세 통합 조회", description = "매물, 실거래 차트, 등기부, 건축물대장, 진단 리포트 데이터를 한 번에 조회합니다.")
    @GetMapping
    fun getDetail(@PathVariable requestId: UUID): AnalysisDetailResponse =
        analysisDetailService.getDetail(requestId)
}
