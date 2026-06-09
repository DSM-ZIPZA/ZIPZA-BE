package com.example.zipzabe.domain.property.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.ValueOperations
import java.time.Duration

class JeonsePriceCacheServiceTest {

    private val redisTemplate = Mockito.mock(StringRedisTemplate::class.java)
    @Suppress("UNCHECKED_CAST")
    private val valueOperations = Mockito.mock(ValueOperations::class.java) as ValueOperations<String, String>
    private val service = JeonsePriceCacheService(
        redisTemplate = redisTemplate,
        objectMapper = ObjectMapper().registerKotlinModule(),
    )

    @Test
    fun `stores jeonse prices in redis for seven days`() {
        Mockito.`when`(redisTemplate.opsForValue()).thenReturn(valueOperations)

        service.put(
            query = "서울 강남구 역삼동 123",
            buildingName = "집자아파트",
            isApartment = true,
            months = 12,
            prices = listOf(20_000L, 25_000L),
        )

        Mockito.verify(valueOperations).set(
            any(String::class.java),
            eq("[20000,25000]"),
            eq(Duration.ofDays(7)),
        )
    }

    @Test
    fun `reads cached jeonse prices from redis`() {
        Mockito.`when`(redisTemplate.opsForValue()).thenReturn(valueOperations)
        Mockito.`when`(valueOperations.get(any(String::class.java))).thenReturn("[20000,25000]")

        val prices = service.get(
            query = "서울 강남구 역삼동 123",
            buildingName = "집자아파트",
            isApartment = true,
            months = 12,
        )

        assertEquals(listOf(20_000L, 25_000L), prices)
    }
}
