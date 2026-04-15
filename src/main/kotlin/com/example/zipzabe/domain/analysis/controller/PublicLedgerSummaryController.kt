package com.example.zipzabe.domain.analysis.controller

import com.example.zipzabe.domain.analysis.dto.PublicLedgerSummaryResponse
import com.example.zipzabe.domain.analysis.service.PublicLedgerSummaryService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/analysis-requests/{requestId}/public-ledger-summary")
class PublicLedgerSummaryController(
    private val publicLedgerSummaryService: PublicLedgerSummaryService,
) {

    @PostMapping
    fun analyze(@PathVariable requestId: UUID): ResponseEntity<PublicLedgerSummaryResponse> {
        val response = publicLedgerSummaryService.analyze(requestId)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @GetMapping
    fun getSummary(@PathVariable requestId: UUID): PublicLedgerSummaryResponse =
        publicLedgerSummaryService.getSummary(requestId)
}
