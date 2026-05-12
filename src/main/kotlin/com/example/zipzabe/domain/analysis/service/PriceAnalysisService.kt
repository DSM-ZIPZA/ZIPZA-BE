package com.example.zipzabe.domain.analysis.service

import com.example.zipzabe.domain.analysis.dto.PriceAnalysisResponse
import com.example.zipzabe.domain.analysis.dto.PriceAnalysisStatus
import com.example.zipzabe.domain.analysis.entity.PriceAnalysis
import com.example.zipzabe.domain.analysis.repository.AnalysisRequestRepository
import com.example.zipzabe.domain.analysis.repository.PriceAnalysisRepository
import com.example.zipzabe.domain.trade.entity.ContractType
import com.example.zipzabe.domain.trade.entity.TradeRecord
import com.example.zipzabe.domain.trade.repository.TradeRecordRepository
import com.example.zipzabe.global.error.exception.AnalysisRequestNotFoundException
import com.example.zipzabe.global.error.exception.PriceAnalysisNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID
import kotlin.math.abs
import kotlin.math.max

@Service
class PriceAnalysisService(
    private val analysisRequestRepository: AnalysisRequestRepository,
    private val tradeRecordRepository: TradeRecordRepository,
    private val priceAnalysisRepository: PriceAnalysisRepository,
) {

    @Transactional
    fun analyze(requestId: UUID, months: Int = DEFAULT_SEARCH_MONTHS): PriceAnalysisResponse {
        val request = analysisRequestRepository.findById(requestId)
            .orElseThrow(::AnalysisRequestNotFoundException)
        val normalizedMonths = months.coerceIn(MIN_SEARCH_MONTHS, MAX_SEARCH_MONTHS)
        val baseDate = if (request.contractDate.isAfter(LocalDate.now())) LocalDate.now() else request.contractDate
        val fromDate = baseDate.minusMonths(normalizedMonths.toLong())

        val samples = tradeRecordRepository.findByPropertyOrderByContractDateDesc(request.property)
            .filter { it.contractType == ContractType.JEONSE }
            .filter { !it.contractDate.isBefore(fromDate) && !it.contractDate.isAfter(baseDate) }
            .filter { isSimilarArea(it, request.exclusiveArea) }

        val saved = if (samples.size < MIN_SAMPLE_COUNT) {
            priceAnalysisRepository.save(
                PriceAnalysis(
                    request = request,
                    referenceMinimum = 0L,
                    referenceMaximum = 0L,
                    referenceMedian = 0L,
                    sampleCount = samples.size,
                    latestTradeDate = samples.maxOfOrNull { it.contractDate } ?: baseDate,
                    isOverpriced = false,
                    riskScore = INSUFFICIENT_DATA_SCORE,
                    riskReason = "유사 전세 실거래 표본이 ${samples.size}건으로 부족해 시세 부풀리기 여부를 확정하기 어렵습니다.",
                )
            )
        } else {
            val deposits = samples.map { it.depositAmount }.sorted()
            val median = median(deposits)
            val ratio = request.depositAmount.toDouble() / median * 100.0
            val status = resolveStatus(ratio)
            priceAnalysisRepository.save(
                PriceAnalysis(
                    request = request,
                    referenceMinimum = deposits.first(),
                    referenceMaximum = deposits.last(),
                    referenceMedian = median,
                    sampleCount = samples.size,
                    latestTradeDate = samples.maxOf { it.contractDate },
                    isOverpriced = status != PriceAnalysisStatus.NORMAL,
                    riskScore = riskScore(status),
                    riskReason = buildRiskReason(status, ratio, request.depositAmount, median, samples.size),
                )
            )
        }

        return toResponse(saved, request.depositAmount)
    }

    @Transactional(readOnly = true)
    fun getPriceAnalysis(requestId: UUID): PriceAnalysisResponse {
        val request = analysisRequestRepository.findById(requestId)
            .orElseThrow(::AnalysisRequestNotFoundException)
        val analysis = priceAnalysisRepository.findTopByRequestOrderByAnalyzedAtDesc(request)
            ?: throw PriceAnalysisNotFoundException()
        return toResponse(analysis, request.depositAmount)
    }

    private fun isSimilarArea(record: TradeRecord, requestExclusiveArea: Double): Boolean {
        val tolerance = max(MIN_AREA_TOLERANCE, requestExclusiveArea * AREA_TOLERANCE_RATE)
        return abs(record.exclusiveArea - requestExclusiveArea) <= tolerance
    }

    private fun median(values: List<Long>): Long {
        val mid = values.size / 2
        return if (values.size % 2 == 0) (values[mid - 1] + values[mid]) / 2 else values[mid]
    }

    private fun resolveStatus(priceRatio: Double): PriceAnalysisStatus =
        when {
            priceRatio > DANGER_RATIO -> PriceAnalysisStatus.DANGER
            priceRatio > CAUTION_RATIO -> PriceAnalysisStatus.CAUTION
            else -> PriceAnalysisStatus.NORMAL
        }

    private fun riskScore(status: PriceAnalysisStatus): Int =
        when (status) {
            PriceAnalysisStatus.NORMAL -> 0
            PriceAnalysisStatus.CAUTION -> 40
            PriceAnalysisStatus.DANGER -> 70
            PriceAnalysisStatus.INSUFFICIENT_DATA -> INSUFFICIENT_DATA_SCORE
        }

    private fun buildRiskReason(
        status: PriceAnalysisStatus,
        priceRatio: Double,
        depositAmount: Long,
        median: Long,
        sampleCount: Int,
    ): String {
        val ratioText = "%.1f".format(priceRatio)
        return when (status) {
            PriceAnalysisStatus.NORMAL ->
                "보증금 ${depositAmount}만원은 유사 전세 실거래 중앙값 ${median}만원 대비 ${ratioText}%로 정상 범위입니다."
            PriceAnalysisStatus.CAUTION ->
                "보증금 ${depositAmount}만원은 유사 전세 실거래 중앙값 ${median}만원 대비 ${ratioText}%로 높아 추가 확인이 필요합니다."
            PriceAnalysisStatus.DANGER ->
                "보증금 ${depositAmount}만원은 유사 전세 실거래 중앙값 ${median}만원 대비 ${ratioText}%로 시세 부풀리기 위험이 큽니다."
            PriceAnalysisStatus.INSUFFICIENT_DATA ->
                "유사 전세 실거래 표본이 ${sampleCount}건으로 부족해 시세 부풀리기 여부를 확정하기 어렵습니다."
        }
    }

    private fun toResponse(analysis: PriceAnalysis, depositAmount: Long): PriceAnalysisResponse {
        val priceRatio = if (analysis.referenceMedian > 0L) {
            depositAmount.toDouble() / analysis.referenceMedian * 100.0
        } else {
            null
        }
        val status = if (analysis.sampleCount < MIN_SAMPLE_COUNT) {
            PriceAnalysisStatus.INSUFFICIENT_DATA
        } else {
            resolveStatus(requireNotNull(priceRatio))
        }

        return PriceAnalysisResponse(
            requestId = requireNotNull(analysis.request.id),
            depositAmount = depositAmount,
            referenceMinimum = analysis.referenceMinimum,
            referenceMaximum = analysis.referenceMaximum,
            referenceMedian = analysis.referenceMedian,
            sampleCount = analysis.sampleCount,
            latestTradeDate = analysis.latestTradeDate,
            priceRatio = priceRatio,
            status = status,
            isOverpriced = analysis.isOverpriced,
            riskScore = analysis.riskScore,
            riskReason = analysis.riskReason,
            analyzedAt = analysis.analyzedAt,
        )
    }

    companion object {
        private const val DEFAULT_SEARCH_MONTHS = 24
        private const val MIN_SEARCH_MONTHS = 1
        private const val MAX_SEARCH_MONTHS = 60
        private const val MIN_SAMPLE_COUNT = 3
        private const val MIN_AREA_TOLERANCE = 3.0
        private const val AREA_TOLERANCE_RATE = 0.10
        private const val CAUTION_RATIO = 110.0
        private const val DANGER_RATIO = 120.0
        private const val INSUFFICIENT_DATA_SCORE = 10
    }
}
