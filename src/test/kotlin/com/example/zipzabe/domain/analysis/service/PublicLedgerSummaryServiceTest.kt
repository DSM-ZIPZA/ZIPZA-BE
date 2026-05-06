package com.example.zipzabe.domain.analysis.service

import com.example.zipzabe.domain.analysis.entity.AnalysisRequest
import com.example.zipzabe.domain.analysis.entity.AnalysisStatus
import com.example.zipzabe.domain.analysis.entity.BuildingAnalysis
import com.example.zipzabe.domain.analysis.entity.ContractType
import com.example.zipzabe.domain.analysis.entity.RightsAnalysis
import com.example.zipzabe.domain.analysis.repository.AnalysisRequestRepository
import com.example.zipzabe.domain.analysis.repository.BuildingAnalysisRepository
import com.example.zipzabe.domain.analysis.repository.RightsAnalysisRepository
import com.example.zipzabe.domain.building.entity.BuildingLedger
import com.example.zipzabe.domain.building.repository.BuildingLedgerRepository
import com.example.zipzabe.domain.property.entity.Property
import com.example.zipzabe.domain.registry.entity.ParseStatus
import com.example.zipzabe.domain.registry.entity.RegistryCandidate
import com.example.zipzabe.domain.registry.entity.RegistryMortgage
import com.example.zipzabe.domain.registry.entity.RegistryOwnership
import com.example.zipzabe.domain.registry.entity.RegistryRaw
import com.example.zipzabe.domain.registry.entity.RegistryRestriction
import com.example.zipzabe.domain.registry.entity.RegistryTitle
import com.example.zipzabe.domain.registry.repository.RegistryMortgageRepository
import com.example.zipzabe.domain.registry.repository.RegistryOwnershipRepository
import com.example.zipzabe.domain.registry.repository.RegistryRawRepository
import com.example.zipzabe.domain.registry.repository.RegistryRestrictionRepository
import com.example.zipzabe.domain.registry.repository.RegistryTitleRepository
import com.example.zipzabe.domain.user.entity.User
import com.example.zipzabe.global.error.exception.BuildingLedgerNotFoundException
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalDate
import java.util.Optional
import java.util.UUID

class PublicLedgerSummaryServiceTest {

    private val analysisRequestRepository = Mockito.mock(AnalysisRequestRepository::class.java)
    private val buildingLedgerRepository = Mockito.mock(BuildingLedgerRepository::class.java)
    private val registryRawRepository = Mockito.mock(RegistryRawRepository::class.java)
    private val registryOwnershipRepository = Mockito.mock(RegistryOwnershipRepository::class.java)
    private val registryRestrictionRepository = Mockito.mock(RegistryRestrictionRepository::class.java)
    private val registryMortgageRepository = Mockito.mock(RegistryMortgageRepository::class.java)
    private val registryTitleRepository = Mockito.mock(RegistryTitleRepository::class.java)
    private val buildingAnalysisRepository = Mockito.mock(BuildingAnalysisRepository::class.java)
    private val rightsAnalysisRepository = Mockito.mock(RightsAnalysisRepository::class.java)

    private val service = PublicLedgerSummaryService(
        analysisRequestRepository = analysisRequestRepository,
        buildingLedgerRepository = buildingLedgerRepository,
        registryRawRepository = registryRawRepository,
        registryOwnershipRepository = registryOwnershipRepository,
        registryRestrictionRepository = registryRestrictionRepository,
        registryMortgageRepository = registryMortgageRepository,
        registryTitleRepository = registryTitleRepository,
        buildingAnalysisRepository = buildingAnalysisRepository,
        rightsAnalysisRepository = rightsAnalysisRepository,
        objectMapper = ObjectMapper(),
    )

    @Test
    fun `analyzePublicLedger returns integrated summary and saves analyses`() {
        val request = createAnalysisRequest()
        val requestId = requireNotNull(request.id)
        val buildingLedger = createBuildingLedger(request.property)
        val registryRaw = createRegistryRaw(request)
        val registryTitle = createRegistryTitle(registryRaw)
        val ownerships = listOf(
            createPreviousOwner(registryRaw),
            createCurrentOwner(registryRaw)
        )
        val restrictions = listOf(
            createTrustRestriction(registryRaw),
            createSeizureRestriction(registryRaw)
        )
        val mortgages = listOf(createMortgage(registryRaw))

        Mockito.`when`(analysisRequestRepository.findById(requestId)).thenReturn(Optional.of(request))
        Mockito.`when`(buildingLedgerRepository.findTopByPropertyOrderByFetchedAtDesc(request.property)).thenReturn(buildingLedger)
        Mockito.`when`(registryRawRepository.findTopByRequestOrderByFetchedAtDesc(request)).thenReturn(registryRaw)
        Mockito.`when`(registryTitleRepository.findByRegistryRaw(registryRaw)).thenReturn(listOf(registryTitle))
        Mockito.`when`(registryOwnershipRepository.findByRegistryRawOrderByRankNumberAsc(registryRaw)).thenReturn(ownerships)
        Mockito.`when`(registryRestrictionRepository.findByRegistryRawOrderByRankNumberAsc(registryRaw)).thenReturn(restrictions)
        Mockito.`when`(registryMortgageRepository.findByRegistryRawOrderByRankNumberAsc(registryRaw)).thenReturn(mortgages)

        val response = service.analyze(requestId)

        assertEquals(request.id, response.requestId)
        assertTrue(response.matching.isAddressMatched)
        assertTrue(response.matching.isAreaMatched)
        assertTrue(response.matching.isFloorMatched)
        assertTrue(response.matching.isUsageMatched)
        assertTrue(response.matching.overallMatched)
        assertTrue(response.violation.hasViolation)
        assertTrue(response.ownershipChange.hasRecentOwnerChange)
        assertEquals(1, response.ownershipChange.ownerChangeCount)
        assertTrue(response.trustRegistration.hasTrustRegistration)
        assertEquals(1, response.trustRegistration.trustEntries.size)
        assertEquals(1, response.registrySummary.currentOwners.size)
        assertEquals(1, response.registrySummary.mortgages.size)
        assertEquals(2, response.registrySummary.restrictions.size)
        assertTrue(response.overallWarnings.any { it.contains("Trust registration") })
        assertTrue(response.overallWarnings.any { it.contains("Violation building") })

        Mockito.verify(buildingAnalysisRepository).save(Mockito.any(BuildingAnalysis::class.java))
        Mockito.verify(rightsAnalysisRepository).save(Mockito.any(RightsAnalysis::class.java))
    }

    @Test
    fun `analyzePublicLedger throws when building ledger is missing`() {
        val request = createAnalysisRequest()
        val requestId = requireNotNull(request.id)

        Mockito.`when`(analysisRequestRepository.findById(requestId)).thenReturn(Optional.of(request))
        Mockito.`when`(buildingLedgerRepository.findTopByPropertyOrderByFetchedAtDesc(request.property)).thenReturn(null)

        assertThrows(BuildingLedgerNotFoundException::class.java) {
            service.analyze(requestId)
        }

        Mockito.verify(buildingAnalysisRepository, Mockito.never()).save(Mockito.any(BuildingAnalysis::class.java))
        Mockito.verify(rightsAnalysisRepository, Mockito.never()).save(Mockito.any(RightsAnalysis::class.java))
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
            detailAddress = "101동 12층",
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
            depositAmount = 500000L,
            floor = 12,
            exclusiveArea = 84.5,
            contractDate = LocalDate.now(),
            balanceDate = LocalDate.now().plusDays(30),
            expiryDate = LocalDate.now().plusYears(2),
            status = AnalysisStatus.PENDING,
        ).also { ReflectionTestUtils.setField(it, "id", UUID.randomUUID()) }
    }

    private fun createBuildingLedger(property: Property): BuildingLedger =
        BuildingLedger(
            property = property,
            mainPurposeCode = "02000",
            mainPurposeName = "아파트",
            totalFloorArea = 1200.0,
            buildingArea = 100.0,
            buildingCoverageRatio = 55.0,
            floorAreaRatio = 250.0,
            structureName = "철근콘크리트구조",
            floorsAboveGround = 20,
            floorsUnderground = 2,
            householdCount = 120,
            approvalDate = LocalDate.now().minusYears(5),
            isEarthquakeResistant = true,
            exclusiveArea = 84.0,
            isViolationBuilding = true,
            violationReason = "위반건축물",
            violationDetail = "증축 위반 이력이 존재합니다.",
        )

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

    private fun createRegistryTitle(registryRaw: RegistryRaw): RegistryTitle =
        RegistryTitle(
            registryRaw = registryRaw,
            realEstateType = "집합건물",
            locationAddress = "서울특별시 강남구 테헤란로 123",
            buildingName = "집자아파트",
            floorInfo = "12층",
            exclusiveArea = 84.0,
            commonArea = 20.0,
            purpose = "아파트",
        )

    private fun createPreviousOwner(registryRaw: RegistryRaw): RegistryOwnership =
        RegistryOwnership(
            registryRaw = registryRaw,
            rankNumber = 1,
            registrationPurpose = "소유권이전",
            receptionDate = LocalDate.now().minusDays(10),
            registrationCause = "매매",
            registrationCauseDate = LocalDate.now().minusDays(10),
            ownerName = "이전소유자",
            ownerIdMasked = "800101-*******",
            isCurrent = false,
        )

    private fun createCurrentOwner(registryRaw: RegistryRaw): RegistryOwnership =
        RegistryOwnership(
            registryRaw = registryRaw,
            rankNumber = 2,
            registrationPurpose = "소유권이전",
            receptionDate = LocalDate.now().minusDays(10),
            registrationCause = "매매",
            registrationCauseDate = LocalDate.now().minusDays(10),
            ownerName = "현재소유자",
            ownerIdMasked = "900101-*******",
            isCurrent = true,
        )

    private fun createTrustRestriction(registryRaw: RegistryRaw): RegistryRestriction =
        RegistryRestriction(
            registryRaw = registryRaw,
            rankNumber = 3,
            registrationPurpose = "신탁",
            receptionDate = LocalDate.now().minusDays(8),
            rightHolderName = "신탁회사",
            detail = "부동산 담보신탁",
        )

    private fun createSeizureRestriction(registryRaw: RegistryRaw): RegistryRestriction =
        RegistryRestriction(
            registryRaw = registryRaw,
            rankNumber = 4,
            registrationPurpose = "압류",
            receptionDate = LocalDate.now().minusDays(5),
            rightHolderName = "세무서",
            detail = "체납처분 압류",
        )

    private fun createMortgage(registryRaw: RegistryRaw): RegistryMortgage =
        RegistryMortgage(
            registryRaw = registryRaw,
            rankNumber = 5,
            registrationPurpose = "근저당권설정",
            receptionDate = LocalDate.now().minusMonths(3),
            registrationCause = "금전소비대차",
            claimAmount = 300000L,
            debtorName = "현재소유자",
            creditorName = "은행",
        )
}
