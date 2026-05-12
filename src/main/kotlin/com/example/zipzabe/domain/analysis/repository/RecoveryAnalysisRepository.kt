package com.example.zipzabe.domain.analysis.repository

import com.example.zipzabe.domain.analysis.entity.AnalysisRequest
import com.example.zipzabe.domain.analysis.entity.RecoveryAnalysis
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface RecoveryAnalysisRepository : JpaRepository<RecoveryAnalysis, UUID> {
    fun findTopByRequestOrderByAnalyzedAtDesc(request: AnalysisRequest): RecoveryAnalysis?
}
