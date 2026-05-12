package com.example.zipzabe.domain.analysis.service

import com.example.zipzabe.domain.analysis.dto.PriceAnalysisStatus
import com.example.zipzabe.domain.analysis.entity.AnalysisRequest
import com.example.zipzabe.domain.analysis.entity.AnalysisStatus
import com.example.zipzabe.domain.analysis.entity.ContractType.JEONSE
import com.example.zipzabe.domain.analysis.repository.AnalysisRequestRepository
import com.example.zipzabe.domain.analysis.repository.PriceAnalysisRepository
import com.example.zipzabe.domain.property.entity.Property
import com.example.zipzabe.domain.trade.entity.BuildingType
import com.example.zipzabe.domain.trade.entity.ContractType
import com.example.zipzabe.domain.trade.entity.TradeRecord
import com.example.zipzabe.domain.trade.repository.TradeRecordRepository
import com.example.zipzabe.domain.user.entity.User
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalDate
import java.util.Optional
import java.util.UUID

class PriceAnalysisServiceTest {

    private val analysisRequestRepository = Mockito.mock(AnalysisRequestRepository::class.java)
    private val tradeRecordRepository = Mockito.mock(TradeRecordRepository::class.java)
    private val priceAnalysisRepository = Mockito.mock(PriceAnalysisRepository::class.java)

    private val service = PriceAnalysisService(
        analysisRequestRepository = analysisRequestRepository,
        tradeRecordRepository = tradeRecordRepository,
        priceAnalysisRepository = priceAnalysisRepository,
    )

    @Test
    fun `analyze marks danger when deposit is more than 120 percent of similar median jeonse`() {
        val request = createAnalysisRequest(depositAmount = 12500L)
        val requestId = requireNotNull(request.id)
        val records = listOf(
            createTradeRecord(request.property, 9000L),
            createTradeRecord(request.property, 10000L),
            createTradeRecord(request.property, 11000L),
        )

        Mockito.`when`(analysisRequestRepository.findById(requestId)).thenReturn(Optional.of(request))
        Mockito.`when`(tradeRecordRepository.findByPropertyOrderByContractDateDesc(request.property)).thenReturn(records)
        Mockito.`when`(priceAnalysisRepository.save(Mockito.any())).thenAnswer { it.arguments[0] }

        val response = service.analyze(requestId, months = 24)

        assertEquals(PriceAnalysisStatus.DANGER, response.status)
        assertEquals(10000L, response.referenceMedian)
        assertEquals(70, response.riskScore)
        assertTrue(response.isOverpriced)
    }

    @Test
    fun `analyze returns insufficient data when similar jeonse samples are fewer than three`() {
        val request = createAnalysisRequest(depositAmount = 10000L)
        val requestId = requireNotNull(request.id)

        Mockito.`when`(analysisRequestRepository.findById(requestId)).thenReturn(Optional.of(request))
        Mockito.`when`(tradeRecordRepository.findByPropertyOrderByContractDateDesc(request.property))
            .thenReturn(listOf(createTradeRecord(request.property, 9500L)))
        Mockito.`when`(priceAnalysisRepository.save(Mockito.any())).thenAnswer { it.arguments[0] }

        val response = service.analyze(requestId, months = 24)

        assertEquals(PriceAnalysisStatus.INSUFFICIENT_DATA, response.status)
        assertEquals(1, response.sampleCount)
        assertEquals(10, response.riskScore)
    }

    private fun createAnalysisRequest(depositAmount: Long): AnalysisRequest {
        val user = User(
            email = "tester@example.com",
            nickname = "tester",
            provider = "kakao",
            providerId = "provider-id",
        )
        val property = Property(
            roadAddress = "서울특별시 강남구 테헤란로 123",
            jibunAddress = "서울특별시 강남구 역삼동 123-45",
            detailAddress = "101동 1201호",
            buildingManagementNumber = "1234567890123456789012345",
            postalCode = "06234",
            administrativeCode = "1168010100",
            city = "서울특별시",
            district = "강남구",
            neighborhood = "역삼동",
            buildingName = "집자아파트",
            isApartment = true,
            longitude = 127.032,
            latitude = 37.499,
        )

        return AnalysisRequest(
            user = user,
            property = property,
            contractType = JEONSE,
            depositAmount = depositAmount,
            floor = 12,
            exclusiveArea = 84.9,
            contractDate = LocalDate.of(2024, 4, 20),
            balanceDate = LocalDate.of(2024, 5, 20),
            expiryDate = LocalDate.of(2026, 5, 20),
            status = AnalysisStatus.PENDING,
        ).also { ReflectionTestUtils.setField(it, "id", UUID.randomUUID()) }
    }

    private fun createTradeRecord(property: Property, depositAmount: Long): TradeRecord =
        TradeRecord(
            property = property,
            buildingType = BuildingType.APARTMENT,
            contractType = ContractType.JEONSE,
            depositAmount = depositAmount,
            exclusiveArea = 84.9,
            floor = 12,
            contractDate = LocalDate.of(2024, 3, 15),
        )
}
