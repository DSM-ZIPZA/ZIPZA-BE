package com.example.zipzabe.domain.reminder.controller

import com.example.zipzabe.domain.reminder.dto.ReminderResponse
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
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@Tag(name = "리마인더", description = "잔금일 재분석 리마인더 조회 및 확인 처리 API")
@RestController
@RequestMapping("/api/reminders")
class ReminderController(
    private val reminderService: ReminderService,
    private val userFacade: UserFacade,
) {

    @Operation(summary = "전체 리마인더 조회", description = "현재 로그인한 사용자의 전체 리마인더를 잔금일 오름차순으로 조회합니다.")
    @GetMapping
    fun getAll(): List<ReminderResponse> =
        reminderService.getAll(userFacade.getCurrentUser())

    @Operation(summary = "미확인 리마인더 조회", description = "오늘 이전 날짜 중 아직 확인하지 않은 리마인더를 조회합니다.")
    @GetMapping("/pending")
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
    @PatchMapping("/{reminderId}/acknowledge")
    fun acknowledge(
        @Parameter(description = "리마인더 ID", required = true) @PathVariable reminderId: UUID,
    ): ResponseEntity<Void> {
        reminderService.acknowledge(reminderId, userFacade.getCurrentUser())
        return ResponseEntity.noContent().build()
    }
}
