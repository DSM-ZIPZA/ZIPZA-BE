package com.example.zipzabe.global.auth.jwt

import com.example.zipzabe.global.error.exception.InvalidTokenException
import com.example.zipzabe.global.error.exception.TokenExpiredException
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.Date
import java.util.UUID

@Component
class JwtProvider(
    @Value("\${jwt.secretKey}") secretKey: String,
    @Value("\${jwt.accessExp}") private val accessExp: Long,
) {
    private val signingKey = Keys.hmacShaKeyFor(secretKey.toByteArray())

    fun generateToken(userId: UUID): String {
        val now = Date()
        return Jwts.builder()
            .subject(userId.toString())
            .issuedAt(now)
            .expiration(Date(now.time + accessExp))
            .signWith(signingKey)
            .compact()
    }

    fun getUserId(token: String): UUID {
        return try {
            UUID.fromString(getClaims(token).subject)
        } catch (e: ExpiredJwtException) {
            throw TokenExpiredException()
        } catch (e: JwtException) {
            throw InvalidTokenException()
        }
    }

    fun getExpiration(token: String): Long {
        return try {
            val remaining = getClaims(token).expiration.time - System.currentTimeMillis()
            if (remaining < 0) 0L else remaining
        } catch (e: ExpiredJwtException) {
            0L
        } catch (e: JwtException) {
            throw InvalidTokenException()
        }
    }

    private fun getClaims(token: String) =
        Jwts.parser()
            .verifyWith(signingKey)
            .build()
            .parseSignedClaims(token)
            .payload
}
