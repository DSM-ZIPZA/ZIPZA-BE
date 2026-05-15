package com.example.zipzabe.domain.analysis.service

import com.example.zipzabe.domain.analysis.dto.AnalysisStartRequest
import com.example.zipzabe.domain.analysis.dto.AnalysisStartResponse
import com.example.zipzabe.domain.building.dto.BuildingLedgerFetchRequest
import com.example.zipzabe.domain.building.service.BuildingLedgerImportService
import com.example.zipzabe.domain.registry.dto.RegistryApickOcrRequest
import com.example.zipzabe.domain.registry.service.RegistryOcrImportService
import com.example.zipzabe.domain.report.service.DiagnosisReportService
import com.example.zipzabe.domain.trade.service.RentTradeService
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
) {

    fun start(requestId: UUID, request: AnalysisStartRequest): AnalysisStartResponse {
        val buildingLedger = buildingLedgerImportService.fetchAndSave(
            requestId = requestId,
            fetchRequest = BuildingLedgerFetchRequest(
                dong = request.building.dong,
                ho = request.building.ho,
            ),
        )
        val registryOcr = registryOcrImportService.importRegistryFromApick(
            requestId = requestId,
            request = RegistryApickOcrRequest(
                address = request.registry.address,
                uniqueNum = request.registry.uniqueNum,
                type = request.registry.type,
            ),
        )
        val rentTrades = rentTradeService.fetchRentTrades(
            requestId = requestId,
            months = request.rentTradeMonths,
            buildingType = request.rentTradeBuildingType,
        )
        val priceAnalysis = priceAnalysisService.analyze(requestId, request.rentTradeMonths)
        val publicLedgerSummary = publicLedgerSummaryService.analyze(requestId)
        val guaranteeAnalysis = guaranteeAnalysisService.analyze(requestId)
        val recoveryAnalysis = recoveryAnalysisService.analyze(requestId)
        val fraudPatternAnalysis = fraudPatternAnalysisService.analyze(requestId)
        val diagnosisReport = diagnosisReportService.createReport(requestId, request.diagnosisSupplement)

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
        )
    }
}
