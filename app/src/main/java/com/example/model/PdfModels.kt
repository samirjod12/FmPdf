package com.example.model

import java.util.UUID

enum class FilterType(val displayName: String) {
    ORIGINAL("Original"),
    MAGIC_COLOR("Magic Color"),
    BW_CLEAN("B&W Clean"),
    GRAYSCALE("Grayscale"),
    WARM("Warm Soft"),
    VIBRANT("Sharp Vivid"),
    INVERT("Inverted")
}

enum class PdfPageSize(val displayName: String, val widthPoints: Int, val heightPoints: Int) {
    A4("A4 (Standard)", 595, 842),
    LETTER("US Letter", 612, 792),
    AUTO("Fit Image Size", 0, 0)
}

enum class PdfQuality(val displayName: String, val compressionQuality: Int, val maxDimension: Int) {
    HIGH("High Quality (Print)", 90, 2400),
    MEDIUM("Standard (Balanced)", 75, 1600),
    LOW("Compact (Email friendly)", 55, 1000)
}

data class DocPage(
    val id: String = UUID.randomUUID().toString(),
    val originalImagePath: String,
    val filterType: FilterType = FilterType.ORIGINAL,
    val rotationDegrees: Int = 0,
    val brightness: Float = 0f,
    val contrast: Float = 1f,
    val pageNumber: Int = 1
)

data class PdfProject(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis(),
    val pages: List<DocPage> = emptyList(),
    val pdfFilePath: String? = null,
    val thumbnailPath: String? = null,
    val fileSizeBytes: Long = 0L,
    val pageSize: PdfPageSize = PdfPageSize.A4,
    val quality: PdfQuality = PdfQuality.HIGH
) {
    val pageCount: Int
        get() = pages.size

    val formattedSize: String
        get() = when {
            fileSizeBytes <= 0 -> "Calculating..."
            fileSizeBytes < 1024 -> "$fileSizeBytes B"
            fileSizeBytes < 1024 * 1024 -> String.format("%.1f KB", fileSizeBytes / 1024.0)
            else -> String.format("%.2f MB", fileSizeBytes / (1024.0 * 1024.0))
        }
}
