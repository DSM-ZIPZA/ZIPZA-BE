package com.example.zipzabe.domain.report.service

import com.example.zipzabe.domain.analysis.repository.AnalysisRequestRepository
import com.example.zipzabe.domain.report.dto.ManualCheckItemResponse
import com.example.zipzabe.domain.report.dto.ManualCheckListResponse
import com.example.zipzabe.domain.report.dto.RiskSeverity
import com.example.zipzabe.domain.report.entity.CheckType
import com.example.zipzabe.domain.report.entity.ManualCheckItem
import com.example.zipzabe.domain.report.repository.ManualCheckItemRepository
import com.example.zipzabe.global.error.exception.AnalysisRequestNotFoundException
import com.example.zipzabe.global.error.exception.ManualCheckItemNotFoundException
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Service
class ManualCheckItemService(
    private val analysisRequestRepository: AnalysisRequestRepository,
    private val manualCheckItemRepository: ManualCheckItemRepository,
    private val objectMapper: ObjectMapper,
) {

    @Transactional
    fun generate(requestId: UUID): ManualCheckListResponse {
        val request = analysisRequestRepository.findById(requestId)
            .orElseThrow(::AnalysisRequestNotFoundException)

        manualCheckItemRepository.deleteAllByRequest(request)

        val items = TEMPLATES.map { template ->
            manualCheckItemRepository.save(
                ManualCheckItem(
                    request = request,
                    checkType = template.checkType,
                    title = template.title,
                    badgeText = template.badgeText,
                    severity = template.severity,
                    guideText = template.guideText,
                    procedureSteps = objectMapper.writeValueAsString(template.steps),
                    officialUrl = template.officialUrl,
                    expertConsult = template.expertConsult,
                )
            )
        }

        return toListResponse(requestId, items)
    }

    @Transactional(readOnly = true)
    fun list(requestId: UUID): ManualCheckListResponse {
        val request = analysisRequestRepository.findById(requestId)
            .orElseThrow(::AnalysisRequestNotFoundException)
        val items = manualCheckItemRepository.findByRequestOrderByCreatedAtAsc(request)
        return toListResponse(requestId, items)
    }

    @Transactional
    fun markCompleted(requestId: UUID, itemId: UUID): ManualCheckItemResponse {
        val request = analysisRequestRepository.findById(requestId)
            .orElseThrow(::AnalysisRequestNotFoundException)
        val item = manualCheckItemRepository.findById(itemId)
            .orElseThrow(::ManualCheckItemNotFoundException)
        if (item.request.id != request.id) throw ManualCheckItemNotFoundException()
        item.isCompleted = true
        item.checkedAt = LocalDateTime.now()
        return toResponse(manualCheckItemRepository.save(item))
    }

    private fun toListResponse(requestId: UUID, items: List<ManualCheckItem>): ManualCheckListResponse =
        ManualCheckListResponse(
            requestId = requestId,
            items = items.map(::toResponse),
        )

    private fun toResponse(item: ManualCheckItem): ManualCheckItemResponse =
        ManualCheckItemResponse(
            id = requireNotNull(item.id),
            checkType = item.checkType,
            title = item.title,
            badgeText = item.badgeText,
            severity = item.severity,
            guideText = item.guideText,
            procedureSteps = objectMapper.readValue(
                item.procedureSteps,
                object : TypeReference<List<String>>() {}
            ),
            officialUrl = item.officialUrl,
            expertConsult = item.expertConsult,
            isCompleted = item.isCompleted,
            checkedAt = item.checkedAt,
        )

    private data class AdvisoryTemplate(
        val checkType: CheckType,
        val title: String,
        val badgeText: String?,
        val severity: RiskSeverity,
        val guideText: String,
        val steps: List<String>,
        val officialUrl: String?,
        val expertConsult: Boolean = false,
    )

    companion object {
        private val TEMPLATES = listOf(
            AdvisoryTemplate(
                checkType = CheckType.RESIDENT_REGISTRATION_CONFIRM,
                title = "전입세대확인서 확인",
                badgeText = "직접 확인 필요",
                severity = RiskSeverity.HIGH,
                guideText = "등기부등본만으로는 현재 해당 주소에 전입된 세대를 확인할 수 없습니다. " +
                    "선순위 임차인이 존재할 경우 보증금 우선변제 순위가 밀릴 수 있으므로, " +
                    "잔금 지급 전 반드시 전입세대확인서를 발급받아 다른 세대 전입 여부를 직접 확인하세요.",
                steps = listOf(
                    "매물 주소지 관할 동주민센터 또는 구청 방문",
                    "신분증과 임대차계약서(사본 포함) 지참",
                    "전입세대확인서 발급 신청 (임대차 목적 명시)",
                    "발급 문서에서 다른 세대 전입 여부 확인",
                    "다른 세대가 있으면 잔금 보류 후 중개사·전문가와 상담",
                ),
                officialUrl = "https://www.gov.kr",
            ),
            AdvisoryTemplate(
                checkType = CheckType.UNPAID_NATIONAL_TAX_INQUIRY,
                title = "임대인 미납국세 열람",
                badgeText = "직접 확인 필요",
                severity = RiskSeverity.HIGH,
                guideText = "임대인이 국세·지방세를 체납한 경우, 국가는 임차인의 확정일자 우선변제권보다 앞서 체납세액을 징수할 수 있습니다. " +
                    "계약서 작성 후 잔금 지급 전 홈택스를 통해 임대인의 미납국세를 열람하여 숨겨진 징수 위험을 파악하세요.",
                steps = listOf(
                    "홈택스(hometax.go.kr) 접속 후 '미납국세 등 열람' 메뉴 선택",
                    "임대차계약서 사본 첨부 후 열람 신청 (임대인 동의 없이 신청 가능)",
                    "신청 다음 영업일 이후 가까운 세무서를 직접 방문하여 열람",
                    "체납 사실 확인 시 임대인에게 납부 또는 전세보증보험 가입 요구",
                    "체납이 해소되지 않으면 계약 해제 또는 법률 전문가 상담 진행",
                ),
                officialUrl = "https://www.hometax.go.kr",
            ),
            AdvisoryTemplate(
                checkType = CheckType.WAGE_CLAIM_PRIORITY,
                title = "임금채권 우선변제 위험",
                badgeText = "전문가 상담 권장",
                severity = RiskSeverity.MEDIUM,
                guideText = "임대인이 사업체를 운영하는 경우, 근로자의 최종 3개월치 임금·퇴직금·재해보상금(임금채권)은 " +
                    "임차인의 확정일자 우선변제권보다 법적으로 앞서 변제됩니다. " +
                    "경매가 발생하면 임금채권이 먼저 배당되어 보증금 회수 금액이 줄어들 수 있습니다. " +
                    "임대인이 사업자이거나 법인 소유 매물이라면 전문가 상담을 권장합니다.",
                steps = listOf(
                    "임대인이 사업자(개인·법인)인지 사업자등록 여부 확인",
                    "등기부등본 갑구에서 임금채권 관련 가압류·압류 등기 존재 여부 확인",
                    "HUG 전세보증보험 가입으로 회수 리스크 헷지 검토",
                    "위험이 의심되면 법률구조공단(☎ 132) 또는 법무사·변호사 상담",
                    "주택도시보증공사(HUG) 콜센터(☎ 1566-9009) 문의",
                ),
                officialUrl = "https://www.hug.go.kr",
                expertConsult = true,
            ),
        )
    }
}
