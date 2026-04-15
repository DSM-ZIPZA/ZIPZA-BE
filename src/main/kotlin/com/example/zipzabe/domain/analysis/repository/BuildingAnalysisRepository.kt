package com.example.zipzabe.domain.analysis.repository

import com.example.zipzabe.domain.analysis.entity.AnalysisRequest
import com.example.zipzabe.domain.analysis.entity.BuildingAnalysis
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface BuildingAnalysisRepository : JpaRepository<BuildingAnalysis, UUID> {
    fun findTopByRequestOrderByAnalyzedAtDesc(request: AnalysisRequest): BuildingAnalysis?
}
