package com.example.zipzabe.domain.reminder.repository

import com.example.zipzabe.domain.reminder.entity.Reminder
import com.example.zipzabe.domain.user.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate
import java.util.UUID

interface ReminderRepository : JpaRepository<Reminder, UUID> {
    fun findByUserOrderByRemindDateAsc(user: User): List<Reminder>
    fun findByUserAndIsSentFalseAndRemindDateLessThanEqualOrderByRemindDateAsc(
        user: User,
        date: LocalDate,
    ): List<Reminder>
    fun findByIdAndUser(id: UUID, user: User): Reminder?
}
