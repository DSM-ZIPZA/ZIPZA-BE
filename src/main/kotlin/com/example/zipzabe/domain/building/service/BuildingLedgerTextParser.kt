package com.example.zipzabe.domain.building.service

import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class BuildingLedgerTextParser {

    fun parse(text: String): ParsedBuildingLedger {
        val normalized = normalize(text)
        return ParsedBuildingLedger(
            mainPurposeCode = findMainPurposeCode(normalized),
            mainPurposeName = findMainPurposeName(normalized),
            totalFloorArea = findArea(normalized, "연면적"),
            buildingArea = findArea(normalized, "건축면적"),
            buildingCoverageRatio = findRatio(normalized, "건폐율"),
            floorAreaRatio = findRatio(normalized, "용적률"),
            structureName = findStructureName(normalized),
            floorsAboveGround = findFloorsAboveGround(normalized),
            floorsUnderground = findFloorsUnderground(normalized),
            householdCount = findHouseholdCount(normalized),
            approvalDate = findApprovalDate(normalized),
            isEarthquakeResistant = findEarthquakeResistant(normalized),
            exclusiveArea = findExclusiveArea(normalized),
            isViolationBuilding = findViolationBuilding(normalized),
            violationReason = findViolationReason(normalized),
            violationDetail = findViolationDetail(normalized),
        )
    }

    private fun normalize(text: String): String =
        text.replace("\r\n", "\n")
            .replace('\r', '\n')
            .replace(Regex("[\\t]+"), " ")

    private fun findMainPurposeCode(text: String): String =
        Regex("주용도\\s*코드\\s*[:：]?\\s*([0-9]+)")
            .find(text)?.groupValues?.get(1)?.trim()
            ?: Regex("용도코드\\s*[:：]?\\s*([0-9]+)")
                .find(text)?.groupValues?.get(1)?.trim()
            ?: "UNKNOWN"

    private fun findMainPurposeName(text: String): String =
        Regex("주용도\\s*[:：]?\\s*([가-힣A-Za-z0-9\\s()]{2,30})")
            .find(text)?.groupValues?.get(1)?.trim()?.take(100)
            ?: Regex("용도\\s*[:：]?\\s*([가-힣A-Za-z0-9\\s()]{2,30})")
                .find(text)?.groupValues?.get(1)?.trim()?.take(100)
            ?: "UNKNOWN"

    private fun findArea(text: String, keyword: String): Double? =
        Regex("$keyword[^\\n]{0,40}?([0-9]+(?:[,.][0-9]+)?)\\s*(?:㎡|m2|m²)")
            .find(text)?.groupValues?.get(1)
            ?.replace(",", ".")
            ?.toDoubleOrNull()

    private fun findExclusiveArea(text: String): Double? =
        findArea(text, "전유부분") ?: findArea(text, "전유")

    private fun findRatio(text: String, keyword: String): Double =
        Regex("$keyword\\s*[:：]?\\s*([0-9]+(?:[,.][0-9]+)?)")
            .find(text)?.groupValues?.get(1)
            ?.replace(",", ".")
            ?.toDoubleOrNull()
            ?: 0.0

    private fun findStructureName(text: String): String =
        Regex("(?:주구조|구조)\\s*[:：]?\\s*([가-힣\\s()]{2,50}(?:구조|조|식))")
            .find(text)?.groupValues?.get(1)?.trim()?.take(100)
            ?: "UNKNOWN"

    private fun findFloorsAboveGround(text: String): Int =
        Regex("지상\\s*([0-9]+)\\s*층")
            .find(text)?.groupValues?.get(1)?.toIntOrNull()
            ?: Regex("지상층수\\s*[:：]?\\s*([0-9]+)")
                .find(text)?.groupValues?.get(1)?.toIntOrNull()
            ?: 0

    private fun findFloorsUnderground(text: String): Int =
        Regex("지하\\s*([0-9]+)\\s*층")
            .find(text)?.groupValues?.get(1)?.toIntOrNull()
            ?: Regex("지하층수\\s*[:：]?\\s*([0-9]+)")
                .find(text)?.groupValues?.get(1)?.toIntOrNull()
            ?: 0

    private fun findHouseholdCount(text: String): Int =
        Regex("(?:세대수|가구수)\\s*[:：]?\\s*([0-9]+)")
            .find(text)?.groupValues?.get(1)?.toIntOrNull()
            ?: Regex("세대\\s*[:：]?\\s*([0-9]+)\\s*(?:세대|가구)?")
                .find(text)?.groupValues?.get(1)?.toIntOrNull()
            ?: 0

    private fun findApprovalDate(text: String): LocalDate? {
        val match = Regex("사용승인일?\\s*[:：]?\\s*(\\d{4})[.년\\-](\\d{1,2})[.월\\-](\\d{1,2})")
            .find(text) ?: return null
        val year = match.groupValues[1].toIntOrNull() ?: return null
        val month = match.groupValues[2].toIntOrNull() ?: return null
        val day = match.groupValues[3].toIntOrNull() ?: return null
        return runCatching { LocalDate.of(year, month, day) }.getOrNull()
    }

    private fun findEarthquakeResistant(text: String): Boolean =
        Regex("내진설계\\s*적용여부\\s*[:：]?\\s*([가-힣]+)")
            .find(text)?.groupValues?.get(1)
            ?.let { it.contains("적용") && !it.contains("미적용") }
            ?: false

    private fun findViolationBuilding(text: String): Boolean =
        Regex("위반건축물\\s*[:：]?\\s*([가-힣]+)")
            .find(text)?.groupValues?.get(1)
            ?.let { it.contains("해당") && !it.contains("미해당") }
            ?: false

    private fun findViolationReason(text: String): String? =
        Regex("위반\\s*사유\\s*[:：]?\\s*([^\\n]+)")
            .find(text)?.groupValues?.get(1)?.trim()?.take(200)

    private fun findViolationDetail(text: String): String? =
        Regex("위반\\s*내용\\s*[:：]?\\s*([^\\n]+)")
            .find(text)?.groupValues?.get(1)?.trim()?.take(1000)
}

data class ParsedBuildingLedger(
    val mainPurposeCode: String,
    val mainPurposeName: String,
    val totalFloorArea: Double?,
    val buildingArea: Double?,
    val buildingCoverageRatio: Double,
    val floorAreaRatio: Double,
    val structureName: String,
    val floorsAboveGround: Int,
    val floorsUnderground: Int,
    val householdCount: Int,
    val approvalDate: LocalDate?,
    val isEarthquakeResistant: Boolean,
    val exclusiveArea: Double?,
    val isViolationBuilding: Boolean,
    val violationReason: String?,
    val violationDetail: String?,
)
