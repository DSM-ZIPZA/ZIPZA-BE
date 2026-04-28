package com.example.zipzabe.domain.user.repository

import com.example.zipzabe.domain.user.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface UserRepository : JpaRepository<User, UUID> {
    fun findByProviderAndProviderId(provider: String, providerId: String): User?
    fun findByEmail(email: String): User?
}
