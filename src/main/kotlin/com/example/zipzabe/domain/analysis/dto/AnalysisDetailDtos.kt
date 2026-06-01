package com.example.zipzabe.domain.analysis.dto

import com.example.zipzabe.domain.property.dto.PropertyListingResponse
import com.example.zipzabe.domain.report.dto.NextActionResponse
import com.example.zipzabe.domain.report.dto.RiskItemResponse
import java.time.LocalDate
import java.util.UUID

data class AnalysisDetailResponse(
    val requestId: UUID,
    val property: PropertyListingResponse,
    val buildingInfo: BuildingInfoResponse,
    val priceHistory: List<PricePointResponse>,
    val registrationSections: List<RegistrationSectionResponse>,
    val buildingLandAnalysis: BuildingLandAnalysisDetailResponse,
    val overallAnalysis: OverallAnalysisDetailResponse,
    val nextActions: List<NextActionResponse>,
    val aiSummary: String?,
)

data class BuildingInfoResponse(
    val name: String,
    val address: String,
    val buildingManagementNumber: String,
    val floor: Int,
    val totalFloors: Int?,
    val exclusiveAreaM2: Double,
    val estimatedPropertyValueManwon: Long?,
)

data class PricePointResponse(
    val date: String,
    val open: Long,
    val high: Long,
    val low: Long,
    val close: Long,
    val volume: Int,
)

data class RegistrationSectionResponse(
    val title: String,
    val records: List<RegistrationRecordResponse>,
)

data class RegistrationRecordResponse(
    val rank: Int,
    val purpose: String,
    val registrationDate: LocalDate?,
    val registrationCause: String?,
    val rightsAndNotes: String,
)

data class BuildingLandAnalysisDetailResponse(
    val usage: String?,
    val dongHo: String,
    val illegalBuilding: String,
    val warnings: List<String>,
)

data class OverallAnalysisDetailResponse(
    val totalRiskScore: Int?,
    val priceScore: Int?,
    val registrationScore: Int?,
    val buildingLandScore: Int?,
    val contractScore: Int?,
    val confidenceScore: Int?,
    val warningMessages: List<String>,
    val topRisks: List<RiskItemResponse>,
)
