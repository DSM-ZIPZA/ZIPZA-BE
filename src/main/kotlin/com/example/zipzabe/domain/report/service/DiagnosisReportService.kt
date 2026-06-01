package com.example.zipzabe.domain.report.service

import com.example.zipzabe.domain.analysis.entity.AnalysisRequest
import com.example.zipzabe.domain.analysis.entity.FraudSuspicionLevel
import com.example.zipzabe.domain.analysis.entity.GuaranteeResult
import com.example.zipzabe.domain.analysis.entity.RecoveryGrade
import com.example.zipzabe.domain.analysis.repository.AnalysisRequestRepository
import com.example.zipzabe.domain.analysis.repository.BuildingAnalysisRepository
import com.example.zipzabe.domain.analysis.repository.FraudPatternAnalysisRepository
import com.example.zipzabe.domain.analysis.repository.GuaranteeAnalysisRepository
import com.example.zipzabe.domain.analysis.repository.PriceAnalysisRepository
import com.example.zipzabe.domain.analysis.repository.RecoveryAnalysisRepository
import com.example.zipzabe.domain.analysis.repository.RightsAnalysisRepository
import com.example.zipzabe.domain.analysis.service.GeminiSummaryService
import com.example.zipzabe.domain.registry.entity.RegistryMortgage
import com.example.zipzabe.domain.registry.entity.RegistryRestriction
import com.example.zipzabe.domain.registry.repository.RegistryMortgageRepository
import com.example.zipzabe.domain.registry.repository.RegistryRestrictionRepository
import com.example.zipzabe.domain.report.dto.ActionPriority
import com.example.zipzabe.domain.report.dto.DiagnosisReportResponse
import com.example.zipzabe.domain.report.dto.DiagnosisSupplementRequest
import com.example.zipzabe.domain.report.dto.NextActionResponse
import com.example.zipzabe.domain.report.dto.RiskItemResponse
import com.example.zipzabe.domain.report.dto.RiskSeverity
import com.example.zipzabe.domain.report.entity.DiagnosisReport
import com.example.zipzabe.domain.report.entity.Verdict
import com.example.zipzabe.domain.report.repository.DiagnosisReportRepository
import com.example.zipzabe.global.error.exception.AnalysisRequestNotFoundException
import com.example.zipzabe.global.error.exception.DiagnosisReportNotFoundException
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Service
class DiagnosisReportService(
    private val analysisRequestRepository: AnalysisRequestRepository,
    private val priceAnalysisRepository: PriceAnalysisRepository,
    private val buildingAnalysisRepository: BuildingAnalysisRepository,
    private val rightsAnalysisRepository: RightsAnalysisRepository,
    private val guaranteeAnalysisRepository: GuaranteeAnalysisRepository,
    private val recoveryAnalysisRepository: RecoveryAnalysisRepository,
    private val fraudPatternAnalysisRepository: FraudPatternAnalysisRepository,
    private val registryMortgageRepository: RegistryMortgageRepository,
    private val registryRestrictionRepository: RegistryRestrictionRepository,
    private val diagnosisReportRepository: DiagnosisReportRepository,
    private val objectMapper: ObjectMapper,
    private val geminiSummaryService: GeminiSummaryService,
) {

    @Transactional
    fun createReport(
        requestId: UUID,
        supplement: DiagnosisSupplementRequest = DiagnosisSupplementRequest(),
    ): DiagnosisReportResponse {
        val request = analysisRequestRepository.findById(requestId)
            .orElseThrow(::AnalysisRequestNotFoundException)

        val priceAnalysis = priceAnalysisRepository.findTopByRequestOrderByAnalyzedAtDesc(request)
        val buildingAnalysis = buildingAnalysisRepository.findTopByRequestOrderByAnalyzedAtDesc(request)
        val rightsAnalysis = rightsAnalysisRepository.findTopByRequestOrderByAnalyzedAtDesc(request)
        val guaranteeAnalysis = guaranteeAnalysisRepository.findTopByRequestOrderByAnalyzedAtDesc(request)
        val recoveryAnalysis = recoveryAnalysisRepository.findTopByRequestOrderByAnalyzedAtDesc(request)
        val fraudPatternAnalysis = fraudPatternAnalysisRepository.findTopByRequestOrderByAnalyzedAtDesc(request)

        val riskItems = mutableListOf<RiskItemResponse>()
        val nextActions = mutableListOf<NextActionResponse>()

        val priceScore = priceAnalysis?.riskScore ?: MISSING_ANALYSIS_SCORE
        if (priceAnalysis == null) {
            riskItems += RiskItemResponse("시세 분석 미실행", "전월세 실거래가 기반 시세 부풀리기 분석 결과가 없습니다.", RiskSeverity.LOW)
            nextActions += NextActionResponse("시세 부풀리기 분석을 먼저 실행하세요.", ActionPriority.MEDIUM)
        } else if (priceAnalysis.riskScore >= HIGH_PRICE_SCORE) {
            riskItems += RiskItemResponse("시세 부풀리기 위험", priceAnalysis.riskReason, RiskSeverity.HIGH)
            nextActions += NextActionResponse("인근 중개업소와 국토부 실거래가로 보증금 적정성을 재확인하세요.", ActionPriority.HIGH)
        }

        val rightsScore = rightsAnalysis?.riskScore ?: MISSING_ANALYSIS_SCORE
        if (rightsAnalysis == null) {
            riskItems += RiskItemResponse("권리 분석 미실행", "등기부 기반 권리 분석 결과가 없습니다.", RiskSeverity.MEDIUM)
            nextActions += NextActionResponse("공적장부 요약 분석을 먼저 실행하세요.", ActionPriority.HIGH)
        } else if (rightsAnalysis.riskScore >= HIGH_RIGHTS_SCORE) {
            riskItems += RiskItemResponse("권리관계 위험", rightsAnalysis.riskReason, RiskSeverity.HIGH)
            nextActions += NextActionResponse("등기부 갑구/을구 권리침해와 선순위채권을 계약 전 재확인하세요.", ActionPriority.HIGH)
        }

        val buildingScore = buildingAnalysis?.riskScore ?: 0
        if (buildingAnalysis?.hasViolationBuilding == true) {
            riskItems += RiskItemResponse("위반건축물 위험", buildingAnalysis.riskReason, RiskSeverity.HIGH)
            nextActions += NextActionResponse("건축물대장상 위반건축물 해소 여부를 확인하세요.", ActionPriority.HIGH)
        }

        val registryContext = rightsAnalysis?.let {
            RegistryContext(
                activeMortgages = registryMortgageRepository.findByRegistryRawOrderByRankNumberAsc(it.registryRaw)
                    .filterNot(RegistryMortgage::isErased),
                activeRestrictions = registryRestrictionRepository.findByRegistryRawOrderByRankNumberAsc(it.registryRaw)
                    .filterNot(RegistryRestriction::isErased),
            )
        } ?: RegistryContext(emptyList(), emptyList())

        val supplementalEvaluation = evaluateSupplement(request, supplement, registryContext)
        riskItems += supplementalEvaluation.risks
        nextActions += supplementalEvaluation.actions

        guaranteeAnalysis?.let {
            if (it.guaranteeResult == GuaranteeResult.NOT_POSSIBLE) {
                riskItems += RiskItemResponse("보증가입 불가 위험", it.riskReason, RiskSeverity.CRITICAL)
                nextActions += NextActionResponse("HUG 등 보증기관 가입 가능 여부를 공식 채널에서 확인하기 전 계약을 보류하세요.", ActionPriority.HIGH)
            } else if (it.guaranteeResult == GuaranteeResult.NEEDS_ADDITIONAL_CHECK) {
                riskItems += RiskItemResponse("보증가입 추가확인 필요", it.riskReason, RiskSeverity.MEDIUM)
            }
        }

        recoveryAnalysis?.let {
            if (it.recoveryGrade == RecoveryGrade.DANGER) {
                riskItems += RiskItemResponse("보증금 회수 위험", it.riskReason, RiskSeverity.CRITICAL)
                nextActions += NextActionResponse("선순위 부담액과 경매 시 회수 가능액을 전문가와 검토하세요.", ActionPriority.HIGH)
            }
        }

        fraudPatternAnalysis?.let {
            if (it.suspicionLevel == FraudSuspicionLevel.HIGH) {
                riskItems += RiskItemResponse("대출사기 패턴 의심", it.riskReason, RiskSeverity.HIGH)
                nextActions += NextActionResponse("최근 소유권 이전과 근저당 설정 이력을 재확인하세요.", ActionPriority.HIGH)
            }
        }

        val contractScore = supplementalEvaluation.score
        val totalScore = calculateTotalScore(
            priceScore = priceScore,
            rightsScore = rightsScore,
            buildingScore = buildingScore,
            contractScore = contractScore,
            guaranteeScore = guaranteeAnalysis?.riskScore ?: 0,
            recoveryScore = recoveryAnalysis?.riskScore ?: 0,
            fraudScore = fraudPatternAnalysis?.riskScore ?: 0,
        )
        val verdict = resolveVerdict(totalScore, riskItems)
        val confidenceScore = calculateConfidenceScore(
            priceAnalysis != null,
            buildingAnalysis != null,
            rightsAnalysis != null,
            guaranteeAnalysis != null,
            recoveryAnalysis != null,
            fraudPatternAnalysis != null,
        )

        val top3 = topRisks(riskItems)
        val aiSummary = runCatching { geminiSummaryService.summarize(buildRiskSummaryPrompt(top3)) }.getOrNull()

        val saved = diagnosisReportRepository.save(
            DiagnosisReport(
                request = request,
                priceScore = priceScore,
                rightsScore = rightsScore,
                buildingScore = buildingScore,
                contractScore = contractScore,
                confidenceScore = confidenceScore,
                totalScore = totalScore,
                verdict = verdict,
                topRisks = objectMapper.writeValueAsString(top3),
                nextActions = objectMapper.writeValueAsString(defaultActions(nextActions, verdict)),
                aiSummary = aiSummary,
            )
        )

        return toResponse(saved)
    }

    @Transactional(readOnly = true)
    fun getReport(requestId: UUID): DiagnosisReportResponse {
        val request = analysisRequestRepository.findById(requestId)
            .orElseThrow(::AnalysisRequestNotFoundException)
        val report = diagnosisReportRepository.findTopByRequestOrderByCreatedAtDesc(request)
            ?: throw DiagnosisReportNotFoundException()
        return toResponse(report)
    }

    private fun evaluateSupplement(
        request: AnalysisRequest,
        supplement: DiagnosisSupplementRequest,
        registryContext: RegistryContext,
    ): SupplementalEvaluation {
        val risks = mutableListOf<RiskItemResponse>()
        val actions = mutableListOf<NextActionResponse>()
        var score = 0

        val hasActiveMortgage = registryContext.activeMortgages.any {
            containsKeyword(it.registrationPurpose, "근저당") || containsKeyword(it.registrationPurpose, "전세권")
        }
        val hasTrust = registryContext.activeRestrictions.any { containsKeyword(it.registrationPurpose, "신탁") || containsKeyword(it.detail, "신탁") }
        val hasRightsRestriction = registryContext.activeRestrictions.any {
            containsKeyword(it.registrationPurpose, "압류") ||
                containsKeyword(it.registrationPurpose, "가압류") ||
                containsKeyword(it.registrationPurpose, "가처분") ||
                containsKeyword(it.registrationPurpose, "경매")
        }

        if (supplement.specialTerms.mortgageCancellationRequired && hasActiveMortgage) {
            score = max(score, 70)
            risks += RiskItemResponse("특약-등기 불일치", "근저당/전세권 말소 특약이 있으나 등기상 활성 담보권이 남아 있습니다.", RiskSeverity.HIGH)
            actions += NextActionResponse("잔금 전 말소 완료 등기 확인을 계약 조건으로 명확히 하세요.", ActionPriority.HIGH)
        }
        if (supplement.specialTerms.trustConsentRequired.not() && hasTrust) {
            score = max(score, 80)
            risks += RiskItemResponse("신탁 등기 동의 누락", "신탁 관련 등기가 있으나 신탁사 동의 특약 입력이 없습니다.", RiskSeverity.CRITICAL)
            actions += NextActionResponse("신탁원부와 신탁사 임대 동의 여부를 확인하기 전 계약을 보류하세요.", ActionPriority.HIGH)
        }
        if (supplement.specialTerms.noAdditionalLienBeforeMoveIn && hasRightsRestriction) {
            score = max(score, 75)
            risks += RiskItemResponse("권리침해 특약 충돌", "추가 권리설정 금지 특약이 있으나 압류/가압류/가처분/경매 관련 등기가 존재합니다.", RiskSeverity.CRITICAL)
            actions += NextActionResponse("권리침해 등기 말소 여부를 등기부 최신본으로 확인하세요.", ActionPriority.HIGH)
        }
        if (supplement.landlordRisk.knownGuaranteeAccident) {
            score = max(score, 85)
            risks += RiskItemResponse("임대인 보증사고 이력", "임대인 보증사고 이력이 있다고 입력되었습니다.", RiskSeverity.CRITICAL)
            actions += NextActionResponse("안심전세 앱 또는 보증기관을 통해 임대인 위험 정보를 재확인하세요.", ActionPriority.HIGH)
        }
        if (supplement.landlordRisk.taxArrearsKnown) {
            score = max(score, 75)
            risks += RiskItemResponse("임대인 세금 체납", "임대인 국세/지방세 체납이 있다고 입력되었습니다.", RiskSeverity.CRITICAL)
            actions += NextActionResponse("미납국세/지방세 열람 결과와 우선징수 가능성을 확인하세요.", ActionPriority.HIGH)
        }
        if ((supplement.landlordRisk.landlordGuaranteeCount ?: 0) > HIGH_GUARANTEE_COUNT) {
            score = max(score, 60)
            risks += RiskItemResponse("다주택/고위험 보유 패턴", "임대인의 전세보증 건수가 ${supplement.landlordRisk.landlordGuaranteeCount}건으로 추가심사 위험 구간입니다.", RiskSeverity.HIGH)
        }
        if (supplement.landlordRisk.simultaneousSaleAndLease) {
            score = max(score, 65)
            risks += RiskItemResponse("동시 매매-임대차 의심", "매매와 임대차가 동시에 진행되는 거래로 입력되었습니다.", RiskSeverity.HIGH)
            actions += NextActionResponse("소유권 이전일, 임대인 일치 여부, 전세보증금과 매매가 관계를 확인하세요.", ActionPriority.HIGH)
        }
        if (supplement.occupancy.declaredVacant &&
            (supplement.occupancy.currentOccupantExists || supplement.occupancy.otherHouseholdOnCertificate)
        ) {
            score = max(score, 80)
            risks += RiskItemResponse("공실-점유 충돌", "공실로 입력됐지만 현재 점유자 또는 전입세대가 존재합니다.", RiskSeverity.CRITICAL)
            actions += NextActionResponse("전입세대확인서와 현장 점유 상태를 확인하기 전 잔금을 지급하지 마세요.", ActionPriority.HIGH)
        }
        if (supplement.occupancy.moveInAvailableDate != null &&
            supplement.occupancy.tenantMoveInDate != null &&
            supplement.occupancy.moveInAvailableDate.isAfter(supplement.occupancy.tenantMoveInDate)
        ) {
            score = max(score, 50)
            risks += RiskItemResponse("입주 가능일 충돌", "입주 가능일이 예정 전입일보다 늦습니다.", RiskSeverity.MEDIUM)
            actions += NextActionResponse("인도 가능일과 전입신고 가능일을 계약서에 명확히 반영하세요.", ActionPriority.MEDIUM)
        }
        if (request.depositAmount <= 0L) {
            score = max(score, 30)
            risks += RiskItemResponse("보증금 입력 오류", "분석 요청 보증금이 0 이하입니다.", RiskSeverity.MEDIUM)
        }

        return SupplementalEvaluation(score = score, risks = risks, actions = actions)
    }

    private fun calculateTotalScore(
        priceScore: Int,
        rightsScore: Int,
        buildingScore: Int,
        contractScore: Int,
        guaranteeScore: Int,
        recoveryScore: Int,
        fraudScore: Int,
    ): Int {
        val score = priceScore * 0.20 +
            rightsScore * 0.30 +
            buildingScore * 0.10 +
            contractScore * 0.20 +
            guaranteeScore * 0.10 +
            recoveryScore * 0.05 +
            fraudScore * 0.05
        return min(100, score.roundToInt())
    }

    private fun resolveVerdict(totalScore: Int, risks: List<RiskItemResponse>): Verdict {
        if (risks.any { it.severity == RiskSeverity.CRITICAL }) return Verdict.REJECT
        return when {
            totalScore >= 80 -> Verdict.REJECT
            totalScore >= 60 -> Verdict.HOLD
            totalScore >= 30 -> Verdict.CAUTION
            else -> Verdict.POSSIBLE
        }
    }

    private fun calculateConfidenceScore(vararg completed: Boolean): Int {
        val missing = completed.count { !it }
        return (100 - missing * 12).coerceIn(40, 100)
    }

    private fun topRisks(risks: List<RiskItemResponse>): List<RiskItemResponse> =
        risks.distinctBy { it.title to it.detail }
            .sortedWith(compareByDescending<RiskItemResponse> { severityWeight(it.severity) }.thenBy { it.title })
            .take(TOP_RISK_COUNT)

    private fun defaultActions(actions: List<NextActionResponse>, verdict: Verdict): List<NextActionResponse> {
        val base = actions.distinctBy { it.action }.toMutableList()
        if (base.isEmpty()) {
            base += when (verdict) {
                Verdict.POSSIBLE -> NextActionResponse("계약 전 등기부 최신본, 전입신고, 확정일자를 최종 확인하세요.", ActionPriority.MEDIUM)
                Verdict.CAUTION -> NextActionResponse("주의 항목을 해소한 뒤 계약 진행 여부를 결정하세요.", ActionPriority.HIGH)
                Verdict.HOLD -> NextActionResponse("핵심 위험 항목을 해소하기 전 계약을 보류하세요.", ActionPriority.HIGH)
                Verdict.REJECT -> NextActionResponse("현재 입력 기준으로는 거래 진행을 권장하지 않습니다.", ActionPriority.HIGH)
            }
        }
        return base.sortedWith(compareByDescending<NextActionResponse> { priorityWeight(it.priority) }.thenBy { it.action })
            .take(NEXT_ACTION_COUNT)
    }

    private fun severityWeight(severity: RiskSeverity): Int =
        when (severity) {
            RiskSeverity.LOW -> 1
            RiskSeverity.MEDIUM -> 2
            RiskSeverity.HIGH -> 3
            RiskSeverity.CRITICAL -> 4
        }

    private fun priorityWeight(priority: ActionPriority): Int =
        when (priority) {
            ActionPriority.LOW -> 1
            ActionPriority.MEDIUM -> 2
            ActionPriority.HIGH -> 3
        }

    private fun containsKeyword(value: String?, keyword: String): Boolean =
        value?.replace("\\s".toRegex(), "")
            ?.contains(keyword.replace("\\s".toRegex(), ""), ignoreCase = true) == true

    private fun buildRiskSummaryPrompt(risks: List<RiskItemResponse>): String {
        val items = risks.mapIndexed { i, r ->
            "${i + 1}. [${r.severity}] ${r.title}: ${r.detail}"
        }.joinToString("\n")
        return """
            다음은 전세 계약 위험도 분석 결과 핵심 위험 항목입니다:

            $items

            임차인 관점에서 위 위험 항목들의 핵심 내용을 정확히 3줄로 요약해 주세요.
            각 줄은 하나의 완결된 문장이어야 하며, 번호나 기호를 붙이지 마세요.
        """.trimIndent()
    }

    private fun toResponse(report: DiagnosisReport): DiagnosisReportResponse =
        DiagnosisReportResponse(
            requestId = requireNotNull(report.request.id),
            priceScore = report.priceScore,
            rightsScore = report.rightsScore,
            buildingScore = report.buildingScore,
            contractScore = report.contractScore,
            confidenceScore = report.confidenceScore,
            totalScore = report.totalScore,
            verdict = report.verdict,
            topRisks = readRisks(report.topRisks),
            nextActions = readActions(report.nextActions),
            aiSummary = report.aiSummary,
            createdAt = report.createdAt,
        )

    private fun readRisks(json: String): List<RiskItemResponse> =
        objectMapper.readValue(json, object : TypeReference<List<RiskItemResponse>>() {})

    private fun readActions(json: String): List<NextActionResponse> =
        objectMapper.readValue(json, object : TypeReference<List<NextActionResponse>>() {})

    private data class RegistryContext(
        val activeMortgages: List<RegistryMortgage>,
        val activeRestrictions: List<RegistryRestriction>,
    )

    private data class SupplementalEvaluation(
        val score: Int,
        val risks: List<RiskItemResponse>,
        val actions: List<NextActionResponse>,
    )

    companion object {
        private const val MISSING_ANALYSIS_SCORE = 10
        private const val HIGH_PRICE_SCORE = 40
        private const val HIGH_RIGHTS_SCORE = 50
        private const val HIGH_GUARANTEE_COUNT = 50
        private const val TOP_RISK_COUNT = 3
        private const val NEXT_ACTION_COUNT = 5
    }
}
