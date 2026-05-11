package com.example.zipzabe.domain.report.repository

import com.example.zipzabe.domain.analysis.entity.AnalysisRequest
import com.example.zipzabe.domain.report.entity.DiagnosisReport
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface DiagnosisReportRepository : JpaRepository<DiagnosisReport, UUID> {
    fun findTopByRequestOrderByCreatedAtDesc(request: AnalysisRequest): DiagnosisReport?
}
