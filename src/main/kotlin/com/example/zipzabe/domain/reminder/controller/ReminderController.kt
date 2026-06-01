package com.example.zipzabe.domain.reminder.controller

import com.example.zipzabe.domain.reminder.dto.ReminderCreateRequest
import com.example.zipzabe.domain.reminder.dto.ReminderResponse
import com.example.zipzabe.domain.reminder.dto.ReminderUpdateRequest
import com.example.zipzabe.domain.reminder.service.ReminderService
import com.example.zipzabe.domain.user.facade.UserFacade
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
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@Tag(name = "리마인더", description = "분석 요청 후속 리마인더 API")
@RestController
class ReminderController(
    private val reminderService: ReminderService,
    private val userFacade: UserFacade,
) {
    @Operation(summary = "리마인더 등록", description = "분석 요청에 대한 잔금/만기 리마인더를 등록합니다.")
    @PostMapping("/api/analysis-requests/{requestId}/reminders")
    fun create(
        @PathVariable requestId: UUID,
        @RequestBody request: ReminderCreateRequest,
    ): ResponseEntity<ReminderResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(reminderService.create(requestId, request))

    @Operation(summary = "전체 리마인더 조회", description = "현재 로그인한 사용자의 전체 리마인더를 잔금일 오름차순으로 조회합니다.")
    @GetMapping("/api/reminders")
    fun getAll(): List<ReminderResponse> =
        reminderService.getAll(userFacade.getCurrentUser())

    @Operation(summary = "미확인 리마인더 조회", description = "오늘 이전 날짜 중 아직 확인하지 않은 리마인더를 조회합니다.")
    @GetMapping("/api/reminders/pending")
    fun getPending(): List<ReminderResponse> =
        reminderService.getPending(userFacade.getCurrentUser())

    @Operation(summary = "리마인더 확인 처리", description = "리마인더를 확인 처리합니다.")
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "확인 처리 성공"),
        ApiResponse(
            responseCode = "404",
            description = "리마인더를 찾을 수 없음",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))],
        ),
    )
    @PatchMapping("/api/reminders/{reminderId}/acknowledge")
    fun acknowledge(
        @Parameter(description = "리마인더 ID", required = true) @PathVariable reminderId: UUID,
    ): ResponseEntity<Void> {
        reminderService.acknowledge(reminderId, userFacade.getCurrentUser())
        return ResponseEntity.noContent().build()
    }

    @Operation(summary = "리마인더 수정", description = "리마인더 유형/일자/채널을 수정합니다.")
    @PatchMapping("/api/reminders/{reminderId}")
    fun update(
        @PathVariable reminderId: UUID,
        @RequestBody request: ReminderUpdateRequest,
    ): ReminderResponse = reminderService.update(reminderId, request)

    @Operation(summary = "리마인더 삭제", description = "등록된 리마인더를 삭제합니다.")
    @DeleteMapping("/api/reminders/{reminderId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable reminderId: UUID) = reminderService.delete(reminderId)
}
