package com.example.zipzabe.domain.analysis.service

import com.example.zipzabe.domain.analysis.dto.RecoveryAnalysisResponse
import com.example.zipzabe.domain.analysis.entity.RecoveryAnalysis
import com.example.zipzabe.domain.analysis.entity.RecoveryGrade
import com.example.zipzabe.domain.analysis.repository.AnalysisRequestRepository
import com.example.zipzabe.domain.analysis.repository.RecoveryAnalysisRepository
import com.example.zipzabe.domain.analysis.repository.RightsAnalysisRepository
import com.example.zipzabe.domain.property.entity.Property
import com.example.zipzabe.domain.trade.entity.ContractType
import com.example.zipzabe.domain.trade.repository.TradeRecordRepository
import com.example.zipzabe.global.error.exception.AnalysisRequestNotFoundException
import com.example.zipzabe.global.error.exception.RecoveryAnalysisNotFoundException
import com.example.zipzabe.global.error.exception.RightsAnalysisNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class RecoveryAnalysisService(
    private val analysisRequestRepository: AnalysisRequestRepository,
    private val rightsAnalysisRepository: RightsAnalysisRepository,
    private val tradeRecordRepository: TradeRecordRepository,
    private val recoveryAnalysisRepository: RecoveryAnalysisRepository,
) {

    @Transactional
    fun analyze(requestId: UUID): RecoveryAnalysisResponse {
        val request = analysisRequestRepository.findById(requestId)
            .orElseThrow(::AnalysisRequestNotFoundException)
        val rightsAnalysis = rightsAnalysisRepository.findTopByRequestOrderByAnalyzedAtDesc(request)
            ?: throw RightsAnalysisNotFoundException()

        val estimatedPropertyValue = estimatePropertyValue(request.property, request.depositAmount)
        val totalEncumbrance = rightsAnalysis.totalMortgageAmount + rightsAnalysis.totalJeonseRightAmount
        val auctionValue = (estimatedPropertyValue * AUCTION_RATE).toLong()
        val availableForTenant = auctionValue - totalEncumbrance
        val recoveryRate = if (request.depositAmount > 0) {
            (availableForTenant.toDouble() / request.depositAmount * 100).coerceIn(0.0, 100.0)
        } else {
            0.0
        }

        val grade = when {
            recoveryRate >= 80.0 -> RecoveryGrade.SAFE
            recoveryRate >= 60.0 -> RecoveryGrade.CAUTION
            else -> RecoveryGrade.DANGER
        }
        val riskScore = when (grade) {
            RecoveryGrade.SAFE -> 0
            RecoveryGrade.CAUTION -> 40
            RecoveryGrade.DANGER -> 80
        }
        val riskReason = buildRiskReason(grade, estimatedPropertyValue, totalEncumbrance, availableForTenant, recoveryRate)

        val saved = recoveryAnalysisRepository.save(
            RecoveryAnalysis(
                request = request,
                estimatedPropertyValue = estimatedPropertyValue,
                totalEncumbrance = totalEncumbrance,
                depositAmount = request.depositAmount,
                availableForTenant = availableForTenant,
                recoveryRate = recoveryRate,
                recoveryGrade = grade,
                riskScore = riskScore,
                riskReason = riskReason,
            )
        )

        return toResponse(saved)
    }

    @Transactional(readOnly = true)
    fun getRecoveryAnalysis(requestId: UUID): RecoveryAnalysisResponse {
        val request = analysisRequestRepository.findById(requestId)
            .orElseThrow(::AnalysisRequestNotFoundException)
        val analysis = recoveryAnalysisRepository.findTopByRequestOrderByAnalyzedAtDesc(request)
            ?: throw RecoveryAnalysisNotFoundException()
        return toResponse(analysis)
    }

    private fun estimatePropertyValue(property: Property, depositAmount: Long): Long {
        val jeonseDeposits = tradeRecordRepository
            .findByPropertyOrderByContractDateDesc(property)
            .filter { it.contractType == ContractType.JEONSE }
            .map { it.depositAmount }

        val median = if (jeonseDeposits.isNotEmpty()) median(jeonseDeposits) else depositAmount
        return (median / JEONSE_RATE).toLong()
    }

    private fun median(values: List<Long>): Long {
        val sorted = values.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 0) (sorted[mid - 1] + sorted[mid]) / 2 else sorted[mid]
    }

    private fun buildRiskReason(
        grade: RecoveryGrade,
        estimatedPropertyValue: Long,
        totalEncumbrance: Long,
        availableForTenant: Long,
        recoveryRate: Double,
    ): String {
        val rateStr = "%.1f".format(recoveryRate)
        return when (grade) {
            RecoveryGrade.SAFE ->
                "경매 시 예상 회수가능액은 ${availableForTenant}만원(회수율 ${rateStr}%)으로, " +
                "보증금 회수 위험이 낮습니다."
            RecoveryGrade.CAUTION ->
                "추정 매매가 ${estimatedPropertyValue}만원 대비 선순위 부담액이 ${totalEncumbrance}만원이며, " +
                "경매 시 회수율은 ${rateStr}%로 부분 위험 수준입니다."
            RecoveryGrade.DANGER ->
                "선순위 부담액(${totalEncumbrance}만원)이 경매 낙찰 예상액을 초과하거나 근접하여 " +
                "보증금 회수율이 ${rateStr}%에 불과합니다. 계약을 재검토하세요."
        }
    }

    private fun toResponse(analysis: RecoveryAnalysis) = RecoveryAnalysisResponse(
        requestId = requireNotNull(analysis.request.id),
        estimatedPropertyValue = analysis.estimatedPropertyValue,
        totalEncumbrance = analysis.totalEncumbrance,
        depositAmount = analysis.depositAmount,
        availableForTenant = analysis.availableForTenant,
        recoveryRate = analysis.recoveryRate,
        recoveryGrade = analysis.recoveryGrade,
        riskScore = analysis.riskScore,
        riskReason = analysis.riskReason,
        analyzedAt = analysis.analyzedAt,
    )

    companion object {
        private const val JEONSE_RATE = 0.70
        private const val AUCTION_RATE = 0.65
    }
}
