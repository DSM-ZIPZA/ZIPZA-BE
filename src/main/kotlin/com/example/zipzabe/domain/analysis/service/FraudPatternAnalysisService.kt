package com.example.zipzabe.domain.analysis.service

import com.example.zipzabe.domain.analysis.dto.FraudPatternAnalysisResponse
import com.example.zipzabe.domain.analysis.entity.FraudPatternAnalysis
import com.example.zipzabe.domain.analysis.entity.FraudSuspicionLevel
import com.example.zipzabe.domain.analysis.repository.AnalysisRequestRepository
import com.example.zipzabe.domain.analysis.repository.FraudPatternAnalysisRepository
import com.example.zipzabe.domain.analysis.repository.RightsAnalysisRepository
import com.example.zipzabe.domain.property.entity.Property
import com.example.zipzabe.domain.registry.entity.RegistryMortgage
import com.example.zipzabe.domain.registry.entity.RegistryOwnership
import com.example.zipzabe.domain.registry.repository.RegistryMortgageRepository
import com.example.zipzabe.domain.registry.repository.RegistryOwnershipRepository
import com.example.zipzabe.domain.trade.entity.ContractType
import com.example.zipzabe.domain.trade.repository.TradeRecordRepository
import com.example.zipzabe.global.error.exception.AnalysisRequestNotFoundException
import com.example.zipzabe.global.error.exception.FraudPatternAnalysisNotFoundException
import com.example.zipzabe.global.error.exception.RightsAnalysisNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.temporal.ChronoUnit
import java.util.UUID

@Service
class FraudPatternAnalysisService(
    private val analysisRequestRepository: AnalysisRequestRepository,
    private val rightsAnalysisRepository: RightsAnalysisRepository,
    private val registryMortgageRepository: RegistryMortgageRepository,
    private val registryOwnershipRepository: RegistryOwnershipRepository,
    private val tradeRecordRepository: TradeRecordRepository,
    private val fraudPatternAnalysisRepository: FraudPatternAnalysisRepository,
) {

    @Transactional
    fun analyze(requestId: UUID): FraudPatternAnalysisResponse {
        val request = analysisRequestRepository.findById(requestId)
            .orElseThrow(::AnalysisRequestNotFoundException)
        val rightsAnalysis = rightsAnalysisRepository.findTopByRequestOrderByAnalyzedAtDesc(request)
            ?: throw RightsAnalysisNotFoundException()

        val registryRaw = rightsAnalysis.registryRaw
        val mortgages = registryMortgageRepository
            .findByRegistryRawOrderByRankNumberAsc(registryRaw)
            .filterNot(RegistryMortgage::isErased)
            .sortedByDescending { it.receptionDate }
        val ownerships = registryOwnershipRepository
            .findByRegistryRawOrderByRankNumberAsc(registryRaw)

        val estimatedPropertyValue = estimatePropertyValue(request.property, request.depositAmount)

        val hasFrequentMortgageChange = detectFrequentMortgageChange(mortgages)
        val hasPostOwnershipMortgage = detectPostOwnershipMortgage(mortgages, ownerships)
        val hasOverLeveraged = rightsAnalysis.totalMortgageAmount > (estimatedPropertyValue * OVERLEVERAGED_THRESHOLD).toLong()

        var fraudScore = 0
        if (hasFrequentMortgageChange) fraudScore += 20
        if (hasPostOwnershipMortgage) fraudScore += 25
        if (hasOverLeveraged) fraudScore += 15

        val suspicionLevel = when {
            fraudScore == 0 -> FraudSuspicionLevel.LOW
            fraudScore < 25 -> FraudSuspicionLevel.MEDIUM
            else -> FraudSuspicionLevel.HIGH
        }
        val riskScore = when (suspicionLevel) {
            FraudSuspicionLevel.LOW -> 0
            FraudSuspicionLevel.MEDIUM -> 25
            FraudSuspicionLevel.HIGH -> 50
        }

        val riskReason = buildRiskReason(suspicionLevel, hasFrequentMortgageChange, hasPostOwnershipMortgage, hasOverLeveraged)

        val saved = fraudPatternAnalysisRepository.save(
            FraudPatternAnalysis(
                request = request,
                hasFrequentMortgageChange = hasFrequentMortgageChange,
                hasPostOwnershipMortgage = hasPostOwnershipMortgage,
                hasOverLeveraged = hasOverLeveraged,
                suspicionLevel = suspicionLevel,
                riskScore = riskScore,
                riskReason = riskReason,
            )
        )

        return toResponse(saved)
    }

    @Transactional(readOnly = true)
    fun getFraudPatternAnalysis(requestId: UUID): FraudPatternAnalysisResponse {
        val request = analysisRequestRepository.findById(requestId)
            .orElseThrow(::AnalysisRequestNotFoundException)
        val analysis = fraudPatternAnalysisRepository.findTopByRequestOrderByAnalyzedAtDesc(request)
            ?: throw FraudPatternAnalysisNotFoundException()
        return toResponse(analysis)
    }

    private fun detectFrequentMortgageChange(mortgages: List<RegistryMortgage>): Boolean {
        if (mortgages.size < 2) return false
        val first = mortgages[0].receptionDate ?: return false
        val second = mortgages[1].receptionDate ?: return false
        return ChronoUnit.DAYS.between(second, first) <= FREQUENT_MORTGAGE_DAYS
    }

    private fun detectPostOwnershipMortgage(
        mortgages: List<RegistryMortgage>,
        ownerships: List<RegistryOwnership>,
    ): Boolean {
        val latestOwnerChange = ownerships
            .filterNot(RegistryOwnership::isCurrent)
            .mapNotNull { it.registrationCauseDate ?: it.receptionDate }
            .maxOrNull() ?: return false
        val latestMortgageDate = mortgages.firstOrNull()?.receptionDate ?: return false
        if (latestMortgageDate.isBefore(latestOwnerChange)) return false
        return ChronoUnit.DAYS.between(latestOwnerChange, latestMortgageDate) <= POST_OWNERSHIP_MORTGAGE_DAYS
    }

    private fun estimatePropertyValue(property: Property, depositAmount: Long): Long {
        val jeonseDeposits = tradeRecordRepository
            .findByPropertyOrderByContractDateDesc(property)
            .filter { it.contractType == ContractType.JEONSE }
            .map { it.depositAmount }
        val median = if (jeonseDeposits.isNotEmpty()) {
            val sorted = jeonseDeposits.sorted()
            val mid = sorted.size / 2
            if (sorted.size % 2 == 0) (sorted[mid - 1] + sorted[mid]) / 2 else sorted[mid]
        } else {
            depositAmount
        }
        return (median / JEONSE_RATE).toLong()
    }

    private fun buildRiskReason(
        level: FraudSuspicionLevel,
        hasFrequentMortgageChange: Boolean,
        hasPostOwnershipMortgage: Boolean,
        hasOverLeveraged: Boolean,
    ): String {
        if (level == FraudSuspicionLevel.LOW) {
            return "대출사기 의심 패턴이 탐지되지 않았습니다."
        }
        val detected = mutableListOf<String>()
        if (hasFrequentMortgageChange) detected += "180일 이내 근저당 반복 설정(반복 대출 패턴 의심)"
        if (hasPostOwnershipMortgage) detected += "소유권 이전 30일 이내 근저당 설정(허위 임차인 패턴 의심)"
        if (hasOverLeveraged) detected += "총 근저당이 추정 매매가의 80% 초과(과다 담보 의심)"
        return "다음 의심 패턴이 탐지되었습니다: ${detected.joinToString(", ")}. 전문가 확인을 권장합니다."
    }

    private fun toResponse(analysis: FraudPatternAnalysis) = FraudPatternAnalysisResponse(
        requestId = requireNotNull(analysis.request.id),
        hasFrequentMortgageChange = analysis.hasFrequentMortgageChange,
        hasPostOwnershipMortgage = analysis.hasPostOwnershipMortgage,
        hasOverLeveraged = analysis.hasOverLeveraged,
        suspicionLevel = analysis.suspicionLevel,
        riskScore = analysis.riskScore,
        riskReason = analysis.riskReason,
        analyzedAt = analysis.analyzedAt,
    )

    companion object {
        private const val FREQUENT_MORTGAGE_DAYS = 180L
        private const val POST_OWNERSHIP_MORTGAGE_DAYS = 30L
        private const val OVERLEVERAGED_THRESHOLD = 0.80
        private const val JEONSE_RATE = 0.70
    }
}
