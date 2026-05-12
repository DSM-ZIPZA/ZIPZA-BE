package com.example.zipzabe.domain.analysis.repository

import com.example.zipzabe.domain.analysis.entity.AnalysisRequest
import com.example.zipzabe.domain.analysis.entity.RightsAnalysis
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface RightsAnalysisRepository : JpaRepository<RightsAnalysis, UUID> {
    fun findTopByRequestOrderByAnalyzedAtDesc(request: AnalysisRequest): RightsAnalysis?
}
