package com.example.zipzabe.domain.analysis.service

import com.example.zipzabe.domain.analysis.dto.AnalysisStartRequest
import com.example.zipzabe.domain.analysis.dto.AnalysisStartResponse
import com.example.zipzabe.domain.analysis.dto.AnalysisStartSkippedStep
import com.example.zipzabe.domain.building.dto.BuildingLedgerFetchRequest
import com.example.zipzabe.domain.building.service.BuildingLedgerImportService
import com.example.zipzabe.domain.registry.dto.RegistryApickOcrRequest
import com.example.zipzabe.domain.registry.service.RegistryOcrImportService
import com.example.zipzabe.domain.report.service.DiagnosisReportService
import com.example.zipzabe.domain.report.service.ManualCheckItemService
import com.example.zipzabe.domain.trade.service.RentTradeService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class AnalysisStartService(
    private val buildingLedgerImportService: BuildingLedgerImportService,
    private val registryOcrImportService: RegistryOcrImportService,
    private val rentTradeService: RentTradeService,
    private val priceAnalysisService: PriceAnalysisService,
    private val publicLedgerSummaryService: PublicLedgerSummaryService,
    private val guaranteeAnalysisService: GuaranteeAnalysisService,
    private val recoveryAnalysisService: RecoveryAnalysisService,
    private val fraudPatternAnalysisService: FraudPatternAnalysisService,
    private val diagnosisReportService: DiagnosisReportService,
    private val manualCheckItemService: ManualCheckItemService,
) {
    private val log = LoggerFactory.getLogger(AnalysisStartService::class.java)

    fun start(requestId: UUID, request: AnalysisStartRequest): AnalysisStartResponse {
        val skippedSteps = mutableListOf<AnalysisStartSkippedStep>()

        val buildingLedger = runStep(requestId, "buildingLedger", skippedSteps) {
            buildingLedgerImportService.fetchAndSave(
            requestId = requestId,
            fetchRequest = BuildingLedgerFetchRequest(
                dong = request.building.dong,
                ho = request.building.ho,
            ),
            )
        }
        val registryOcr = runStep(requestId, "registryOcr", skippedSteps) {
            registryOcrImportService.importRegistryFromApick(
            requestId = requestId,
            request = RegistryApickOcrRequest(
                address = request.registry.address,
                uniqueNum = request.registry.uniqueNum,
                type = request.registry.type,
            ),
            )
        }
        val rentTrades = runStep(requestId, "rentTrades", skippedSteps) {
            rentTradeService.fetchRentTrades(
            requestId = requestId,
            months = request.rentTradeMonths,
            buildingType = request.rentTradeBuildingType,
            )
        }
        val priceAnalysis = runStep(requestId, "priceAnalysis", skippedSteps) {
            priceAnalysisService.analyze(requestId, request.rentTradeMonths)
        }
        val publicLedgerSummary = if (buildingLedger != null && registryOcr != null) {
            runStep(requestId, "publicLedgerSummary", skippedSteps) {
                publicLedgerSummaryService.analyze(requestId)
            }
        } else {
            skippedSteps += AnalysisStartSkippedStep(
                step = "publicLedgerSummary",
                reason = "건축물대장 또는 등기부등본 수집이 완료되지 않아 공적장부 요약을 건너뜁니다.",
            )
            null
        }
        val guaranteeAnalysis = runStep(requestId, "guaranteeAnalysis", skippedSteps) {
            guaranteeAnalysisService.analyze(requestId)
        }
        val recoveryAnalysis = if (publicLedgerSummary != null) {
            runStep(requestId, "recoveryAnalysis", skippedSteps) {
                recoveryAnalysisService.analyze(requestId)
            }
        } else {
            skippedSteps += AnalysisStartSkippedStep(
                step = "recoveryAnalysis",
                reason = "권리 분석 결과가 없어 보증금 회수 분석을 건너뜁니다.",
            )
            null
        }
        val fraudPatternAnalysis = if (publicLedgerSummary != null) {
            runStep(requestId, "fraudPatternAnalysis", skippedSteps) {
                fraudPatternAnalysisService.analyze(requestId)
            }
        } else {
            skippedSteps += AnalysisStartSkippedStep(
                step = "fraudPatternAnalysis",
                reason = "권리 분석 결과가 없어 사기 패턴 분석을 건너뜁니다.",
            )
            null
        }
        val diagnosisReport = runStep(requestId, "diagnosisReport", skippedSteps) {
            diagnosisReportService.createReport(requestId, request.diagnosisSupplement)
        }
        val manualChecks = runStep(requestId, "manualChecks", skippedSteps) {
            manualCheckItemService.generate(requestId)
        }

        return AnalysisStartResponse(
            buildingLedger = buildingLedger,
            registryOcr = registryOcr,
            rentTrades = rentTrades,
            priceAnalysis = priceAnalysis,
            publicLedgerSummary = publicLedgerSummary,
            guaranteeAnalysis = guaranteeAnalysis,
            recoveryAnalysis = recoveryAnalysis,
            fraudPatternAnalysis = fraudPatternAnalysis,
            diagnosisReport = diagnosisReport,
            manualChecks = manualChecks,
            skippedSteps = skippedSteps,
        )
    }

    private fun <T> runStep(
        requestId: UUID,
        step: String,
        skippedSteps: MutableList<AnalysisStartSkippedStep>,
        block: () -> T,
    ): T? =
        runCatching(block).getOrElse { e ->
            log.warn(
                "Analysis step skipped. requestId={} step={} reason={}",
                requestId,
                step,
                e.message ?: e::class.simpleName,
                e,
            )
            skippedSteps += AnalysisStartSkippedStep(
                step = step,
                reason = e.message ?: e::class.simpleName ?: "Unknown error",
            )
            null
        }
}
