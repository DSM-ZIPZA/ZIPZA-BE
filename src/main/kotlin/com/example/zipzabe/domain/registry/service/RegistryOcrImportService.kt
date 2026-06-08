package com.example.zipzabe.domain.registry.service

import com.example.zipzabe.domain.analysis.repository.AnalysisRequestRepository
import com.example.zipzabe.domain.registry.dto.RegistryApickOcrRequest
import com.example.zipzabe.domain.registry.dto.RegistryOcrResponse
import com.example.zipzabe.domain.registry.entity.ParseStatus
import com.example.zipzabe.domain.registry.entity.RegistryCandidate
import com.example.zipzabe.domain.registry.entity.RegistryMortgage
import com.example.zipzabe.domain.registry.entity.RegistryOwnership
import com.example.zipzabe.domain.registry.entity.RegistryRaw
import com.example.zipzabe.domain.registry.entity.RegistryRestriction
import com.example.zipzabe.domain.registry.entity.RegistryTitle
import com.example.zipzabe.domain.registry.repository.RegistryCandidateRepository
import com.example.zipzabe.domain.registry.repository.RegistryMortgageRepository
import com.example.zipzabe.domain.registry.repository.RegistryOwnershipRepository
import com.example.zipzabe.domain.registry.repository.RegistryRawRepository
import com.example.zipzabe.domain.registry.repository.RegistryRestrictionRepository
import com.example.zipzabe.domain.registry.repository.RegistryTitleRepository
import com.example.zipzabe.global.error.exception.AnalysisRequestNotFoundException
import com.example.zipzabe.global.error.exception.ExternalApiBadRequestException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.security.MessageDigest
import java.util.UUID

@Service
class RegistryOcrImportService(
    private val analysisRequestRepository: AnalysisRequestRepository,
    private val registryCandidateRepository: RegistryCandidateRepository,
    private val registryRawRepository: RegistryRawRepository,
    private val registryTitleRepository: RegistryTitleRepository,
    private val registryOwnershipRepository: RegistryOwnershipRepository,
    private val registryRestrictionRepository: RegistryRestrictionRepository,
    private val registryMortgageRepository: RegistryMortgageRepository,
    private val registryPdfRenderer: RegistryPdfRenderer,
    private val pdfTextExtractor: PdfTextExtractor,
    private val googleVisionOcrService: GoogleVisionOcrService,
    private val registryTextParser: RegistryTextParser,
    private val apickRegistryPdfService: ApickRegistryPdfService,
) {
    private val log = LoggerFactory.getLogger(RegistryOcrImportService::class.java)

    @Transactional
    fun importRegistryPdf(requestId: UUID, file: MultipartFile): RegistryOcrResponse {
        if (file.isEmpty || file.originalFilename?.endsWith(".pdf", ignoreCase = true) != true) {
            throw ExternalApiBadRequestException()
        }

        return importRegistryPdfBytes(requestId, file.bytes)
    }

    @Transactional
    fun importRegistryFromApick(
        requestId: UUID,
        request: RegistryApickOcrRequest,
    ): RegistryOcrResponse {
        val analysisRequest = analysisRequestRepository.findById(requestId)
            .orElseThrow { AnalysisRequestNotFoundException() }
        val pdfBytes = apickRegistryPdfService.issueAndDownloadPdf(analysisRequest, request)

        return importRegistryPdfBytes(requestId, pdfBytes)
    }

    private fun importRegistryPdfBytes(requestId: UUID, pdfBytes: ByteArray): RegistryOcrResponse {
        val request = analysisRequestRepository.findById(requestId)
            .orElseThrow { AnalysisRequestNotFoundException() }
        val sourceHash = sha256(pdfBytes)
        log.info(
            "Starting registry PDF import. requestId={} propertyId={} pdfBytes={} sourceHash={}",
            requestId,
            request.property.id,
            pdfBytes.size,
            sourceHash,
        )
        val directText = runCatching { pdfTextExtractor.extract(pdfBytes) }
            .getOrElse { e ->
                log.warn("Failed to extract registry PDF text. requestId={} pdfBytes={}", requestId, pdfBytes.size, e)
                ExtractedPdfText(text = "", pageCount = 0)
            }
        var pageCount = directText.pageCount
        val extractedText = if (directText.text.length >= MIN_DIRECT_TEXT_LENGTH) {
            log.info(
                "Registry PDF text extracted directly. requestId={} pageCount={} extractedTextLength={}",
                requestId,
                directText.pageCount,
                directText.text.length,
            )
            directText.text
        } else {
            val renderedPdf = runCatching { registryPdfRenderer.render(pdfBytes) }
                .getOrElse { e ->
                    log.warn("Failed to render registry PDF. requestId={} pdfBytes={}", requestId, pdfBytes.size, e)
                    throw ExternalApiBadRequestException()
                }
            log.info(
                "Rendered registry PDF. requestId={} pageCount={} imageCount={} imageBytes={}",
                requestId,
                renderedPdf.pageCount,
                renderedPdf.pageImages.size,
                renderedPdf.pageImages.sumOf { it.size },
            )
            pageCount = renderedPdf.pageCount
            googleVisionOcrService.extractText(renderedPdf.pageImages)
        }
        log.info(
            "Registry OCR completed. requestId={} extractedTextLength={}",
            requestId,
            extractedText.length,
        )
        val parsed = registryTextParser.parse(
            text = extractedText,
            fallbackAddress = request.property.jibunAddress,
            fallbackBuildingName = request.property.buildingName,
        )
        log.info(
            "Registry text parsed. requestId={} uniqueNumberPresent={} ownershipCount={} restrictionCount={} mortgageCount={}",
            requestId,
            parsed.uniqueNumber.isNotBlank(),
            parsed.ownerships.size,
            parsed.restrictions.size,
            parsed.mortgages.size,
        )

        val candidate = registryCandidateRepository.save(
            RegistryCandidate(
                property = request.property,
                uniqueNumber = parsed.uniqueNumber.take(30),
                realEstateType = parsed.title.realEstateType,
                location = parsed.title.locationAddress,
                isSelected = true,
            ),
        )
        val raw = registryRawRepository.save(
            RegistryRaw(
                registryCandidate = candidate,
                request = request,
                uniqueNumber = parsed.uniqueNumber.take(30),
                sourceHash = sourceHash,
                parseStatus = ParseStatus.SUCCESS,
            ),
        )

        registryTitleRepository.save(
            RegistryTitle(
                registryRaw = raw,
                realEstateType = parsed.title.realEstateType,
                locationAddress = parsed.title.locationAddress,
                buildingName = parsed.title.buildingName,
                floorInfo = parsed.title.floorInfo,
                exclusiveArea = parsed.title.exclusiveArea,
                commonArea = parsed.title.commonArea,
                purpose = parsed.title.purpose,
                landRightType = parsed.title.landRightType,
                landRightRatio = parsed.title.landRightRatio,
            ),
        )
        registryOwnershipRepository.saveAll(
            parsed.ownerships.map {
                RegistryOwnership(
                    registryRaw = raw,
                    rankNumber = it.rankNumber,
                    registrationPurpose = it.registrationPurpose,
                    receptionDate = it.receptionDate,
                    registrationCause = it.registrationCause,
                    registrationCauseDate = it.registrationCauseDate,
                    ownerName = it.ownerName,
                    ownerIdMasked = it.ownerIdMasked,
                    shareRatio = it.shareRatio,
                    isCurrent = it.isCurrent,
                )
            },
        )
        registryRestrictionRepository.saveAll(
            parsed.restrictions.map {
                RegistryRestriction(
                    registryRaw = raw,
                    rankNumber = it.rankNumber,
                    registrationPurpose = it.registrationPurpose,
                    receptionDate = it.receptionDate,
                    registrationCause = it.registrationCause,
                    rightHolderName = it.rightHolderName,
                    detail = it.detail,
                    isErased = it.isErased,
                    eraseDate = it.eraseDate,
                )
            },
        )
        registryMortgageRepository.saveAll(
            parsed.mortgages.map {
                RegistryMortgage(
                    registryRaw = raw,
                    rankNumber = it.rankNumber,
                    registrationPurpose = it.registrationPurpose,
                    receptionDate = it.receptionDate,
                    registrationCause = it.registrationCause,
                    claimAmount = it.claimAmount,
                    debtorName = it.debtorName,
                    creditorName = it.creditorName,
                    isErased = it.isErased,
                    eraseDate = it.eraseDate,
                )
            },
        )

        log.info(
            "Registry import saved. requestId={} registryRawId={} registryCandidateId={}",
            requestId,
            raw.id,
            candidate.id,
        )

        return RegistryOcrResponse(
            registryRawId = raw.id!!,
            uniqueNumber = raw.uniqueNumber,
            sourceHash = raw.sourceHash,
            pageCount = pageCount,
            extractedTextLength = extractedText.length,
            titleCount = 1,
            ownershipCount = parsed.ownerships.size,
            restrictionCount = parsed.restrictions.size,
            mortgageCount = parsed.mortgages.size,
        )
    }

    private fun sha256(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val MIN_DIRECT_TEXT_LENGTH = 100
    }
}
