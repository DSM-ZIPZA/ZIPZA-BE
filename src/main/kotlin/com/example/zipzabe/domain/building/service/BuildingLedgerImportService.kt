package com.example.zipzabe.domain.building.service

import com.example.zipzabe.domain.analysis.repository.AnalysisRequestRepository
import com.example.zipzabe.domain.building.dto.BuildingLedgerFetchRequest
import com.example.zipzabe.domain.building.dto.BuildingLedgerFetchResponse
import com.example.zipzabe.domain.building.dto.BuildingRegisterRequest
import com.example.zipzabe.domain.building.entity.BuildingLedger
import com.example.zipzabe.domain.building.repository.BuildingLedgerRepository
import com.example.zipzabe.domain.registry.service.GoogleVisionOcrService
import com.example.zipzabe.domain.registry.service.RegistryPdfRenderer
import com.example.zipzabe.global.error.exception.AnalysisRequestNotFoundException
import com.example.zipzabe.global.error.exception.ExternalApiBadRequestException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Service
class BuildingLedgerImportService(
    private val analysisRequestRepository: AnalysisRequestRepository,
    private val buildingLedgerRepository: BuildingLedgerRepository,
    private val buildingService: BuildingService,
    private val registryPdfRenderer: RegistryPdfRenderer,
    private val googleVisionOcrService: GoogleVisionOcrService,
    private val buildingLedgerTextParser: BuildingLedgerTextParser,
) {

    @Transactional
    fun fetchAndSave(requestId: UUID, fetchRequest: BuildingLedgerFetchRequest): BuildingLedgerFetchResponse {
        val analysisRequest = analysisRequestRepository.findById(requestId)
            .orElseThrow { AnalysisRequestNotFoundException() }
        val property = analysisRequest.property

        val pdfBytes = buildingService.getBuildingRegisterPdf(
            BuildingRegisterRequest(
                address = property.roadAddress,
                bName = property.buildingName ?: "",
                dong = fetchRequest.dong,
                ho = fetchRequest.ho,
            )
        )

        val renderedPdf = runCatching { registryPdfRenderer.render(pdfBytes) }
            .getOrElse { throw ExternalApiBadRequestException() }
        val ocrText = googleVisionOcrService.extractText(renderedPdf.pageImages)
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
}
