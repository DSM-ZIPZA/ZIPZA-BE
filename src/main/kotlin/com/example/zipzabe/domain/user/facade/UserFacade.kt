package com.example.zipzabe.domain.user.facade

import com.example.zipzabe.domain.user.entity.User
import com.example.zipzabe.domain.user.exception.UserNotFoundException
import com.example.zipzabe.domain.user.repository.UserRepository
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class UserFacade(
    private val userRepository: UserRepository
) {
    fun getCurrentUser(): User {
        val principal = SecurityContextHolder.getContext()
            .authentication
            ?.principal
            ?: throw UserNotFoundException

        val userId = try {
            UUID.fromString(principal.toString())
        } catch (e: IllegalArgumentException) {
            throw UserNotFoundException
        }

        return userRepository.findById(userId).orElseThrow { UserNotFoundException }
    }
}