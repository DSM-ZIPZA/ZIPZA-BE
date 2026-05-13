package com.example.zipzabe.domain.registry.controller

import com.example.zipzabe.domain.registry.dto.RegistryOcrResponse
import com.example.zipzabe.domain.registry.service.RegistryOcrImportService
import com.example.zipzabe.global.error.exception.ErrorResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@Tag(name = "등기부등본 OCR", description = "등기부등본 PDF를 OCR해 권리관계 원천 데이터를 저장하는 API")
@RestController
@RequestMapping("/api/analysis-requests/{requestId}/registry-ocr")
class RegistryOcrController(
    private val registryOcrImportService: RegistryOcrImportService,
) {

    @Operation(
        summary = "등기부등본 PDF OCR",
        description = "업로드된 등기부등본 PDF를 Google Cloud Vision으로 OCR하고 표제부/갑구/을구 정보를 저장합니다.",
    )
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "OCR 저장 성공",
            content = [Content(schema = Schema(implementation = RegistryOcrResponse::class))]),
        ApiResponse(responseCode = "400", description = "잘못된 PDF 요청",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
        ApiResponse(responseCode = "404", description = "분석 요청을 찾을 수 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
        ApiResponse(responseCode = "502", description = "Google Cloud Vision 호출 실패",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]),
    )
    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun importRegistryPdf(
        @Parameter(description = "분석 요청 ID", required = true) @PathVariable requestId: UUID,
        @Parameter(description = "등기부등본 PDF 파일", required = true) @RequestPart("file") file: MultipartFile,
    ): ResponseEntity<RegistryOcrResponse> {
        val response = registryOcrImportService.importRegistryPdf(requestId, file)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }
}
