package com.example.zipzabe.domain.report.dto

import com.example.zipzabe.domain.report.entity.Verdict
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class DiagnosisSupplementRequest(
    val specialTerms: SpecialTermsInput = SpecialTermsInput(),
    val landlordRisk: LandlordRiskInput = LandlordRiskInput(),
    val occupancy: OccupancyInput = OccupancyInput(),
)

data class SpecialTermsInput(
    val mortgageCancellationRequired: Boolean = false,
    val noAdditionalLienBeforeMoveIn: Boolean = false,
    val trustConsentRequired: Boolean = false,
    val taxArrearsDisclosureRequired: Boolean = false,
    val vacantDeliveryRequired: Boolean = false,
    val rawSpecialTerms: String? = null,
)

data class LandlordRiskInput(
    val landlordOwnedHouseCount: Int? = null,
    val landlordGuaranteeCount: Int? = null,
    val knownGuaranteeAccident: Boolean = false,
    val taxArrearsKnown: Boolean = false,
    val simultaneousSaleAndLease: Boolean = false,
)

data class OccupancyInput(
    val declaredVacant: Boolean = false,
    val currentOccupantExists: Boolean = false,
    val moveInAvailableDate: LocalDate? = null,
    val tenantMoveInDate: LocalDate? = null,
    val otherHouseholdOnCertificate: Boolean = false,
)

data class DiagnosisReportResponse(
    val requestId: UUID,
    val priceScore: Int,
    val rightsScore: Int,
    val buildingScore: Int,
    val contractScore: Int,
    val confidenceScore: Int,
    val totalScore: Int,
    val verdict: Verdict,
    val topRisks: List<RiskItemResponse>,
    val nextActions: List<NextActionResponse>,
    val createdAt: LocalDateTime,
)

data class RiskItemResponse(
    val title: String,
    val detail: String,
    val severity: RiskSeverity,
)

data class NextActionResponse(
    val action: String,
    val priority: ActionPriority,
)

enum class RiskSeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL,
}

enum class ActionPriority {
    LOW,
    MEDIUM,
    HIGH,
}
