package com.example.zipzabe.domain.reminder.dto

import com.example.zipzabe.domain.reminder.entity.Reminder
import com.example.zipzabe.domain.reminder.entity.ReminderType
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class ReminderResponse(
    val id: UUID,
    val requestId: UUID,
    val reminderType: ReminderType,
    val remindDate: LocalDate,
    val isSent: Boolean,
    val sentAt: LocalDateTime?,
) {
    companion object {
        fun from(reminder: Reminder) = ReminderResponse(
            id = requireNotNull(reminder.id),
            requestId = requireNotNull(reminder.request.id),
            reminderType = reminder.reminderType,
            remindDate = reminder.remindDate,
            isSent = reminder.isSent,
            sentAt = reminder.sentAt,
        )
    }
}
