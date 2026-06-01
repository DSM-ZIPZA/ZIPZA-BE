package com.example.zipzabe.domain.reminder.service

import com.example.zipzabe.domain.analysis.entity.AnalysisRequest
import com.example.zipzabe.domain.analysis.repository.AnalysisRequestRepository
import com.example.zipzabe.domain.reminder.dto.ReminderCreateRequest
import com.example.zipzabe.domain.reminder.dto.ReminderResponse
import com.example.zipzabe.domain.reminder.dto.ReminderUpdateRequest
import com.example.zipzabe.domain.reminder.entity.Reminder
import com.example.zipzabe.domain.reminder.entity.ReminderChannel
import com.example.zipzabe.domain.reminder.entity.ReminderType
import com.example.zipzabe.domain.reminder.repository.ReminderRepository
import com.example.zipzabe.domain.user.entity.User
import com.example.zipzabe.domain.user.facade.UserFacade
import com.example.zipzabe.global.error.exception.AnalysisRequestNotFoundException
import com.example.zipzabe.global.error.exception.ReminderNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Service
class ReminderService(
    private val userFacade: UserFacade,
    private val analysisRequestRepository: AnalysisRequestRepository,
    private val reminderRepository: ReminderRepository,
) {
    @Transactional
    fun createBalanceReminders(request: AnalysisRequest, user: User) {
        reminderRepository.saveAll(
            listOf(
                Reminder(
                    request = request,
                    user = user,
                    reminderType = ReminderType.BEFORE_BALANCE,
                    remindDate = request.balanceDate.minusDays(1),
                    channel = ReminderChannel.PUSH,
                ),
                Reminder(
                    request = request,
                    user = user,
                    reminderType = ReminderType.BEFORE_BALANCE,
                    remindDate = request.balanceDate,
                    channel = ReminderChannel.PUSH,
                ),
            ),
        )
    }

    @Transactional
    fun create(requestId: UUID, request: ReminderCreateRequest): ReminderResponse {
        val user = userFacade.getCurrentUser()
        val analysisRequest = analysisRequestRepository.findByIdAndUser(requestId, user)
            ?: throw AnalysisRequestNotFoundException()
        val reminder = reminderRepository.save(
            Reminder(
                request = analysisRequest,
                user = user,
                reminderType = request.reminderType,
                remindDate = request.remindDate,
                channel = request.channel,
            ),
        )
        return ReminderResponse.from(reminder)
    }

    @Transactional(readOnly = true)
    fun getAll(): List<ReminderResponse> =
        getAll(userFacade.getCurrentUser())

    @Transactional(readOnly = true)
    fun getAll(user: User): List<ReminderResponse> =
        reminderRepository.findByUserOrderByRemindDateAsc(user).map(ReminderResponse::from)

    @Transactional(readOnly = true)
    fun getPending(): List<ReminderResponse> =
        getPending(userFacade.getCurrentUser())

    @Transactional(readOnly = true)
    fun getPending(user: User): List<ReminderResponse> =
        reminderRepository.findByUserAndIsSentFalseAndRemindDateLessThanEqualOrderByRemindDateAsc(
            user,
            LocalDate.now(),
        ).map(ReminderResponse::from)

    @Transactional
    fun acknowledge(reminderId: UUID, user: User) {
        val reminder = reminderRepository.findByIdAndUser(reminderId, user)
            ?: throw ReminderNotFoundException()
        reminder.isSent = true
        reminder.sentAt = LocalDateTime.now()
    }

    @Transactional
    fun update(reminderId: UUID, request: ReminderUpdateRequest): ReminderResponse {
        val user = userFacade.getCurrentUser()
        val reminder = reminderRepository.findByIdAndUser(reminderId, user)
            ?: throw ReminderNotFoundException()
        request.reminderType?.let { reminder.reminderType = it }
        request.remindDate?.let { reminder.remindDate = it }
        request.channel?.let { reminder.channel = it }
        return ReminderResponse.from(reminder)
    }

    @Transactional
    fun delete(reminderId: UUID) {
        val user = userFacade.getCurrentUser()
        val reminder = reminderRepository.findByIdAndUser(reminderId, user)
            ?: throw ReminderNotFoundException()
        reminderRepository.delete(reminder)
    }
}
