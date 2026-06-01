package com.example.zipzabe.global.auth.oauth2

import com.example.zipzabe.global.auth.jwt.JwtProvider
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler
import org.springframework.stereotype.Component
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.util.UUID

@Component
class OAuth2SuccessHandler(
    private val jwtProvider: JwtProvider,
    @Value("\${app.frontend-redirect-url}") private val frontendRedirectUrl: String,
    @Value("\${jwt.cookie-name:app_session_id}") private val cookieName: String,
    @Value("\${jwt.accessExp}") private val accessExp: Long,
) : SimpleUrlAuthenticationSuccessHandler() {

    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication,
    ) {
        val oAuth2User = authentication.principal as OAuth2User
        val userId = oAuth2User.attributes["userId"] as UUID
        val token = jwtProvider.generateToken(userId)
        val sessionCookie = ResponseCookie.from(cookieName, token)
            .httpOnly(true)
            .secure(request.isSecure)
            .path("/")
            .maxAge(Duration.ofMillis(accessExp))
            .sameSite("Lax")
            .build()
        response.addHeader(HttpHeaders.SET_COOKIE, sessionCookie.toString())

        val separator = if (frontendRedirectUrl.contains("?")) "&" else "?"
        val encodedToken = URLEncoder.encode(token, StandardCharsets.UTF_8)
        redirectStrategy.sendRedirect(request, response, "$frontendRedirectUrl${separator}token=$encodedToken")
    }
}
