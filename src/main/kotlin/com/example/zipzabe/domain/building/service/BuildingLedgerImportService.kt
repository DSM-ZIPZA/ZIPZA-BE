package com.example.zipzabe.domain.building.service

import com.example.zipzabe.domain.analysis.repository.AnalysisRequestRepository
import com.example.zipzabe.domain.address.service.AddressService
import com.example.zipzabe.domain.building.dto.BuildingLedgerFetchRequest
import com.example.zipzabe.domain.building.dto.BuildingLedgerFetchResponse
import com.example.zipzabe.domain.building.dto.BuildingRegisterRequest
import com.example.zipzabe.domain.building.entity.BuildingLedger
import com.example.zipzabe.domain.building.repository.BuildingLedgerRepository
import com.example.zipzabe.domain.registry.service.GoogleVisionOcrService
import com.example.zipzabe.domain.registry.service.PdfTextExtractor
import com.example.zipzabe.domain.registry.service.RegistryPdfRenderer
import com.example.zipzabe.global.error.exception.AnalysisRequestNotFoundException
import com.example.zipzabe.global.error.exception.ExternalApiBadRequestException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Service
class BuildingLedgerImportService(
    private val analysisRequestRepository: AnalysisRequestRepository,
    private val buildingLedgerRepository: BuildingLedgerRepository,
    private val buildingService: BuildingService,
    private val addressService: AddressService,
    private val registryPdfRenderer: RegistryPdfRenderer,
    private val pdfTextExtractor: PdfTextExtractor,
    private val googleVisionOcrService: GoogleVisionOcrService,
    private val buildingLedgerTextParser: BuildingLedgerTextParser,
) {
    private val log = LoggerFactory.getLogger(BuildingLedgerImportService::class.java)

    @Transactional
    fun fetchAndSave(requestId: UUID, fetchRequest: BuildingLedgerFetchRequest): BuildingLedgerFetchResponse {
        val analysisRequest = analysisRequestRepository.findById(requestId)
            .orElseThrow { AnalysisRequestNotFoundException() }
        val property = analysisRequest.property
        val address = resolveJibunAddress(property.jibunAddress, property.roadAddress)
            .ifBlank { property.buildingName.orEmpty() }
            .trim()
        if (address.isBlank()) {
            throw ExternalApiBadRequestException()
        }

        val pdfBytes = buildingService.getBuildingRegisterPdf(
            BuildingRegisterRequest(
                address = address,
                bName = property.buildingName ?: "",
                dong = fetchRequest.dong,
                ho = fetchRequest.ho,
            )
        )

        val directText = runCatching { pdfTextExtractor.extract(pdfBytes) }
            .getOrElse {
                log.warn("Failed to extract building ledger PDF text. requestId={} pdfBytes={}", requestId, pdfBytes.size, it)
                null
            }
        val ocrText = if (directText != null && directText.text.length >= MIN_DIRECT_TEXT_LENGTH) {
            log.info(
                "Building ledger PDF text extracted directly. requestId={} pageCount={} extractedTextLength={}",
                requestId,
                directText.pageCount,
                directText.text.length,
            )
            directText.text
        } else {
            val renderedPdf = runCatching { registryPdfRenderer.render(pdfBytes) }
                .getOrElse { throw ExternalApiBadRequestException() }
            googleVisionOcrService.extractText(renderedPdf.pageImages)
        }
        val parsed = buildingLedgerTextParser.parse(ocrText)

        val ledger = buildingLedgerRepository.save(
            BuildingLedger(
                property = property,
                mainPurposeCode = parsed.mainPurposeCode,
                mainPurposeName = parsed.mainPurposeName,
                totalFloorArea = parsed.totalFloorArea ?: 0.0,
                buildingArea = parsed.buildingArea ?: 0.0,
                buildingCoverageRatio = parsed.buildingCoverageRatio,
                floorAreaRatio = parsed.floorAreaRatio,
                structureName = parsed.structureName,
                floorsAboveGround = parsed.floorsAboveGround,
                floorsUnderground = parsed.floorsUnderground,
                householdCount = parsed.householdCount,
                approvalDate = parsed.approvalDate ?: LocalDate.now(),
                isEarthquakeResistant = parsed.isEarthquakeResistant,
                exclusiveArea = parsed.exclusiveArea ?: 0.0,
                isViolationBuilding = parsed.isViolationBuilding,
                violationReason = parsed.violationReason,
                violationDetail = parsed.violationDetail,
            )
        )

        return BuildingLedgerFetchResponse(
            buildingLedgerId = requireNotNull(ledger.id),
            mainPurposeName = ledger.mainPurposeName,
            totalFloorArea = ledger.totalFloorArea,
            exclusiveArea = ledger.exclusiveArea,
            approvalDate = ledger.approvalDate,
            isViolationBuilding = ledger.isViolationBuilding,
        )
    }

    private fun resolveJibunAddress(jibunAddress: String, roadAddress: String): String {
        val storedJibun = jibunAddress.trim()
        val road = roadAddress.trim()
        if (storedJibun.isNotBlank() && normalizeAddress(storedJibun) != normalizeAddress(road)) {
            return storedJibun
        }
        return runCatching { addressService.resolve(road.ifBlank { storedJibun }).jibunAddress.trim() }
            .getOrNull()
            ?.takeIf { it.isNotBlank() && normalizeAddress(it) != normalizeAddress(road) }
            ?: storedJibun.ifBlank { road }
    }

    private fun normalizeAddress(value: String): String =
        value.replace("\\s+".toRegex(), "")

    companion object {
        private const val MIN_DIRECT_TEXT_LENGTH = 100
    }
}
