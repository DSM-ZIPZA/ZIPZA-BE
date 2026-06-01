package com.example.zipzabe.domain.reminder.dto

import com.example.zipzabe.domain.reminder.entity.Reminder
import com.example.zipzabe.domain.reminder.entity.ReminderChannel
import com.example.zipzabe.domain.reminder.entity.ReminderType
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class ReminderCreateRequest(
    val reminderType: ReminderType,
    val remindDate: LocalDate,
    val channel: ReminderChannel,
)

data class ReminderUpdateRequest(
    val reminderType: ReminderType? = null,
    val remindDate: LocalDate? = null,
    val channel: ReminderChannel? = null,
)

data class ReminderResponse(
    val id: UUID,
    val reminderId: UUID,
    val requestId: UUID,
    val reminderType: ReminderType,
    val remindDate: LocalDate,
    val channel: ReminderChannel,
    val isSent: Boolean,
    val sentAt: LocalDateTime?,
) {
    companion object {
        fun from(reminder: Reminder): ReminderResponse {
            val id = requireNotNull(reminder.id)
            return ReminderResponse(
                id = id,
                reminderId = id,
                requestId = requireNotNull(reminder.request.id),
                reminderType = reminder.reminderType,
                remindDate = reminder.remindDate,
                channel = reminder.channel,
                isSent = reminder.isSent,
                sentAt = reminder.sentAt,
            )
        }
    }
}
