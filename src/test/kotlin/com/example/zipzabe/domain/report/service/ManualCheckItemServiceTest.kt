package com.example.zipzabe.domain.report.service

import com.example.zipzabe.domain.analysis.entity.AnalysisRequest
import com.example.zipzabe.domain.analysis.entity.AnalysisStatus
import com.example.zipzabe.domain.analysis.entity.ContractType.JEONSE
import com.example.zipzabe.domain.analysis.repository.AnalysisRequestRepository
import com.example.zipzabe.domain.property.entity.Property
import com.example.zipzabe.domain.report.dto.RiskSeverity
import com.example.zipzabe.domain.report.entity.CheckType
import com.example.zipzabe.domain.report.entity.ManualCheckItem
import com.example.zipzabe.domain.report.repository.ManualCheckItemRepository
import com.example.zipzabe.domain.user.entity.User
import com.example.zipzabe.global.error.exception.ManualCheckItemNotFoundException
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalDate
import java.util.Optional
import java.util.UUID

class ManualCheckItemServiceTest {

    private val analysisRequestRepository = Mockito.mock(AnalysisRequestRepository::class.java)
    private val manualCheckItemRepository = Mockito.mock(ManualCheckItemRepository::class.java)
    private val objectMapper = ObjectMapper()

    private val service = ManualCheckItemService(
        analysisRequestRepository = analysisRequestRepository,
        manualCheckItemRepository = manualCheckItemRepository,
        objectMapper = objectMapper,
    )

    @Test
    fun `generate creates three advisory items with correct check types`() {
        val request = createAnalysisRequest()
        val requestId = requireNotNull(request.id)

        Mockito.`when`(analysisRequestRepository.findById(requestId)).thenReturn(Optional.of(request))
        Mockito.doNothing().`when`(manualCheckItemRepository).deleteAllByRequest(request)
        Mockito.`when`(manualCheckItemRepository.save(Mockito.any())).thenAnswer { item ->
            val m = item.arguments[0] as ManualCheckItem
            ReflectionTestUtils.setField(m, "id", UUID.randomUUID())
            m
        }

        val response = service.generate(requestId)

        assertEquals(requestId, response.requestId)
        assertEquals(3, response.items.size)

        val checkTypes = response.items.map { it.checkType }.toSet()
        assertTrue(checkTypes.contains(CheckType.RESIDENT_REGISTRATION_CONFIRM))
        assertTrue(checkTypes.contains(CheckType.UNPAID_NATIONAL_TAX_INQUIRY))
        assertTrue(checkTypes.contains(CheckType.WAGE_CLAIM_PRIORITY))
    }

    @Test
    fun `generate sets expertConsult true only for wage claim item`() {
        val request = createAnalysisRequest()
        val requestId = requireNotNull(request.id)

        Mockito.`when`(analysisRequestRepository.findById(requestId)).thenReturn(Optional.of(request))
        Mockito.doNothing().`when`(manualCheckItemRepository).deleteAllByRequest(request)
        Mockito.`when`(manualCheckItemRepository.save(Mockito.any())).thenAnswer { item ->
            val m = item.arguments[0] as ManualCheckItem
            ReflectionTestUtils.setField(m, "id", UUID.randomUUID())
            m
        }

        val response = service.generate(requestId)

        val wageClaim = response.items.first { it.checkType == CheckType.WAGE_CLAIM_PRIORITY }
        assertTrue(wageClaim.expertConsult)

        val nonExpert = response.items.filter { it.checkType != CheckType.WAGE_CLAIM_PRIORITY }
        nonExpert.forEach { assertFalse(it.expertConsult) }
    }

    @Test
    fun `generate sets HIGH severity for resident registration and unpaid tax items`() {
        val request = createAnalysisRequest()
        val requestId = requireNotNull(request.id)

        Mockito.`when`(analysisRequestRepository.findById(requestId)).thenReturn(Optional.of(request))
        Mockito.doNothing().`when`(manualCheckItemRepository).deleteAllByRequest(request)
        Mockito.`when`(manualCheckItemRepository.save(Mockito.any())).thenAnswer { item ->
            val m = item.arguments[0] as ManualCheckItem
            ReflectionTestUtils.setField(m, "id", UUID.randomUUID())
            m
        }

        val response = service.generate(requestId)

        val resident = response.items.first { it.checkType == CheckType.RESIDENT_REGISTRATION_CONFIRM }
        val unpaidTax = response.items.first { it.checkType == CheckType.UNPAID_NATIONAL_TAX_INQUIRY }
        val wageClaim = response.items.first { it.checkType == CheckType.WAGE_CLAIM_PRIORITY }

        assertEquals(RiskSeverity.HIGH, resident.severity)
        assertEquals(RiskSeverity.HIGH, unpaidTax.severity)
        assertEquals(RiskSeverity.MEDIUM, wageClaim.severity)
    }

    @Test
    fun `generate deserializes procedure steps as non-empty list`() {
        val request = createAnalysisRequest()
        val requestId = requireNotNull(request.id)

        Mockito.`when`(analysisRequestRepository.findById(requestId)).thenReturn(Optional.of(request))
        Mockito.doNothing().`when`(manualCheckItemRepository).deleteAllByRequest(request)
        Mockito.`when`(manualCheckItemRepository.save(Mockito.any())).thenAnswer { item ->
            val m = item.arguments[0] as ManualCheckItem
            ReflectionTestUtils.setField(m, "id", UUID.randomUUID())
            m
        }

        val response = service.generate(requestId)

        response.items.forEach { item ->
            assertTrue(item.procedureSteps.isNotEmpty(), "procedureSteps must not be empty for ${item.checkType}")
        }
    }

    @Test
    fun `generate calls deleteAllByRequest for idempotency`() {
        val request = createAnalysisRequest()
        val requestId = requireNotNull(request.id)

        Mockito.`when`(analysisRequestRepository.findById(requestId)).thenReturn(Optional.of(request))
        Mockito.doNothing().`when`(manualCheckItemRepository).deleteAllByRequest(request)
        Mockito.`when`(manualCheckItemRepository.save(Mockito.any())).thenAnswer { item ->
            val m = item.arguments[0] as ManualCheckItem
            ReflectionTestUtils.setField(m, "id", UUID.randomUUID())
            m
        }

        service.generate(requestId)

        Mockito.verify(manualCheckItemRepository, Mockito.times(1)).deleteAllByRequest(request)
    }

    @Test
    fun `markCompleted throws when item does not belong to the request`() {
        val request = createAnalysisRequest()
        val requestId = requireNotNull(request.id)
        val otherRequest = createAnalysisRequest()

        val item = ManualCheckItem(
            request = otherRequest,
            checkType = CheckType.RESIDENT_REGISTRATION_CONFIRM,
            title = "전입세대확인서 확인",
            severity = RiskSeverity.HIGH,
            guideText = "guide",
            procedureSteps = "[]",
        ).also { ReflectionTestUtils.setField(it, "id", UUID.randomUUID()) }

        val itemId = requireNotNull(item.id)

        Mockito.`when`(analysisRequestRepository.findById(requestId)).thenReturn(Optional.of(request))
        Mockito.`when`(manualCheckItemRepository.findById(itemId)).thenReturn(Optional.of(item))

        assertThrows<ManualCheckItemNotFoundException> {
            service.markCompleted(requestId, itemId)
        }
    }

    @Test
    fun `markCompleted sets isCompleted and checkedAt`() {
        val request = createAnalysisRequest()
        val requestId = requireNotNull(request.id)

        val item = ManualCheckItem(
            request = request,
            checkType = CheckType.UNPAID_NATIONAL_TAX_INQUIRY,
            title = "임대인 미납국세 열람",
            severity = RiskSeverity.HIGH,
            guideText = "guide",
            procedureSteps = objectMapper.writeValueAsString(listOf("step1")),
            officialUrl = "https://www.hometax.go.kr",
        ).also { ReflectionTestUtils.setField(it, "id", UUID.randomUUID()) }

        val itemId = requireNotNull(item.id)

        Mockito.`when`(analysisRequestRepository.findById(requestId)).thenReturn(Optional.of(request))
        Mockito.`when`(manualCheckItemRepository.findById(itemId)).thenReturn(Optional.of(item))
        Mockito.`when`(manualCheckItemRepository.save(Mockito.any())).thenAnswer { it.arguments[0] }

        val response = service.markCompleted(requestId, itemId)

        assertTrue(response.isCompleted)
        assertNotNull(response.checkedAt)
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
            floor = 5,
            exclusiveArea = 84.9,
            contractDate = LocalDate.of(2024, 4, 20),
            balanceDate = LocalDate.of(2024, 5, 20),
            expiryDate = LocalDate.of(2026, 5, 20),
            status = AnalysisStatus.PENDING,
        ).also { ReflectionTestUtils.setField(it, "id", UUID.randomUUID()) }
    }
}
