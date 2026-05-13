package com.example.zipzabe.domain.registry.service

import org.apache.pdfbox.Loader
import org.apache.pdfbox.rendering.ImageType
import org.apache.pdfbox.rendering.PDFRenderer
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

@Component
class RegistryPdfRenderer(
    @Value("\${google.vision.render-dpi:300}")
    private val renderDpi: Int,
    @Value("\${google.vision.max-pages:20}")
    private val maxPages: Int,
) {

    fun render(pdfBytes: ByteArray): RenderedPdf {
        Loader.loadPDF(pdfBytes).use { document ->
            val pageCount = document.numberOfPages
            require(pageCount > 0) { "PDF has no pages." }
            require(pageCount <= maxPages) { "PDF page count exceeds max-pages: $maxPages." }

            val renderer = PDFRenderer(document)
            val pages = (0 until pageCount).map { pageIndex ->
                val image = renderer.renderImageWithDPI(pageIndex, renderDpi.toFloat(), ImageType.RGB)
                ByteArrayOutputStream().use { output ->
                    ImageIO.write(image, "png", output)
                    output.toByteArray()
                }
            }

            return RenderedPdf(pageCount = pageCount, pageImages = pages)
        }
    }
}

data class RenderedPdf(
    val pageCount: Int,
    val pageImages: List<ByteArray>,
)
