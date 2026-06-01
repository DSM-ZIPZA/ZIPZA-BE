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
    @Value("\${jwt.cookie-name:app_session_id}") private val cookieName: String,
) {
    fun execute(request: HttpServletRequest) {
        val token = resolveToken(request) ?: return
        val remaining = jwtProvider.getExpiration(token)
        if (remaining > 0) tokenBlacklistService.addToBlacklist(token, remaining)
    }

    private fun resolveToken(request: HttpServletRequest): String? {
        val bearer = request.getHeader(header)
        if (bearer != null && bearer.startsWith("$prefix ")) {
            return bearer.removePrefix("$prefix ")
        }

        return request.cookies
            ?.firstOrNull { it.name == cookieName }
            ?.value
            ?.takeIf { it.isNotBlank() }
    }
}
