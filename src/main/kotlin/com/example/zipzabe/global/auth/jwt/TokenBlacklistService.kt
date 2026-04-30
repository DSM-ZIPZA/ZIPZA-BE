package com.example.zipzabe.global.auth.jwt

import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class TokenBlacklistService(private val redisTemplate: StringRedisTemplate) {

    fun addToBlacklist(token: String, remainingMillis: Long) {
        redisTemplate.opsForValue().set(
            "blacklist:$token",
            "true",
            Duration.ofMillis(remainingMillis),
        )
    }

    fun isBlacklisted(token: String): Boolean =
        redisTemplate.hasKey("blacklist:$token") == true
}
