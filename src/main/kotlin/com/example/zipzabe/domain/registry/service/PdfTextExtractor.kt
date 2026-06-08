package com.example.zipzabe.domain.registry.service

import org.apache.pdfbox.Loader
import org.apache.pdfbox.text.PDFTextStripper
import org.springframework.stereotype.Component

@Component
class PdfTextExtractor {

    fun extract(pdfBytes: ByteArray): ExtractedPdfText =
        Loader.loadPDF(pdfBytes).use { document ->
            val stripper = PDFTextStripper()
            stripper.sortByPosition = true

            ExtractedPdfText(
                text = stripper.getText(document).trim(),
                pageCount = document.numberOfPages,
            )
        }
}

data class ExtractedPdfText(
    val text: String,
    val pageCount: Int,
)
