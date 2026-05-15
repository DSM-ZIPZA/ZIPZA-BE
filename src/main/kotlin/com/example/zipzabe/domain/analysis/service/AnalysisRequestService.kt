package com.example.zipzabe.domain.analysis.service

import com.example.zipzabe.domain.analysis.dto.AnalysisRequestCreateRequest
import com.example.zipzabe.domain.analysis.dto.AnalysisRequestResponse
import com.example.zipzabe.domain.analysis.dto.AnalysisRequestSummaryResponse
import com.example.zipzabe.domain.analysis.dto.AnalysisResultSummary
import com.example.zipzabe.domain.analysis.dto.CountedResultAvailability
import com.example.zipzabe.domain.analysis.dto.DocumentCollectionSummary
import com.example.zipzabe.domain.analysis.dto.ResultAvailability
import com.example.zipzabe.domain.analysis.entity.AnalysisRequest
import com.example.zipzabe.domain.analysis.repository.AnalysisRequestRepository
import com.example.zipzabe.domain.analysis.repository.BuildingAnalysisRepository
import com.example.zipzabe.domain.analysis.repository.FraudPatternAnalysisRepository
import com.example.zipzabe.domain.analysis.repository.GuaranteeAnalysisRepository
import com.example.zipzabe.domain.analysis.repository.PriceAnalysisRepository
import com.example.zipzabe.domain.analysis.repository.RecoveryAnalysisRepository
import com.example.zipzabe.domain.analysis.repository.RightsAnalysisRepository
import com.example.zipzabe.domain.building.repository.BuildingLedgerRepository
import com.example.zipzabe.domain.property.entity.Property
import com.example.zipzabe.domain.property.repository.PropertyRepository
import com.example.zipzabe.domain.registry.repository.RegistryRawRepository
import com.example.zipzabe.domain.report.repository.DiagnosisReportRepository
import com.example.zipzabe.domain.trade.repository.TradeRecordRepository
import com.example.zipzabe.domain.user.facade.UserFacade
import com.example.zipzabe.global.error.exception.AnalysisRequestNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class AnalysisRequestService(
    private val userFacade: UserFacade,
    private val propertyRepository: PropertyRepository,
    private val analysisRequestRepository: AnalysisRequestRepository,
    private val buildingLedgerRepository: BuildingLedgerRepository,
    private val registryRawRepository: RegistryRawRepository,
    private val tradeRecordRepository: TradeRecordRepository,
    private val priceAnalysisRepository: PriceAnalysisRepository,
    private val buildingAnalysisRepository: BuildingAnalysisRepository,
    private val rightsAnalysisRepository: RightsAnalysisRepository,
    private val guaranteeAnalysisRepository: GuaranteeAnalysisRepository,
    private val recoveryAnalysisRepository: RecoveryAnalysisRepository,
    private val fraudPatternAnalysisRepository: FraudPatternAnalysisRepository,
    private val diagnosisReportRepository: DiagnosisReportRepository,
) {

    @Transactional
    fun create(request: AnalysisRequestCreateRequest): AnalysisRequestResponse {
        val user = userFacade.getCurrentUser()
        val property = propertyRepository.save(
            Property(
                roadAddress = request.property.roadAddress,
                jibunAddress = request.property.jibunAddress,
                detailAddress = request.property.detailAddress,
                buildingManagementNumber = request.property.buildingManagementNumber,
                postalCode = request.property.postalCode,
                administrativeCode = request.property.administrativeCode,
                city = request.property.city,
                district = request.property.district,
                neighborhood = request.property.neighborhood,
                buildingName = request.property.buildingName,
                isApartment = request.property.isApartment,
                longitude = request.property.longitude,
                latitude = request.property.latitude,
            ),
        )
        val analysisRequest = analysisRequestRepository.save(
            AnalysisRequest(
                user = user,
                property = property,
                contractType = request.contractType,
                depositAmount = request.depositAmount,
                monthlyRent = request.monthlyRent,
                floor = request.floor,
                exclusiveArea = request.exclusiveArea,
                contractDate = request.contractDate,
                balanceDate = request.balanceDate,
                expiryDate = request.expiryDate,
            ),
        )

        return AnalysisRequestResponse.from(analysisRequest)
    }

    @Transactional(readOnly = true)
    fun getAll(): List<AnalysisRequestResponse> {
        val user = userFacade.getCurrentUser()
        return analysisRequestRepository.findByUserOrderByRequestedAtDesc(user)
            .map(AnalysisRequestResponse::from)
    }

    @Transactional(readOnly = true)
    fun get(requestId: UUID): AnalysisRequestResponse =
        AnalysisRequestResponse.from(findOwnedRequest(requestId))

    @Transactional(readOnly = true)
    fun getSummary(requestId: UUID): AnalysisRequestSummaryResponse {
        val request = findOwnedRequest(requestId)
        val buildingLedger = buildingLedgerRepository.findTopByPropertyOrderByFetchedAtDesc(request.property)
        val registryRaw = registryRawRepository.findTopByRequestOrderByFetchedAtDesc(request)
        val rentTradeCount = tradeRecordRepository.countByProperty(request.property)
        val priceAnalysis = priceAnalysisRepository.findTopByRequestOrderByAnalyzedAtDesc(request)
        val buildingAnalysis = buildingAnalysisRepository.findTopByRequestOrderByAnalyzedAtDesc(request)
        val rightsAnalysis = rightsAnalysisRepository.findTopByRequestOrderByAnalyzedAtDesc(request)
        val guaranteeAnalysis = guaranteeAnalysisRepository.findTopByRequestOrderByAnalyzedAtDesc(request)
        val recoveryAnalysis = recoveryAnalysisRepository.findTopByRequestOrderByAnalyzedAtDesc(request)
        val fraudPatternAnalysis = fraudPatternAnalysisRepository.findTopByRequestOrderByAnalyzedAtDesc(request)
        val diagnosisReport = diagnosisReportRepository.findTopByRequestOrderByCreatedAtDesc(request)

        return AnalysisRequestSummaryResponse(
            request = AnalysisRequestResponse.from(request),
            documents = DocumentCollectionSummary(
                buildingLedger = ResultAvailability(
                    completed = buildingLedger != null,
                    id = buildingLedger?.id,
                    updatedAt = buildingLedger?.fetchedAt,
                ),
                registryOcr = ResultAvailability(
                    completed = registryRaw != null,
                    id = registryRaw?.id,
                    updatedAt = registryRaw?.fetchedAt,
                ),
                rentTrades = CountedResultAvailability(
                    completed = rentTradeCount > 0,
                    count = rentTradeCount,
                ),
            ),
            analyses = AnalysisResultSummary(
                priceAnalysis = ResultAvailability(
                    completed = priceAnalysis != null,
                    id = priceAnalysis?.id,
                    updatedAt = priceAnalysis?.analyzedAt,
                ),
                publicLedgerSummary = ResultAvailability(
                    completed = buildingAnalysis != null && rightsAnalysis != null,
                    id = buildingAnalysis?.id ?: rightsAnalysis?.id,
                    updatedAt = listOfNotNull(buildingAnalysis?.analyzedAt, rightsAnalysis?.analyzedAt).maxOrNull(),
                ),
                guaranteeAnalysis = ResultAvailability(
                    completed = guaranteeAnalysis != null,
                    id = guaranteeAnalysis?.id,
                    updatedAt = guaranteeAnalysis?.analyzedAt,
                ),
                recoveryAnalysis = ResultAvailability(
                    completed = recoveryAnalysis != null,
                    id = recoveryAnalysis?.id,
                    updatedAt = recoveryAnalysis?.analyzedAt,
                ),
                fraudPatternAnalysis = ResultAvailability(
                    completed = fraudPatternAnalysis != null,
                    id = fraudPatternAnalysis?.id,
                    updatedAt = fraudPatternAnalysis?.analyzedAt,
                ),
            ),
            diagnosisReport = ResultAvailability(
                completed = diagnosisReport != null,
                id = diagnosisReport?.id,
                updatedAt = diagnosisReport?.createdAt,
            ),
            readyToStartAnalysis = buildingLedger != null && registryRaw != null,
        )
    }

    private fun findOwnedRequest(requestId: UUID): AnalysisRequest {
        val user = userFacade.getCurrentUser()
        return analysisRequestRepository.findByIdAndUser(requestId, user)
            ?: throw AnalysisRequestNotFoundException()
    }
}
