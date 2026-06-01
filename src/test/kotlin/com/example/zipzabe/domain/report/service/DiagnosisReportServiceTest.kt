package com.example.zipzabe.domain.report.service

import com.example.zipzabe.domain.analysis.entity.AnalysisRequest
import com.example.zipzabe.domain.analysis.entity.AnalysisStatus
import com.example.zipzabe.domain.analysis.entity.ContractType
import com.example.zipzabe.domain.analysis.entity.RightsAnalysis
import com.example.zipzabe.domain.analysis.repository.AnalysisRequestRepository
import com.example.zipzabe.domain.analysis.repository.BuildingAnalysisRepository
import com.example.zipzabe.domain.analysis.repository.FraudPatternAnalysisRepository
import com.example.zipzabe.domain.analysis.repository.GuaranteeAnalysisRepository
import com.example.zipzabe.domain.analysis.repository.PriceAnalysisRepository
import com.example.zipzabe.domain.analysis.repository.RecoveryAnalysisRepository
import com.example.zipzabe.domain.analysis.repository.RightsAnalysisRepository
import com.example.zipzabe.domain.analysis.service.GeminiSummaryService
import com.example.zipzabe.domain.property.entity.Property
import com.example.zipzabe.domain.registry.entity.ParseStatus
import com.example.zipzabe.domain.registry.entity.RegistryCandidate
import com.example.zipzabe.domain.registry.entity.RegistryRaw
import com.example.zipzabe.domain.registry.entity.RegistryRestriction
import com.example.zipzabe.domain.registry.repository.RegistryMortgageRepository
import com.example.zipzabe.domain.registry.repository.RegistryRestrictionRepository
import com.example.zipzabe.domain.report.dto.DiagnosisSupplementRequest
import com.example.zipzabe.domain.report.dto.OccupancyInput
import com.example.zipzabe.domain.report.entity.DiagnosisReport
import com.example.zipzabe.domain.report.entity.Verdict
import com.example.zipzabe.domain.report.repository.DiagnosisReportRepository
import com.example.zipzabe.domain.user.entity.User
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalDate
import java.util.Optional
import java.util.UUID

class DiagnosisReportServiceTest {

    private val analysisRequestRepository = Mockito.mock(AnalysisRequestRepository::class.java)
    private val priceAnalysisRepository = Mockito.mock(PriceAnalysisRepository::class.java)
    private val buildingAnalysisRepository = Mockito.mock(BuildingAnalysisRepository::class.java)
    private val rightsAnalysisRepository = Mockito.mock(RightsAnalysisRepository::class.java)
    private val guaranteeAnalysisRepository = Mockito.mock(GuaranteeAnalysisRepository::class.java)
    private val recoveryAnalysisRepository = Mockito.mock(RecoveryAnalysisRepository::class.java)
    private val fraudPatternAnalysisRepository = Mockito.mock(FraudPatternAnalysisRepository::class.java)
    private val registryMortgageRepository = Mockito.mock(RegistryMortgageRepository::class.java)
    private val registryRestrictionRepository = Mockito.mock(RegistryRestrictionRepository::class.java)
    private val diagnosisReportRepository = Mockito.mock(DiagnosisReportRepository::class.java)
    private val geminiSummaryService = Mockito.mock(GeminiSummaryService::class.java)

    private val service = DiagnosisReportService(
        analysisRequestRepository = analysisRequestRepository,
        priceAnalysisRepository = priceAnalysisRepository,
        buildingAnalysisRepository = buildingAnalysisRepository,
        rightsAnalysisRepository = rightsAnalysisRepository,
        guaranteeAnalysisRepository = guaranteeAnalysisRepository,
        recoveryAnalysisRepository = recoveryAnalysisRepository,
        fraudPatternAnalysisRepository = fraudPatternAnalysisRepository,
        registryMortgageRepository = registryMortgageRepository,
        registryRestrictionRepository = registryRestrictionRepository,
        diagnosisReportRepository = diagnosisReportRepository,
        objectMapper = jacksonObjectMapper(),
        geminiSummaryService = geminiSummaryService,
    )

    @Test
    fun `createReport rejects when trust registry exists without trust consent term`() {
        val request = createAnalysisRequest()
        val requestId = requireNotNull(request.id)
        val registryRaw = createRegistryRaw(request)
        val rightsAnalysis = RightsAnalysis(
            request = request,
            registryRaw = registryRaw,
            currentOwner = "현재소유자",
            isOwnerMatched = true,
            isRecentlyOwnerChanged = false,
            ownerChangeCount = 0,
            totalMortgageAmount = 0L,
            totalJeonseRightAmount = 0L,
            totalLeaseRightAmount = 0L,
            hasSeizure = false,
            hasProvisionalSeizure = false,
            hasForcedAuction = false,
            hasTrust = true,
            hasLeaseRegistration = false,
            registryDate = LocalDate.now(),
            riskScore = 30,
            riskReason = "신탁 등기가 존재합니다.",
        )

        Mockito.`when`(analysisRequestRepository.findById(requestId)).thenReturn(Optional.of(request))
        Mockito.`when`(rightsAnalysisRepository.findTopByRequestOrderByAnalyzedAtDesc(request)).thenReturn(rightsAnalysis)
        Mockito.`when`(registryMortgageRepository.findByRegistryRawOrderByRankNumberAsc(registryRaw)).thenReturn(emptyList())
        Mockito.`when`(registryRestrictionRepository.findByRegistryRawOrderByRankNumberAsc(registryRaw))
            .thenReturn(listOf(createTrustRestriction(registryRaw)))
        Mockito.`when`(diagnosisReportRepository.save(Mockito.any(DiagnosisReport::class.java))).thenAnswer { it.arguments[0] }

        val response = service.createReport(requestId, DiagnosisSupplementRequest())

        assertEquals(Verdict.REJECT, response.verdict)
        assertTrue(response.topRisks.any { it.title == "신탁 등기 동의 누락" })
    }

    @Test
    fun `createReport rejects when vacancy input conflicts with occupant state`() {
        val request = createAnalysisRequest()
        val requestId = requireNotNull(request.id)
        val supplement = DiagnosisSupplementRequest(
            occupancy = OccupancyInput(
                declaredVacant = true,
                currentOccupantExists = true,
            )
        )

        Mockito.`when`(analysisRequestRepository.findById(requestId)).thenReturn(Optional.of(request))
        Mockito.`when`(diagnosisReportRepository.save(Mockito.any(DiagnosisReport::class.java))).thenAnswer { it.arguments[0] }

        val response = service.createReport(requestId, supplement)

        assertEquals(Verdict.REJECT, response.verdict)
        assertTrue(response.topRisks.any { it.title == "공실-점유 충돌" })
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
            contractType = ContractType.JEONSE,
            depositAmount = 30000L,
            floor = 12,
            exclusiveArea = 84.9,
            contractDate = LocalDate.of(2024, 4, 20),
            balanceDate = LocalDate.of(2024, 5, 20),
            expiryDate = LocalDate.of(2026, 5, 20),
            status = AnalysisStatus.PENDING,
        ).also { ReflectionTestUtils.setField(it, "id", UUID.randomUUID()) }
    }

    private fun createRegistryRaw(request: AnalysisRequest): RegistryRaw {
        val candidate = RegistryCandidate(
            property = request.property,
            uniqueNumber = "110111-1234567",
            realEstateType = "집합건물",
            location = "서울특별시 강남구 역삼동 123-45",
        )

        return RegistryRaw(
            registryCandidate = candidate,
            request = request,
            uniqueNumber = "110111-1234567",
            sourceHash = "a".repeat(64),
            parseStatus = ParseStatus.SUCCESS,
        )
    }

    private fun createTrustRestriction(registryRaw: RegistryRaw): RegistryRestriction =
        RegistryRestriction(
            registryRaw = registryRaw,
            rankNumber = 1,
            registrationPurpose = "신탁",
            receptionDate = LocalDate.now(),
            rightHolderName = "신탁회사",
            detail = "부동산 담보신탁",
        )
}
