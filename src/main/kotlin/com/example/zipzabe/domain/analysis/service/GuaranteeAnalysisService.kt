package com.example.zipzabe.domain.analysis.service

import com.example.zipzabe.domain.analysis.dto.GuaranteeAnalysisResponse
import com.example.zipzabe.domain.analysis.entity.GuaranteeAnalysis
import com.example.zipzabe.domain.analysis.entity.GuaranteeResult
import com.example.zipzabe.domain.analysis.repository.AnalysisRequestRepository
import com.example.zipzabe.domain.analysis.repository.GuaranteeAnalysisRepository
import com.example.zipzabe.domain.property.entity.Property
import com.example.zipzabe.domain.trade.entity.ContractType
import com.example.zipzabe.domain.trade.repository.TradeRecordRepository
import com.example.zipzabe.global.error.exception.AnalysisRequestNotFoundException
import com.example.zipzabe.global.error.exception.GuaranteeAnalysisNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class GuaranteeAnalysisService(
    private val analysisRequestRepository: AnalysisRequestRepository,
    private val tradeRecordRepository: TradeRecordRepository,
    private val guaranteeAnalysisRepository: GuaranteeAnalysisRepository,
) {

    @Transactional
    fun analyze(requestId: UUID): GuaranteeAnalysisResponse {
        val request = analysisRequestRepository.findById(requestId)
            .orElseThrow(::AnalysisRequestNotFoundException)

        val estimatedPropertyValue = estimatePropertyValue(request.property, request.depositAmount)
        val jeonseRate = request.depositAmount.toDouble() / estimatedPropertyValue * 100.0
        val regionMaxDeposit = resolveRegionMaxDeposit(request.property.city)

        val result = when {
            jeonseRate > 100.0 || request.depositAmount > regionMaxDeposit -> GuaranteeResult.NOT_POSSIBLE
            jeonseRate > 90.0 || request.depositAmount > (regionMaxDeposit * 0.9).toLong() -> GuaranteeResult.NEEDS_ADDITIONAL_CHECK
            else -> GuaranteeResult.POSSIBLE
        }
        val riskScore = when (result) {
            GuaranteeResult.POSSIBLE -> 0
            GuaranteeResult.NEEDS_ADDITIONAL_CHECK -> 20
            GuaranteeResult.NOT_POSSIBLE -> 50
        }
        val riskReason = buildRiskReason(result, jeonseRate, request.depositAmount, regionMaxDeposit)

        val saved = guaranteeAnalysisRepository.save(
            GuaranteeAnalysis(
                request = request,
                depositAmount = request.depositAmount,
                estimatedPropertyValue = estimatedPropertyValue,
                jeonseRate = jeonseRate,
                regionMaxDeposit = regionMaxDeposit,
                guaranteeResult = result,
                riskScore = riskScore,
                riskReason = riskReason,
            )
        )

        return toResponse(saved)
    }

    @Transactional(readOnly = true)
    fun getGuaranteeAnalysis(requestId: UUID): GuaranteeAnalysisResponse {
        val request = analysisRequestRepository.findById(requestId)
            .orElseThrow(::AnalysisRequestNotFoundException)
        val analysis = guaranteeAnalysisRepository.findTopByRequestOrderByAnalyzedAtDesc(request)
            ?: throw GuaranteeAnalysisNotFoundException()
        return toResponse(analysis)
    }

    private fun resolveRegionMaxDeposit(city: String): Long =
        if (city in METRO_CITIES) METRO_MAX_DEPOSIT else REGIONAL_MAX_DEPOSIT

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
        result: GuaranteeResult,
        jeonseRate: Double,
        depositAmount: Long,
        regionMaxDeposit: Long,
    ): String {
        val rateStr = "%.1f".format(jeonseRate)
        return when (result) {
            GuaranteeResult.POSSIBLE ->
                "전세가율 ${rateStr}%, 보증금 ${depositAmount}만원으로 HUG 전세보증보험 가입이 가능합니다."
            GuaranteeResult.NEEDS_ADDITIONAL_CHECK ->
                "전세가율 ${rateStr}%로 기준(90%)에 근접합니다. 보증 가입 여부를 HUG에 직접 확인하세요."
            GuaranteeResult.NOT_POSSIBLE ->
                "전세가율 ${rateStr}% 또는 보증금(${depositAmount}만원)이 지역 한도(${regionMaxDeposit}만원)를 초과하여 " +
                "HUG 전세보증보험 가입이 불가합니다."
        }
    }

    private fun toResponse(analysis: GuaranteeAnalysis) = GuaranteeAnalysisResponse(
        requestId = requireNotNull(analysis.request.id),
        depositAmount = analysis.depositAmount,
        estimatedPropertyValue = analysis.estimatedPropertyValue,
        jeonseRate = analysis.jeonseRate,
        regionMaxDeposit = analysis.regionMaxDeposit,
        guaranteeResult = analysis.guaranteeResult,
        riskScore = analysis.riskScore,
        riskReason = analysis.riskReason,
        analyzedAt = analysis.analyzedAt,
    )

    companion object {
        private const val JEONSE_RATE = 0.70
        private const val METRO_MAX_DEPOSIT = 70_000L   // 만원
        private const val REGIONAL_MAX_DEPOSIT = 50_000L // 만원
        private val METRO_CITIES = setOf("서울", "경기", "인천")
    }
}
