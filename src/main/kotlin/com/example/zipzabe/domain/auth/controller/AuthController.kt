package com.example.zipzabe.domain.auth.controller

import com.example.zipzabe.domain.auth.service.LogoutService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseCookie
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping
class AuthController(
    private val logoutService: LogoutService,
    @Value("\${jwt.cookie-name:app_session_id}") private val cookieName: String,
) {
    @DeleteMapping("/auth/logout", "/api/auth/logout")
    fun logout(request: HttpServletRequest): ResponseEntity<Void> {
        logoutService.execute(request)
        val expiredCookie = ResponseCookie.from(cookieName, "")
            .httpOnly(true)
            .secure(request.isSecure)
            .path("/")
            .maxAge(0)
            .sameSite("Lax")
            .build()

        return ResponseEntity.status(HttpStatus.NO_CONTENT)
            .header(HttpHeaders.SET_COOKIE, expiredCookie.toString())
            .build()
    }
}
