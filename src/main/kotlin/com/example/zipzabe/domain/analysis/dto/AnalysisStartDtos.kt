package com.example.zipzabe.domain.analysis.dto

import com.example.zipzabe.domain.building.dto.BuildingLedgerFetchResponse
import com.example.zipzabe.domain.registry.dto.RegistryOcrResponse
import com.example.zipzabe.domain.report.dto.DiagnosisReportResponse
import com.example.zipzabe.domain.report.dto.DiagnosisSupplementRequest
import com.example.zipzabe.domain.trade.dto.RentTradeFetchResponse
import com.example.zipzabe.domain.trade.entity.BuildingType

data class AnalysisStartRequest(
    val building: BuildingLedgerStartRequest,
    val registry: RegistryApickStartRequest = RegistryApickStartRequest(),
    val rentTradeMonths: Int = 24,
    val rentTradeBuildingType: BuildingType? = null,
    val diagnosisSupplement: DiagnosisSupplementRequest = DiagnosisSupplementRequest(),
)

data class BuildingLedgerStartRequest(
    val dong: String,
    val ho: String,
)

data class RegistryApickStartRequest(
    val address: String? = null,
    val uniqueNum: String? = null,
    val type: String? = null,
)

data class AnalysisStartResponse(
    val buildingLedger: BuildingLedgerFetchResponse,
    val registryOcr: RegistryOcrResponse,
    val rentTrades: RentTradeFetchResponse,
    val priceAnalysis: PriceAnalysisResponse,
    val publicLedgerSummary: PublicLedgerSummaryResponse,
    val guaranteeAnalysis: GuaranteeAnalysisResponse,
    val recoveryAnalysis: RecoveryAnalysisResponse,
    val fraudPatternAnalysis: FraudPatternAnalysisResponse,
    val diagnosisReport: DiagnosisReportResponse,
)
