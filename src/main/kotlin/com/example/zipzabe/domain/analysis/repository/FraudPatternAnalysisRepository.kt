package com.example.zipzabe.domain.analysis.repository

import com.example.zipzabe.domain.analysis.entity.AnalysisRequest
import com.example.zipzabe.domain.analysis.entity.FraudPatternAnalysis
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface FraudPatternAnalysisRepository : JpaRepository<FraudPatternAnalysis, UUID> {
    fun findTopByRequestOrderByAnalyzedAtDesc(request: AnalysisRequest): FraudPatternAnalysis?
}
