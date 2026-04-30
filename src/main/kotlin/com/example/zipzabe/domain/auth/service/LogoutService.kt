package com.example.zipzabe.domain.auth.service

import com.example.zipzabe.global.auth.jwt.JwtProvider
import com.example.zipzabe.global.auth.jwt.TokenBlacklistService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class LogoutService(
    private val jwtProvider: JwtProvider,
    private val tokenBlacklistService: TokenBlacklistService,
    @Value("\${jwt.header}") private val header: String,
    @Value("\${jwt.prefix}") private val prefix: String,
) {
    fun execute(request: HttpServletRequest) {
        val bearer = request.getHeader(header) ?: return
        if (!bearer.startsWith("$prefix ")) return
        val token = bearer.removePrefix("$prefix ")
        val remaining = jwtProvider.getExpiration(token)
        if (remaining > 0) tokenBlacklistService.addToBlacklist(token, remaining)
    }
}
