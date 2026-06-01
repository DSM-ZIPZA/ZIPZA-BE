package com.example.zipzabe.global.auth.jwt

import com.example.zipzabe.global.error.exception.BlacklistedTokenException
import com.example.zipzabe.global.error.exception.ZipzaException
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtProvider: JwtProvider,
    private val tokenBlacklistService: TokenBlacklistService,
    @Value("\${jwt.header}") private val header: String,
    @Value("\${jwt.prefix}") private val prefix: String,
    @Value("\${jwt.cookie-name:app_session_id}") private val cookieName: String,
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val authenticated = resolveTokens(request).any { token ->
            try {
                if (tokenBlacklistService.isBlacklisted(token)) throw BlacklistedTokenException()
                val userId = jwtProvider.getUserId(token)
                val auth = UsernamePasswordAuthenticationToken(
                    userId,
                    null,
                    listOf(SimpleGrantedAuthority("ROLE_USER")),
                )
                SecurityContextHolder.getContext().authentication = auth
                true
            } catch (e: ZipzaException) {
                SecurityContextHolder.clearContext()
                false
            }
        }

        if (!authenticated) {
            SecurityContextHolder.clearContext()
        }

        filterChain.doFilter(request, response)
    }

    private fun resolveTokens(request: HttpServletRequest): List<String> {
        val tokens = mutableListOf<String>()
        val bearer = request.getHeader(header)
        if (bearer != null && bearer.startsWith("$prefix ")) {
            tokens.add(bearer.removePrefix("$prefix "))
        }

        request.cookies
            ?.firstOrNull { it.name == cookieName }
            ?.value
            ?.takeIf { it.isNotBlank() }
            ?.let { tokens.add(it) }

        return tokens.distinct()
    }
}
