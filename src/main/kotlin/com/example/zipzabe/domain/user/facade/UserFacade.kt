package com.example.zipzabe.domain.user.facade

import com.example.zipzabe.domain.user.entity.User
import com.example.zipzabe.domain.user.exception.UserNotFoundException
import com.example.zipzabe.domain.user.repository.UserRepository
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

@Component
class UserFacade(
    private val userRepository: UserRepository
) {
    fun getCurrentUser(): User {
        val email = SecurityContextHolder.getContext()
            .authentication
            ?.name
            ?: throw UserNotFoundException

        return userRepository.findByEmail(email)
            ?: throw UserNotFoundException
    }
}