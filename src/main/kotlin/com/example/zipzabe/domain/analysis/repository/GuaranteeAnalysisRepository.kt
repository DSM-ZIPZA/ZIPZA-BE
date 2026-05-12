package com.example.zipzabe.domain.analysis.repository

import com.example.zipzabe.domain.analysis.entity.AnalysisRequest
import com.example.zipzabe.domain.analysis.entity.GuaranteeAnalysis
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface GuaranteeAnalysisRepository : JpaRepository<GuaranteeAnalysis, UUID> {
    fun findTopByRequestOrderByAnalyzedAtDesc(request: AnalysisRequest): GuaranteeAnalysis?
}
