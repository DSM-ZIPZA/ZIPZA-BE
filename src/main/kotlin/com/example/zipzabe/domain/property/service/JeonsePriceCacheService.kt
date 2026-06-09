package com.example.zipzabe.domain.property.service

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Duration

@Service
class JeonsePriceCacheService(
    private val redisTemplate: StringRedisTemplate,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(JeonsePriceCacheService::class.java)

    fun get(
        query: String,
        buildingName: String?,
        isApartment: Boolean?,
        months: Int,
    ): List<Long>? {
        val key = buildKey(query, buildingName, isApartment, months)
        return runCatching {
            val cached = redisTemplate.opsForValue().get(key) ?: return null
            objectMapper.readValue(cached, object : TypeReference<List<Long>>() {})
        }.onFailure {
            log.warn("Failed to read jeonse price cache. key={} reason={}", key, it.message)
        }.getOrNull()
    }

    fun put(
        query: String,
        buildingName: String?,
        isApartment: Boolean?,
        months: Int,
        prices: List<Long>,
    ) {
        if (prices.isEmpty()) return
        val key = buildKey(query, buildingName, isApartment, months)
        runCatching {
            redisTemplate.opsForValue().set(
                key,
                objectMapper.writeValueAsString(prices),
                CACHE_TTL,
            )
        }.onFailure {
            log.warn("Failed to write jeonse price cache. key={} reason={}", key, it.message)
        }
    }

    private fun buildKey(
        query: String,
        buildingName: String?,
        isApartment: Boolean?,
        months: Int,
    ): String {
        val source = listOf(
            query.trim(),
            buildingName?.trim().orEmpty(),
            isApartment?.toString().orEmpty(),
            months.toString(),
        ).joinToString("|")
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(source.toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
        return "$CACHE_NAMESPACE:$digest"
    }

    companion object {
        private const val CACHE_NAMESPACE = "zipza:property-price:jeonse:v1"
        private val CACHE_TTL: Duration = Duration.ofDays(7)
    }
}
