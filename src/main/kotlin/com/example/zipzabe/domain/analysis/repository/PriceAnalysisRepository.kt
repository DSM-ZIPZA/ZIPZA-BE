package com.example.zipzabe.domain.analysis.repository

import com.example.zipzabe.domain.analysis.entity.AnalysisRequest
import com.example.zipzabe.domain.analysis.entity.PriceAnalysis
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface PriceAnalysisRepository : JpaRepository<PriceAnalysis, UUID> {
    fun findTopByRequestOrderByAnalyzedAtDesc(request: AnalysisRequest): PriceAnalysis?
}
