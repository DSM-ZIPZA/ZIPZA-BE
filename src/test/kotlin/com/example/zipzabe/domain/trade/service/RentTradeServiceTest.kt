package com.example.zipzabe.domain.trade.service

import com.example.zipzabe.domain.analysis.entity.AnalysisRequest
import com.example.zipzabe.domain.analysis.entity.AnalysisStatus
import com.example.zipzabe.domain.analysis.entity.ContractType.JEONSE
import com.example.zipzabe.domain.analysis.repository.AnalysisRequestRepository
import com.example.zipzabe.domain.building.repository.BuildingLedgerRepository
import com.example.zipzabe.domain.property.entity.Property
import com.example.zipzabe.domain.trade.entity.BuildingType
import com.example.zipzabe.domain.trade.entity.ContractType
import com.example.zipzabe.domain.trade.entity.TradeRecord
import com.example.zipzabe.domain.trade.repository.TradeRecordRepository
import com.example.zipzabe.domain.user.entity.User
import com.example.zipzabe.global.feign.client.MolitRentClient
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalDate
import java.util.Optional
import java.util.UUID

class RentTradeServiceTest {

    private val analysisRequestRepository = Mockito.mock(AnalysisRequestRepository::class.java)
    private val buildingLedgerRepository = Mockito.mock(BuildingLedgerRepository::class.java)
    private val tradeRecordRepository = Mockito.mock(TradeRecordRepository::class.java)
    private val molitRentClient = Mockito.mock(MolitRentClient::class.java)

    private val service = RentTradeService(
        analysisRequestRepository = analysisRequestRepository,
        buildingLedgerRepository = buildingLedgerRepository,
        tradeRecordRepository = tradeRecordRepository,
        molitRentClient = molitRentClient,
        serviceKey = "test-service-key",
    )

    @Test
    fun `fetchRentTrades parses molit XML and saves matching apartment rent trades`() {
        val request = createAnalysisRequest()
        val requestId = requireNotNull(request.id)

        Mockito.`when`(analysisRequestRepository.findById(requestId)).thenReturn(Optional.of(request))
        Mockito.`when`(
            molitRentClient.getApartmentRent(
                "test-service-key",
                "11680",
                "202404",
                1,
                1000
            )
        ).thenReturn(APARTMENT_RENT_XML)
        Mockito.`when`(tradeRecordRepository.findByProperty(request.property)).thenReturn(emptyList())
        Mockito.`when`(tradeRecordRepository.saveAll(Mockito.any<Iterable<TradeRecord>>())).thenAnswer {
            it.getArgument<Iterable<TradeRecord>>(0).toList()
        }

        val response = service.fetchRentTrades(
            requestId = requestId,
            months = 1,
            buildingType = BuildingType.APARTMENT,
        )

        assertEquals(requestId, response.requestId)
        assertEquals("11680", response.lawdCd)
        assertEquals("202404", response.dealYmFrom)
        assertEquals("202404", response.dealYmTo)
        assertEquals(BuildingType.APARTMENT, response.buildingType)
        assertEquals(1, response.fetchedCount)
        assertEquals(1, response.savedCount)

        val record = response.records.single()
        assertEquals(ContractType.JEONSE, record.contractType)
        assertEquals(28300L, record.depositAmount)
        assertNull(record.monthlyRent)
        assertEquals(84.9, record.exclusiveArea)
        assertEquals(12, record.floor)
        assertEquals(LocalDate.of(2024, 4, 15), record.contractDate)
    }

    private fun createAnalysisRequest(): AnalysisRequest {
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
            depositAmount = 30000L,
            floor = 12,
            exclusiveArea = 84.9,
            contractDate = LocalDate.of(2024, 4, 20),
            balanceDate = LocalDate.of(2024, 5, 20),
            expiryDate = LocalDate.of(2026, 5, 20),
            status = AnalysisStatus.PENDING,
        ).also { ReflectionTestUtils.setField(it, "id", UUID.randomUUID()) }
    }

    companion object {
        private val APARTMENT_RENT_XML = """
            <response>
                <header>
                    <resultCode>00</resultCode>
                    <resultMsg>NORMAL SERVICE.</resultMsg>
                </header>
                <body>
                    <items>
                        <item>
                            <년>2024</년>
                            <월>4</월>
                            <일>15</일>
                            <법정동>역삼동</법정동>
                            <지번>123-45</지번>
                            <아파트>집자</아파트>
                            <보증금액>28,300</보증금액>
                            <월세금액>0</월세금액>
                            <전용면적>84.9</전용면적>
                            <층>12</층>
                            <계약구분>신규</계약구분>
                            <계약기간>24.05~26.05</계약기간>
                        </item>
                        <item>
                            <년>2024</년>
                            <월>4</월>
                            <일>16</일>
                            <법정동>역삼동</법정동>
                            <지번>999</지번>
                            <아파트>다른아파트</아파트>
                            <보증금액>10,000</보증금액>
                            <월세금액>50</월세금액>
                            <전용면적>59.5</전용면적>
                            <층>5</층>
                        </item>
                    </items>
                    <numOfRows>1000</numOfRows>
                    <pageNo>1</pageNo>
                    <totalCount>2</totalCount>
                </body>
            </response>
        """.trimIndent()
    }
}
