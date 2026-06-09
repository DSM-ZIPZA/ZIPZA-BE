package com.example.zipzabe.domain.analysis.service

import com.example.zipzabe.domain.analysis.dto.PublicLedgerSummaryResponse
import com.example.zipzabe.domain.analysis.entity.AnalysisRequest
import com.example.zipzabe.domain.analysis.entity.AnalysisStatus
import com.example.zipzabe.domain.analysis.entity.BuildingAnalysis
import com.example.zipzabe.domain.analysis.entity.RightsAnalysis
import com.example.zipzabe.domain.analysis.repository.AnalysisRequestRepository
import com.example.zipzabe.domain.analysis.repository.BuildingAnalysisRepository
import com.example.zipzabe.domain.analysis.repository.RightsAnalysisRepository
import com.example.zipzabe.domain.building.entity.BuildingLedger
import com.example.zipzabe.domain.building.repository.BuildingLedgerRepository
import com.example.zipzabe.domain.property.entity.Property
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
import com.example.zipzabe.global.error.exception.AnalysisRequestNotFoundException
import com.example.zipzabe.global.error.exception.BuildingLedgerNotFoundException
import com.example.zipzabe.global.error.exception.PublicLedgerSummaryNotFoundException
import com.example.zipzabe.global.error.exception.RegistryRawNotFoundException
import com.example.zipzabe.global.error.exception.RegistryTitleNotFoundException
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.math.abs
import kotlin.math.min

@Service
class PublicLedgerSummaryService(
    private val analysisRequestRepository: AnalysisRequestRepository,
    private val buildingLedgerRepository: BuildingLedgerRepository,
    private val registryRawRepository: RegistryRawRepository,
    private val registryOwnershipRepository: RegistryOwnershipRepository,
    private val registryRestrictionRepository: RegistryRestrictionRepository,
    private val registryMortgageRepository: RegistryMortgageRepository,
    private val registryTitleRepository: RegistryTitleRepository,
    private val buildingAnalysisRepository: BuildingAnalysisRepository,
    private val rightsAnalysisRepository: RightsAnalysisRepository,
    private val objectMapper: ObjectMapper,
) {

    @Transactional
    fun analyze(requestId: UUID): PublicLedgerSummaryResponse {
        val rawData = fetchRawData(requestId)
        rawData.request.status = AnalysisStatus.IN_PROGRESS

        val buildingSummary = buildBuildingSummary(rawData.request, rawData.buildingLedger, rawData.registryTitle)
        val rightsSummary = buildRightsSummary(rawData.registryTitle, rawData.ownerships, rawData.restrictions, rawData.mortgages)

        persistBuildingAnalysis(rawData.request, rawData.buildingLedger, buildingSummary)
        persistRightsAnalysis(rawData.request, rawData.registryRaw, rightsSummary)

        rawData.request.status = AnalysisStatus.COMPLETED

        return buildResponse(rawData.request, rawData.buildingLedger, rawData.registryTitle, buildingSummary, rightsSummary)
    }

    @Transactional(readOnly = true)
    fun getSummary(requestId: UUID): PublicLedgerSummaryResponse {
        val request = analysisRequestRepository.findById(requestId)
            .orElseThrow(::AnalysisRequestNotFoundException)

        buildingAnalysisRepository.findTopByRequestOrderByAnalyzedAtDesc(request)
            ?: throw PublicLedgerSummaryNotFoundException()

        val rawData = fetchRawData(requestId)
        val buildingSummary = buildBuildingSummary(rawData.request, rawData.buildingLedger, rawData.registryTitle)
        val rightsSummary = buildRightsSummary(rawData.registryTitle, rawData.ownerships, rawData.restrictions, rawData.mortgages)

        return buildResponse(rawData.request, rawData.buildingLedger, rawData.registryTitle, buildingSummary, rightsSummary)
    }

    private fun fetchRawData(requestId: UUID): RawData {
        val request = analysisRequestRepository.findById(requestId)
            .orElseThrow(::AnalysisRequestNotFoundException)
        val buildingLedger = buildingLedgerRepository.findTopByPropertyOrderByFetchedAtDesc(request.property)
            ?: throw BuildingLedgerNotFoundException()
        val registryRaw = registryRawRepository.findTopByRequestOrderByFetchedAtDesc(request)
            ?: throw RegistryRawNotFoundException()
        val registryTitle = registryTitleRepository.findByRegistryRaw(registryRaw).firstOrNull()
            ?: throw RegistryTitleNotFoundException()
        val ownerships = registryOwnershipRepository.findByRegistryRawOrderByRankNumberAsc(registryRaw)
        val restrictions = registryRestrictionRepository.findByRegistryRawOrderByRankNumberAsc(registryRaw)
        val mortgages = registryMortgageRepository.findByRegistryRawOrderByRankNumberAsc(registryRaw)

        return RawData(request, buildingLedger, registryRaw, registryTitle, ownerships, restrictions, mortgages)
    }

    private fun buildResponse(
        request: AnalysisRequest,
        buildingLedger: BuildingLedger,
        registryTitle: RegistryTitle,
        buildingSummary: BuildingSummary,
        rightsSummary: RightsSummary,
    ): PublicLedgerSummaryResponse {
        val overallWarnings = (buildingSummary.warnings + rightsSummary.warnings).distinct()

        return PublicLedgerSummaryResponse(
            requestId = requireNotNull(request.id),
            matching = PublicLedgerSummaryResponse.MatchingSummary(
                isAddressMatched = buildingSummary.isAddressMatched,
                isAreaMatched = buildingSummary.isAreaMatched,
                isFloorMatched = buildingSummary.isFloorMatched,
                isUsageMatched = buildingSummary.isUsageMatched,
                overallMatched = buildingSummary.overallMatched,
            ),
            violation = PublicLedgerSummaryResponse.ViolationSummary(
                hasViolation = buildingSummary.hasViolationBuilding,
                violationType = buildingLedger.violationReason,
                warningMessage = buildingSummary.violationMessage,
            ),
            registrySummary = rightsSummary.registrySummary,
            ownershipChange = PublicLedgerSummaryResponse.OwnershipChangeSummary(
                hasRecentOwnerChange = rightsSummary.hasRecentOwnerChange,
                ownerChangeCount = rightsSummary.ownerChangeCount,
                latestOwnerChangeDate = rightsSummary.latestOwnerChangeDate,
                warningMessage = rightsSummary.ownershipWarning,
            ),
            trustRegistration = PublicLedgerSummaryResponse.TrustRegistrationSummary(
                hasTrustRegistration = rightsSummary.hasTrust,
                trustEntries = rightsSummary.trustEntries,
                warningMessage = rightsSummary.trustWarning,
            ),
            buildingComparison = PublicLedgerSummaryResponse.BuildingComparisonSummary(
                listingExclusiveArea = registryTitle.exclusiveArea?.takeIf { it > 0.0 } ?: request.exclusiveArea,
                ledgerExclusiveArea = buildingLedger.exclusiveArea,
                areaDifference = buildingSummary.areaDifference,
                listingFloor = parseFloor(registryTitle.floorInfo) ?: request.floor,
                ledgerFloorInfo = registryTitle.floorInfo,
                listingPurpose = buildingSummary.listingPurpose,
                ledgerPurpose = buildingLedger.mainPurposeName,
            ),
            overallWarnings = overallWarnings,
        )
    }

    private fun buildBuildingSummary(
        request: AnalysisRequest,
        buildingLedger: BuildingLedger,
        registryTitle: RegistryTitle,
    ): BuildingSummary {
        val addressMatched = isAddressMatched(request.property, registryTitle)
        val registryArea = registryTitle.exclusiveArea?.takeIf { it > 0.0 }
        val comparisonArea = registryArea ?: request.exclusiveArea.takeIf { it > 0.0 }
        val hasListingArea = comparisonArea != null
        val areaDifference = comparisonArea?.let { abs(it - buildingLedger.exclusiveArea) } ?: 0.0
        val areaMatched = !hasListingArea || areaDifference <= AREA_DIFF_TOLERANCE
        val parsedFloor = parseFloor(registryTitle.floorInfo)
        val hasListingFloor = request.floor != 0
        val floorMatched = !hasListingFloor || parsedFloor == request.floor
        val listingPurpose = inferListingPurpose(request.property)
        val usageMatched = isResidentialPurpose(buildingLedger.mainPurposeName)

        val warnings = mutableListOf<String>()
        if (!addressMatched) {
            warnings += "매물 주소와 등기부 표제부 주소가 일치하지 않습니다."
        }
        if (hasListingArea && !areaMatched) {
            warnings += "등기부 전유면적과 건축물대장 전용면적이 ${"%.2f".format(areaDifference)}㎡ 차이납니다."
        }
        if (hasListingFloor && parsedFloor == null) {
            warnings += "등기부 표제부의 층 정보를 해석할 수 없습니다."
        } else if (hasListingFloor && !floorMatched) {
            warnings += "입력된 층수와 등기부 표제부 층 정보가 일치하지 않습니다."
        }
        if (!usageMatched) {
            warnings += "건축물대장 주용도가 주거 용도로 확인되지 않습니다."
        }

        val violationMessage = if (buildingLedger.isViolationBuilding) {
            listOfNotNull(
                "건축물대장에서 위반건축물 표시가 확인됩니다.",
                buildingLedger.violationReason,
                buildingLedger.violationDetail
            ).joinToString(" ")
        } else {
            null
        }
        if (violationMessage != null) {
            warnings += violationMessage
        }

        return BuildingSummary(
            isAddressMatched = addressMatched,
            isAreaMatched = areaMatched,
            isFloorMatched = floorMatched,
            isUsageMatched = usageMatched,
            overallMatched = addressMatched && areaMatched && floorMatched && usageMatched,
            areaDifference = areaDifference,
            buildingAge = ChronoUnit.YEARS.between(buildingLedger.approvalDate, LocalDate.now()).toInt(),
            hasViolationBuilding = buildingLedger.isViolationBuilding,
            violationMessage = violationMessage,
            listingPurpose = listingPurpose,
            warnings = warnings.distinct(),
        )
    }

    private fun buildRightsSummary(
        registryTitle: RegistryTitle,
        ownerships: List<RegistryOwnership>,
        restrictions: List<RegistryRestriction>,
        mortgages: List<RegistryMortgage>,
    ): RightsSummary {
        val activeMortgages = mortgages.filterNot(RegistryMortgage::isErased)
        val activeRestrictions = restrictions.filterNot(RegistryRestriction::isErased)
        val currentOwners = ownerships.filter(RegistryOwnership::isCurrent)
        val trustRestrictions = activeRestrictions.filter { containsKeyword(it.registrationPurpose, it.detail, "신탁") }

        val ownerChangeCount = ownerships.count { !it.isCurrent }
        val latestOwnerChangeDate = ownerships
            .asSequence()
            .filterNot(RegistryOwnership::isCurrent)
            .mapNotNull { it.registrationCauseDate ?: it.receptionDate }
            .maxOrNull()
        val hasRecentOwnerChange = latestOwnerChangeDate?.let {
            !it.isBefore(LocalDate.now().minusDays(RECENT_OWNER_CHANGE_DAYS))
        } ?: false

        val totalMortgageAmount = activeMortgages
            .filter { containsKeyword(it.registrationPurpose, keyword = "근저당") }
            .sumOf { it.claimAmount ?: 0L }
        val totalJeonseRightAmount = activeMortgages
            .filter { containsKeyword(it.registrationPurpose, keyword = "전세권") }
            .sumOf { it.claimAmount ?: 0L }
        val totalLeaseRightAmount = activeMortgages
            .filter { containsKeyword(it.registrationPurpose, keyword = "임차권") }
            .sumOf { it.claimAmount ?: 0L }

        val hasSeizure = activeRestrictions.any {
            containsKeyword(it.registrationPurpose, it.detail, "압류") &&
                !containsKeyword(it.registrationPurpose, it.detail, "가압류")
        }
        val hasProvisionalSeizure = activeRestrictions.any {
            containsKeyword(it.registrationPurpose, it.detail, "가압류")
        }
        val hasForcedAuction = activeRestrictions.any {
            containsKeyword(it.registrationPurpose, it.detail, "강제경매")
        }
        val hasTrust = trustRestrictions.isNotEmpty()
        val hasLeaseRegistration = activeRestrictions.any {
            containsKeyword(it.registrationPurpose, it.detail, "주택임차권") ||
                containsKeyword(it.registrationPurpose, it.detail, "임차권등기")
        }

        val warnings = mutableListOf<String>()
        if (currentOwners.isEmpty()) {
            warnings += "등기부에서 현재 소유자 정보를 찾지 못했습니다."
        }
        if (hasRecentOwnerChange) {
            warnings += "최근 소유권 변동 이력이 확인됩니다."
        }
        if (hasTrust) {
            warnings += "등기부에 신탁 관련 등기가 확인됩니다."
        }
        if (hasSeizure) {
            warnings += "등기부에 압류 등기가 확인됩니다."
        }
        if (hasProvisionalSeizure) {
            warnings += "등기부에 가압류 등기가 확인됩니다."
        }
        if (hasForcedAuction) {
            warnings += "등기부에 강제경매 관련 등기가 확인됩니다."
        }
        if (hasLeaseRegistration) {
            warnings += "등기부에 임차권등기 관련 권리가 확인됩니다."
        }
        if (activeMortgages.isNotEmpty()) {
            warnings += "말소되지 않은 근저당권 또는 임차권 관련 권리가 남아 있습니다."
        }

        return RightsSummary(
            registrySummary = PublicLedgerSummaryResponse.RegistrySummary(
                realEstateType = registryTitle.realEstateType,
                locationAddress = registryTitle.locationAddress,
                buildingName = registryTitle.buildingName,
                currentOwners = currentOwners.map {
                    PublicLedgerSummaryResponse.CurrentOwnerSummary(
                        ownerName = it.ownerName,
                        ownerIdMasked = it.ownerIdMasked,
                        shareRatio = it.shareRatio,
                    )
                },
                mortgages = activeMortgages.map {
                    PublicLedgerSummaryResponse.MortgageSummary(
                        rankNumber = it.rankNumber,
                        registrationPurpose = it.registrationPurpose,
                        claimAmount = it.claimAmount,
                        creditorName = it.creditorName,
                        receptionDate = it.receptionDate,
                    )
                },
                restrictions = activeRestrictions.map {
                    PublicLedgerSummaryResponse.RestrictionSummary(
                        rankNumber = it.rankNumber,
                        registrationPurpose = it.registrationPurpose,
                        rightHolderName = it.rightHolderName,
                        detail = it.detail,
                        receptionDate = it.receptionDate,
                    )
                },
            ),
            currentOwnerLabel = summarizeCurrentOwners(currentOwners),
            hasCurrentOwner = currentOwners.isNotEmpty(),
            hasRecentOwnerChange = hasRecentOwnerChange,
            ownerChangeCount = ownerChangeCount,
            latestOwnerChangeDate = latestOwnerChangeDate,
            totalMortgageAmount = totalMortgageAmount,
            totalJeonseRightAmount = totalJeonseRightAmount,
            totalLeaseRightAmount = totalLeaseRightAmount,
            hasSeizure = hasSeizure,
            hasProvisionalSeizure = hasProvisionalSeizure,
            hasForcedAuction = hasForcedAuction,
            hasTrust = hasTrust,
            hasLeaseRegistration = hasLeaseRegistration,
            trustEntries = trustRestrictions.map {
                PublicLedgerSummaryResponse.TrustEntrySummary(
                    rankNumber = it.rankNumber,
                    registrationPurpose = it.registrationPurpose,
                    detail = it.detail,
                    receptionDate = it.receptionDate,
                )
            },
            ownershipWarning = if (hasRecentOwnerChange) {
                "최근 소유권 변동일이 $latestOwnerChangeDate 로 확인됩니다."
            } else {
                null
            },
            trustWarning = if (hasTrust) {
                "현재 유효한 등기 항목에서 신탁 관련 등기가 확인됩니다."
            } else {
                null
            },
            warnings = warnings.distinct(),
            summaryText = buildSummaryText(
                currentOwners = currentOwners,
                latestOwnerChangeDate = latestOwnerChangeDate,
                ownerChangeCount = ownerChangeCount,
                activeMortgages = activeMortgages,
                activeRestrictions = activeRestrictions,
                warnings = warnings.distinct(),
            ),
        )
    }

    private fun persistBuildingAnalysis(
        request: AnalysisRequest,
        buildingLedger: BuildingLedger,
        summary: BuildingSummary,
    ) {
        buildingAnalysisRepository.save(
            BuildingAnalysis(
                request = request,
                buildingLedger = buildingLedger,
                isPurposeMatched = summary.isUsageMatched,
                isAreaMatched = summary.isAreaMatched,
                areaDifference = summary.areaDifference,
                isFloorInRange = summary.isFloorMatched,
                buildingAge = summary.buildingAge,
                riskScore = calculateBuildingRiskScore(summary),
                riskReason = summary.warnings.ifEmpty {
                    listOf("매물 정보와 공적 건축물 정보 사이에 주요 불일치가 발견되지 않았습니다.")
                }.joinToString(" "),
                isAddressMatched = summary.isAddressMatched,
                isUsageMatched = summary.isUsageMatched,
                hasViolationBuilding = summary.hasViolationBuilding,
                violationMessage = summary.violationMessage,
                comparisonWarnings = objectMapper.writeValueAsString(summary.warnings),
            )
        )
    }

    private fun persistRightsAnalysis(
        request: AnalysisRequest,
        registryRaw: RegistryRaw,
        summary: RightsSummary,
    ) {
        rightsAnalysisRepository.save(
            RightsAnalysis(
                request = request,
                registryRaw = registryRaw,
                currentOwner = summary.currentOwnerLabel,
                isOwnerMatched = summary.hasCurrentOwner,
                isRecentlyOwnerChanged = summary.hasRecentOwnerChange,
                ownerChangeCount = summary.ownerChangeCount,
                totalMortgageAmount = summary.totalMortgageAmount,
                totalJeonseRightAmount = summary.totalJeonseRightAmount,
                totalLeaseRightAmount = summary.totalLeaseRightAmount,
                hasSeizure = summary.hasSeizure,
                hasProvisionalSeizure = summary.hasProvisionalSeizure,
                hasForcedAuction = summary.hasForcedAuction,
                hasTrust = summary.hasTrust,
                hasLeaseRegistration = summary.hasLeaseRegistration,
                registryDate = registryRaw.fetchedAt.toLocalDate(),
                riskScore = calculateRightsRiskScore(summary),
                riskReason = summary.warnings.ifEmpty {
                    listOf("등기부에서 주요 권리 위험이 발견되지 않았습니다.")
                }.joinToString(" "),
                latestOwnerChangeDate = summary.latestOwnerChangeDate,
                trustEntryCount = summary.trustEntries.size,
                summaryText = summary.summaryText,
            )
        )
    }

    private fun isAddressMatched(property: Property, registryTitle: RegistryTitle): Boolean {
        val registryAddress = normalize(registryTitle.locationAddress)
        if (registryAddress.isBlank()) {
            return false
        }

        val candidates = listOfNotNull(
            property.roadAddress,
            property.jibunAddress,
            property.detailAddress,
            property.buildingName,
            registryTitle.buildingName,
        ).map(::normalize)

        return candidates.any { candidate ->
            candidate.isNotBlank() && (registryAddress.contains(candidate) || candidate.contains(registryAddress))
        }
    }

    private fun inferListingPurpose(property: Property): String =
        if (property.isApartment) "아파트" else "주거시설"

    private fun isResidentialPurpose(mainPurposeName: String): Boolean {
        val normalizedPurpose = normalize(mainPurposeName)
        return RESIDENTIAL_PURPOSE_KEYWORDS.any(normalizedPurpose::contains)
    }

    private fun parseFloor(floorInfo: String?): Int? {
        if (floorInfo.isNullOrBlank()) {
            return null
        }
        val match = FLOOR_PATTERN.find(floorInfo) ?: return null
        val value = match.value.toIntOrNull() ?: return null
        return if (floorInfo.contains("지하")) -value else value
    }

    private fun containsKeyword(primary: String?, secondary: String? = null, keyword: String): Boolean {
        val values = listOfNotNull(primary, secondary).map(::normalize)
        val normalizedKeyword = normalize(keyword)
        return values.any { it.contains(normalizedKeyword) }
    }

    private fun normalize(value: String): String =
        value.lowercase()
            .replace("서울특별시", "서울")
            .replace("부산광역시", "부산")
            .replace("대구광역시", "대구")
            .replace("인천광역시", "인천")
            .replace("광주광역시", "광주")
            .replace("대전광역시", "대전")
            .replace("울산광역시", "울산")
            .replace("세종특별자치시", "세종")
            .replace("경기도", "경기")
            .replace("강원특별자치도", "강원")
            .replace("충청북도", "충북")
            .replace("충청남도", "충남")
            .replace("전북특별자치도", "전북")
            .replace("전라북도", "전북")
            .replace("전라남도", "전남")
            .replace("경상북도", "경북")
            .replace("경상남도", "경남")
            .replace("제주특별자치도", "제주")
            .replace(ADDRESS_NORMALIZE_PATTERN, "")

    private fun summarizeCurrentOwners(currentOwners: List<RegistryOwnership>): String {
        return when {
            currentOwners.isEmpty() -> "UNKNOWN"
            currentOwners.size == 1 -> currentOwners.first().ownerName.take(100)
            else -> "${currentOwners.first().ownerName} 외 ${currentOwners.size - 1}명".take(100)
        }
    }

    private fun buildSummaryText(
        currentOwners: List<RegistryOwnership>,
        latestOwnerChangeDate: LocalDate?,
        ownerChangeCount: Int,
        activeMortgages: List<RegistryMortgage>,
        activeRestrictions: List<RegistryRestriction>,
        warnings: List<String>,
    ): String {
        val ownerSummary = if (currentOwners.isEmpty()) {
            "현재 소유자: 확인 불가"
        } else {
            "현재 소유자: ${currentOwners.joinToString(", ") { it.ownerName }}"
        }
        val ownerChangeSummary = "소유권 변동 횟수: $ownerChangeCount, 최근 변동일: ${latestOwnerChangeDate ?: "없음"}"
        val mortgageSummary = "말소되지 않은 근저당권 또는 임차권 관련 권리: ${activeMortgages.size}건"
        val restrictionSummary = "말소되지 않은 제한 권리: ${activeRestrictions.size}건"
        val warningSummary = if (warnings.isEmpty()) "주의사항: 없음" else "주의사항: ${warnings.joinToString(" | ")}"

        return listOf(ownerSummary, ownerChangeSummary, mortgageSummary, restrictionSummary, warningSummary)
            .joinToString("\n")
    }

    private fun calculateBuildingRiskScore(summary: BuildingSummary): Int {
        var score = 0
        if (!summary.isAddressMatched) score += 20
        if (!summary.isAreaMatched) score += 20
        if (!summary.isFloorMatched) score += 15
        if (!summary.isUsageMatched) score += 20
        if (summary.hasViolationBuilding) score += 30
        return min(score, 100)
    }

    private fun calculateRightsRiskScore(summary: RightsSummary): Int {
        var score = 0
        if (!summary.hasCurrentOwner) score += 20
        if (summary.hasRecentOwnerChange) score += 25
        if (summary.hasTrust) score += 30
        if (summary.hasSeizure) score += 25
        if (summary.hasProvisionalSeizure) score += 20
        if (summary.hasForcedAuction) score += 25
        if (summary.hasLeaseRegistration) score += 20
        if (summary.totalMortgageAmount > 0L || summary.totalJeonseRightAmount > 0L || summary.totalLeaseRightAmount > 0L) {
            score += 10
        }
        return min(score, 100)
    }

    private data class RawData(
        val request: AnalysisRequest,
        val buildingLedger: BuildingLedger,
        val registryRaw: RegistryRaw,
        val registryTitle: RegistryTitle,
        val ownerships: List<RegistryOwnership>,
        val restrictions: List<RegistryRestriction>,
        val mortgages: List<RegistryMortgage>,
    )

    private data class BuildingSummary(
        val isAddressMatched: Boolean,
        val isAreaMatched: Boolean,
        val isFloorMatched: Boolean,
        val isUsageMatched: Boolean,
        val overallMatched: Boolean,
        val areaDifference: Double,
        val buildingAge: Int,
        val hasViolationBuilding: Boolean,
        val violationMessage: String?,
        val listingPurpose: String,
        val warnings: List<String>,
    )

    private data class RightsSummary(
        val registrySummary: PublicLedgerSummaryResponse.RegistrySummary,
        val currentOwnerLabel: String,
        val hasCurrentOwner: Boolean,
        val hasRecentOwnerChange: Boolean,
        val ownerChangeCount: Int,
        val latestOwnerChangeDate: LocalDate?,
        val totalMortgageAmount: Long,
        val totalJeonseRightAmount: Long,
        val totalLeaseRightAmount: Long,
        val hasSeizure: Boolean,
        val hasProvisionalSeizure: Boolean,
        val hasForcedAuction: Boolean,
        val hasTrust: Boolean,
        val hasLeaseRegistration: Boolean,
        val trustEntries: List<PublicLedgerSummaryResponse.TrustEntrySummary>,
        val ownershipWarning: String?,
        val trustWarning: String?,
        val warnings: List<String>,
        val summaryText: String,
    )

    companion object {
        private const val AREA_DIFF_TOLERANCE = 1.0
        private const val RECENT_OWNER_CHANGE_DAYS = 90L
        private val FLOOR_PATTERN = Regex("\\d+")
        private val ADDRESS_NORMALIZE_PATTERN = Regex("[\\s\\-_,.]")
        private val RESIDENTIAL_PURPOSE_KEYWORDS = listOf(
            "아파트",
            "주택",
            "다세대",
            "연립",
            "오피스텔",
            "공동주택",
            "다가구",
            "단독"
        )
    }
}
