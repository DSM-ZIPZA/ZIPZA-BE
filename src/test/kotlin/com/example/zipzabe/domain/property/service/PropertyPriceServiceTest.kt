package com.example.zipzabe.domain.property.service

import com.example.zipzabe.domain.trade.service.RentTradeService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito

class PropertyPriceServiceTest {

    private val rentTradeService = Mockito.mock(RentTradeService::class.java)
    private val cacheService = Mockito.mock(JeonsePriceCacheService::class.java)
    private val service = PropertyPriceService(rentTradeService, cacheService)

    @Test
    fun `uses redis cached jeonse prices without calling molit`() {
        Mockito.`when`(
            cacheService.get("서울 강남구 역삼동 123", "집자아파트", true, 12)
        ).thenReturn(listOf(20_000L, 30_000L))

        val response = service.getAverageSalePrice(
            query = "서울 강남구 역삼동 123",
            latitude = 37.5,
            longitude = 127.0,
            radiusMeters = 250.0,
            buildingName = "집자아파트",
            isApartment = true,
            months = 12,
        )

        assertEquals(25_000L, response.averageSalePriceManwon)
        assertEquals(2, response.sampleCount)
        Mockito.verifyNoInteractions(rentTradeService)
    }

    @Test
    fun `stores molit jeonse prices after redis cache miss`() {
        Mockito.`when`(
            cacheService.get("서울 강남구 역삼동 123", "집자아파트", true, 12)
        ).thenReturn(null)
        Mockito.`when`(
            rentTradeService.fetchRecentJeonseDepositsByAddress(
                query = "서울 강남구 역삼동 123",
                buildingName = "집자아파트",
                isApartment = true,
                months = 12,
            )
        ).thenReturn(listOf(20_000L, 30_000L))

        val response = service.getAverageSalePrice(
            query = "서울 강남구 역삼동 123",
            latitude = 37.5,
            longitude = 127.0,
            radiusMeters = 250.0,
            buildingName = "집자아파트",
            isApartment = true,
            months = 12,
        )

        assertEquals(25_000L, response.averageSalePriceManwon)
        Mockito.verify(cacheService).put(
            query = "서울 강남구 역삼동 123",
            buildingName = "집자아파트",
            isApartment = true,
            months = 12,
            prices = listOf(20_000L, 30_000L),
        )
    }
}
