package com.example.zipzabe.domain.analysis.dto

import java.time.LocalDate
import java.util.UUID

data class PublicLedgerSummaryResponse(
    val requestId: UUID,
    val matching: MatchingSummary,
    val violation: ViolationSummary,
    val registrySummary: RegistrySummary,
    val ownershipChange: OwnershipChangeSummary,
    val trustRegistration: TrustRegistrationSummary,
    val buildingComparison: BuildingComparisonSummary,
    val overallWarnings: List<String>,
) {
    data class MatchingSummary(
        val isAddressMatched: Boolean,
        val isAreaMatched: Boolean,
        val isFloorMatched: Boolean,
        val isUsageMatched: Boolean,
        val overallMatched: Boolean,
    )

    data class ViolationSummary(
        val hasViolation: Boolean,
        val violationType: String?,
        val warningMessage: String?,
    )

    data class RegistrySummary(
        val realEstateType: String,
        val locationAddress: String,
        val buildingName: String?,
        val currentOwners: List<CurrentOwnerSummary>,
        val mortgages: List<MortgageSummary>,
        val restrictions: List<RestrictionSummary>,
    )

    data class CurrentOwnerSummary(
        val ownerName: String,
        val ownerIdMasked: String?,
        val shareRatio: String?,
    )

    data class MortgageSummary(
        val rankNumber: Int,
        val registrationPurpose: String,
        val claimAmount: Long?,
        val creditorName: String?,
        val receptionDate: LocalDate?,
    )

    data class RestrictionSummary(
        val rankNumber: Int,
        val registrationPurpose: String,
        val rightHolderName: String?,
        val detail: String?,
        val receptionDate: LocalDate?,
    )

    data class OwnershipChangeSummary(
        val hasRecentOwnerChange: Boolean,
        val ownerChangeCount: Int,
        val latestOwnerChangeDate: LocalDate?,
        val warningMessage: String?,
    )

    data class TrustRegistrationSummary(
        val hasTrustRegistration: Boolean,
        val trustEntries: List<TrustEntrySummary>,
        val warningMessage: String?,
    )

    data class TrustEntrySummary(
        val rankNumber: Int,
        val registrationPurpose: String,
        val detail: String?,
        val receptionDate: LocalDate?,
    )

    data class BuildingComparisonSummary(
        val listingExclusiveArea: Double,
        val ledgerExclusiveArea: Double,
        val areaDifference: Double,
        val listingFloor: Int,
        val ledgerFloorInfo: String?,
        val listingPurpose: String?,
        val ledgerPurpose: String?,
    )
}
