package com.example.zipzabe.domain.registry.service

import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class RegistryTextParser {

    fun parse(text: String, fallbackAddress: String, fallbackBuildingName: String?): ParsedRegistry {
        val normalizedText = normalize(text)
        val lines = normalizedText.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val titleSection = sectionBefore(normalizedText, "갑구")
        val ownershipSection = sectionBetween(normalizedText, "갑구", "을구")
        val mortgageSection = sectionAfter(normalizedText, "을구")

        val uniqueNumber = findUniqueNumber(normalizedText)
        val title = parseTitle(titleSection, normalizedText, fallbackAddress, fallbackBuildingName)
        val ownershipEntries = parseOwnerships(ownershipSection)
        val mortgageEntries = parseMortgages(mortgageSection)
        val restrictionEntries = parseRestrictions(ownershipSection, mortgageSection)

        return ParsedRegistry(
            uniqueNumber = uniqueNumber,
            title = title,
            ownerships = ownershipEntries,
            restrictions = restrictionEntries,
            mortgages = mortgageEntries,
            rawLineCount = lines.size,
        )
    }

    private fun normalize(text: String): String =
        text.replace("\r\n", "\n")
            .replace('\r', '\n')
            .replace(Regex("[\\t ]+"), " ")

    private fun sectionBefore(text: String, marker: String): String {
        val index = text.indexOf(marker)
        return if (index >= 0) text.substring(0, index) else text
    }

    private fun sectionBetween(text: String, start: String, end: String): String {
        val startIndex = text.indexOf(start)
        if (startIndex < 0) {
            return ""
        }
        val endIndex = text.indexOf(end, startIndex + start.length)
        return if (endIndex > startIndex) text.substring(startIndex, endIndex) else text.substring(startIndex)
    }

    private fun sectionAfter(text: String, marker: String): String {
        val index = text.indexOf(marker)
        return if (index >= 0) text.substring(index) else ""
    }

    private fun findUniqueNumber(text: String): String =
        Regex("(?:고유번호|부동산고유번호)\\s*[:：]?\\s*([0-9\\-]{8,30})")
            .find(text)
            ?.groupValues
            ?.get(1)
            ?.trim()
            ?: Regex("\\b\\d{4}-\\d{4}-\\d{6}\\b").find(text)?.value
            ?: "UNKNOWN-${text.hashCode().toUInt().toString(16)}"

    private fun parseTitle(
        titleSection: String,
        wholeText: String,
        fallbackAddress: String,
        fallbackBuildingName: String?,
    ): ParsedTitle {
        val titleText = if (titleSection.isBlank()) wholeText else titleSection
        val realEstateType = when {
            titleText.contains("집합건물") -> "집합건물"
            titleText.contains("토지") -> "토지"
            titleText.contains("건물") -> "건물"
            else -> "집합건물"
        }
        val locationAddress = findLabeledValue(titleText, "소재지번", "소재지", "도로명주소")
            ?: titleText.lines().firstOrNull { it.contains("동") && (it.contains("번") || it.contains("로")) }?.trim()
            ?: fallbackAddress
        val buildingName = findLabeledValue(titleText, "건물의 명칭", "건물명칭", "건물명")
            ?: fallbackBuildingName
        val floorInfo = Regex("(제?\\s*\\d+\\s*층|\\d+\\s*호|[지하상]?\\d+층)")
            .find(titleText)
            ?.value
            ?.replace(" ", "")
        val exclusiveArea = findArea(titleText, "전유")
        val commonArea = findArea(titleText, "공용")
        val purpose = findLabeledValue(titleText, "용도", "건물내역")?.take(100)
        val landRightType = findLabeledValue(titleText, "대지권의 종류", "대지권종류")?.take(50)
        val landRightRatio = Regex("(?:대지권비율|대지권의 비율)\\s*[:：]?\\s*([^\\n]+)")
            .find(titleText)
            ?.groupValues
            ?.get(1)
            ?.trim()
            ?.take(100)

        return ParsedTitle(
            realEstateType = realEstateType,
            locationAddress = locationAddress,
            buildingName = buildingName?.take(100),
            floorInfo = floorInfo?.take(50),
            exclusiveArea = exclusiveArea,
            commonArea = commonArea,
            purpose = purpose,
            landRightType = landRightType,
            landRightRatio = landRightRatio,
        )
    }

    private fun findLabeledValue(text: String, vararg labels: String): String? {
        labels.forEach { label ->
            Regex("$label\\s*[:：]?\\s*([^\\n]+)")
                .find(text)
                ?.groupValues
                ?.get(1)
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?.let { return it }
        }
        return null
    }

    private fun findArea(text: String, keyword: String): Double? =
        Regex("$keyword[^\\n]{0,40}?([0-9]+(?:[,.][0-9]+)?)\\s*(?:㎡|m2|m²)")
            .find(text)
            ?.groupValues
            ?.get(1)
            ?.replace(",", ".")
            ?.toDoubleOrNull()

    private fun parseOwnerships(section: String): List<ParsedOwnership> {
        val entries = splitRankEntries(section)
            .mapNotNull { entry ->
                val purpose = findPurpose(entry, ownershipPurposes) ?: return@mapNotNull null
                if (isRestrictionPurpose(purpose)) {
                    return@mapNotNull null
                }
                ParsedOwnership(
                    rankNumber = findRank(entry),
                    registrationPurpose = purpose,
                    receptionDate = findDate(entry),
                    registrationCause = findCause(entry),
                    registrationCauseDate = findCauseDate(entry),
                    ownerName = findPersonName(entry, "소유자", "공유자") ?: "미상",
                    ownerIdMasked = findMaskedId(entry),
                    shareRatio = findShareRatio(entry),
                    isCurrent = false,
                )
            }
            .toMutableList()

        val currentIndex = entries.indexOfLast {
            !isErased(it.registrationPurpose) && it.registrationPurpose.contains("소유권")
        }
        return entries.mapIndexed { index, ownership ->
            ownership.copy(isCurrent = index == currentIndex)
        }
    }

    private fun parseRestrictions(ownershipSection: String, mortgageSection: String): List<ParsedRestriction> {
        val ownershipRestrictions = splitRankEntries(ownershipSection).mapNotNull { entry ->
            val purpose = findPurpose(entry, restrictionPurposes) ?: return@mapNotNull null
            ParsedRestriction(
                rankNumber = findRank(entry),
                registrationPurpose = purpose,
                receptionDate = findDate(entry),
                registrationCause = findCause(entry),
                rightHolderName = findPersonName(entry, "권리자", "채권자", "수탁자"),
                detail = entry.take(1000),
                isErased = isErased(entry),
                eraseDate = findEraseDate(entry),
            )
        }
        val mortgageRestrictions = splitRankEntries(mortgageSection).mapNotNull { entry ->
            val purpose = findPurpose(entry, restrictionPurposes) ?: return@mapNotNull null
            ParsedRestriction(
                rankNumber = findRank(entry),
                registrationPurpose = purpose,
                receptionDate = findDate(entry),
                registrationCause = findCause(entry),
                rightHolderName = findPersonName(entry, "권리자", "채권자"),
                detail = entry.take(1000),
                isErased = isErased(entry),
                eraseDate = findEraseDate(entry),
            )
        }
        return ownershipRestrictions + mortgageRestrictions
    }

    private fun parseMortgages(section: String): List<ParsedMortgage> =
        splitRankEntries(section).mapNotNull { entry ->
            val purpose = findPurpose(entry, mortgagePurposes) ?: return@mapNotNull null
            ParsedMortgage(
                rankNumber = findRank(entry),
                registrationPurpose = purpose,
                receptionDate = findDate(entry),
                registrationCause = findCause(entry),
                claimAmount = findAmountManwon(entry),
                debtorName = findPersonName(entry, "채무자"),
                creditorName = findPersonName(entry, "근저당권자", "전세권자", "임차권자", "채권자"),
                isErased = isErased(entry),
                eraseDate = findEraseDate(entry),
            )
        }

    private fun splitRankEntries(section: String): List<String> {
        if (section.isBlank()) {
            return emptyList()
        }

        val entries = mutableListOf<StringBuilder>()
        section.lines().map { it.trim() }.filter { it.isNotBlank() }.forEach { line ->
            if (Regex("^\\d{1,3}(?:\\s|[.)-])").containsMatchIn(line) || entries.isEmpty()) {
                entries += StringBuilder(line)
            } else {
                entries.last().append('\n').append(line)
            }
        }
        return entries.map { it.toString() }
    }

    private fun findRank(entry: String): Int =
        Regex("^\\s*(\\d{1,3})").find(entry)?.groupValues?.get(1)?.toIntOrNull() ?: 0

    private fun findPurpose(entry: String, candidates: List<String>): String? =
        candidates.firstOrNull { entry.contains(it) }

    private fun findDate(entry: String): LocalDate? =
        dateRegex.find(entry)?.toLocalDate()

    private fun findCauseDate(entry: String): LocalDate? =
        Regex("(?:등기원인|원인)[^\\n]{0,30}?${dateRegex.pattern}")
            .find(entry)
            ?.let { dateRegex.find(it.value)?.toLocalDate() }

    private fun findEraseDate(entry: String): LocalDate? =
        Regex("말소[^\\n]{0,30}?${dateRegex.pattern}")
            .find(entry)
            ?.let { dateRegex.find(it.value)?.toLocalDate() }

    private fun MatchResult.toLocalDate(): LocalDate? {
        val year = groupValues[1].toIntOrNull() ?: return null
        val month = groupValues[2].toIntOrNull() ?: return null
        val day = groupValues[3].toIntOrNull() ?: return null
        return runCatching { LocalDate.of(year, month, day) }.getOrNull()
    }

    private fun findCause(entry: String): String? =
        Regex("(?:등기원인|원인)\\s*[:：]?\\s*([^\\n]+)")
            .find(entry)
            ?.groupValues
            ?.get(1)
            ?.trim()
            ?.take(100)

    private fun findPersonName(entry: String, vararg labels: String): String? {
        labels.forEach { label ->
            Regex("$label\\s*[:：]?\\s*([가-힣A-Za-z0-9().·\\s]{2,40})")
                .find(entry)
                ?.groupValues
                ?.get(1)
                ?.trim()
                ?.split(Regex("\\s{2,}|\\n"))
                ?.firstOrNull()
                ?.takeIf { it.isNotBlank() }
                ?.let { return it.take(100) }
        }
        return null
    }

    private fun findMaskedId(entry: String): String? =
        Regex("\\d{6}\\s*-\\s*[0-9*]{7}")
            .find(entry)
            ?.value
            ?.replace(" ", "")
            ?.take(50)

    private fun findShareRatio(entry: String): String? =
        Regex("(?:지분|공유지분)\\s*[:：]?\\s*([0-9]+\\s*/\\s*[0-9]+)")
            .find(entry)
            ?.groupValues
            ?.get(1)
            ?.replace(" ", "")
            ?.take(50)

    private fun findAmountManwon(entry: String): Long? {
        val amountText = Regex("금\\s*([0-9,]+)\\s*원").find(entry)?.groupValues?.get(1)
            ?: Regex("(?:채권최고액|전세금|보증금)\\s*[:：]?\\s*([0-9,]+)")
                .find(entry)
                ?.groupValues
                ?.get(1)
        val won = amountText?.replace(",", "")?.toLongOrNull() ?: return null
        return won / 10_000L
    }

    private fun isRestrictionPurpose(purpose: String): Boolean =
        restrictionPurposes.any { purpose.contains(it) }

    private fun isErased(text: String): Boolean =
        text.contains("말소") || text.contains("해지") || text.contains("취소")

    companion object {
        private val dateRegex = Regex("(\\d{4})\\s*(?:년|[.\\-/])\\s*(\\d{1,2})\\s*(?:월|[.\\-/])\\s*(\\d{1,2})")
        private val ownershipPurposes = listOf("소유권보존", "소유권이전", "공유자전원지분전부이전", "지분이전")
        private val restrictionPurposes = listOf("신탁", "압류", "가압류", "가처분", "경매개시결정", "강제경매", "임의경매", "예고등기")
        private val mortgagePurposes = listOf("근저당권설정", "저당권설정", "전세권설정", "임차권등기", "임차권설정")
    }
}

data class ParsedRegistry(
    val uniqueNumber: String,
    val title: ParsedTitle,
    val ownerships: List<ParsedOwnership>,
    val restrictions: List<ParsedRestriction>,
    val mortgages: List<ParsedMortgage>,
    val rawLineCount: Int,
)

data class ParsedTitle(
    val realEstateType: String,
    val locationAddress: String,
    val buildingName: String?,
    val floorInfo: String?,
    val exclusiveArea: Double?,
    val commonArea: Double?,
    val purpose: String?,
    val landRightType: String?,
    val landRightRatio: String?,
)

data class ParsedOwnership(
    val rankNumber: Int,
    val registrationPurpose: String,
    val receptionDate: LocalDate?,
    val registrationCause: String?,
    val registrationCauseDate: LocalDate?,
    val ownerName: String,
    val ownerIdMasked: String?,
    val shareRatio: String?,
    val isCurrent: Boolean,
)

data class ParsedRestriction(
    val rankNumber: Int,
    val registrationPurpose: String,
    val receptionDate: LocalDate?,
    val registrationCause: String?,
    val rightHolderName: String?,
    val detail: String?,
    val isErased: Boolean,
    val eraseDate: LocalDate?,
)

data class ParsedMortgage(
    val rankNumber: Int,
    val registrationPurpose: String,
    val receptionDate: LocalDate?,
    val registrationCause: String?,
    val claimAmount: Long?,
    val debtorName: String?,
    val creditorName: String?,
    val isErased: Boolean,
    val eraseDate: LocalDate?,
)
