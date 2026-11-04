package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.model.DocPage
import com.example.model.PdfPageSize
import com.example.model.PdfProject
import com.example.model.PdfQuality
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStream

object PdfEngine {

    suspend fun generatePdf(
        context: Context,
        project: PdfProject,
        onProgress: (current: Int, total: Int) -> Unit = { _, _ -> }
    ): Pair<String, Long> = withContext(Dispatchers.IO) {
        val pdfDoc = PdfDocument()
        val projectsDir = File(context.filesDir, "generated_pdfs").apply { if (!exists()) mkdirs() }
        val sanitizedTitle = project.title.replace(Regex("[^a-zA-Z0-9_.-]"), "_")
        val outputFile = File(projectsDir, "${sanitizedTitle}_${System.currentTimeMillis()}.pdf")

        try {
            val totalPages = project.pages.size
            for ((index, page) in project.pages.withIndex()) {
                onProgress(index + 1, totalPages)

                val rawBitmap = ImageFilterUtil.loadBitmap(page.originalImagePath, project.quality.maxDimension)
                if (rawBitmap != null) {
                    val processedBitmap = ImageFilterUtil.applyFilter(
                        source = rawBitmap,
                        filter = page.filterType,
                        brightness = page.brightness,
                        contrast = page.contrast,
                        rotationDegrees = page.rotationDegrees
                    )

                    val pageW: Int
                    val pageH: Int
                    if (project.pageSize == PdfPageSize.AUTO) {
                        pageW = processedBitmap.width
                        pageH = processedBitmap.height
                    } else {
                        pageW = project.pageSize.widthPoints
                        pageH = project.pageSize.heightPoints
                    }

                    val pageInfo = PdfDocument.PageInfo.Builder(pageW, pageH, index + 1).create()
                    val pdfPage = pdfDoc.startPage(pageInfo)
                    val canvas: Canvas = pdfPage.canvas

                    // Paint background white
                    val bgPaint = Paint().apply { color = Color.WHITE }
                    canvas.drawRect(0f, 0f, pageW.toFloat(), pageH.toFloat(), bgPaint)

                    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

                    if (project.pageSize == PdfPageSize.AUTO) {
                        canvas.drawBitmap(processedBitmap, 0f, 0f, paint)
                    } else {
                        // Calculate best fit maintaining aspect ratio with 16pt margin
                        val margin = 20f
                        val availableW = pageW - (margin * 2)
                        val availableH = pageH - (margin * 2)

                        val imgW = processedBitmap.width.toFloat()
                        val imgH = processedBitmap.height.toFloat()

                        val scale = minOf(availableW / imgW, availableH / imgH)
                        val drawW = imgW * scale
                        val drawH = imgH * scale

                        val left = margin + (availableW - drawW) / 2f
                        val top = margin + (availableH - drawH) / 2f

                        val destRect = Rect(left.toInt(), top.toInt(), (left + drawW).toInt(), (top + drawH).toInt())
                        canvas.drawBitmap(processedBitmap, null, destRect, paint)
                    }

                    pdfDoc.finishPage(pdfPage)

                    if (processedBitmap !== rawBitmap) {
                        processedBitmap.recycle()
                    }
                    rawBitmap.recycle()
                }
            }

            FileOutputStream(outputFile).use { out ->
                pdfDoc.writeTo(out)
            }
        } finally {
            pdfDoc.close()
        }

        Pair(outputFile.absolutePath, outputFile.length())
    }

    /**
     * Downloads/Exports the PDF file to the public Downloads directory so the user can easily find it.
     */
    suspend fun exportPdfToDownloads(context: Context, project: PdfProject): Result<String> = withContext(Dispatchers.IO) {
        try {
            val sourcePath = project.pdfFilePath ?: return@withContext Result.failure(Exception("PDF file not generated yet"))
            val sourceFile = File(sourcePath)
            if (!sourceFile.exists()) return@withContext Result.failure(Exception("Source PDF file does not exist"))

            val fileName = "${project.title.replace(Regex("[^a-zA-Z0-9_.-]"), "_")}.pdf"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/FmPDF")
                }

                val uri: Uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    ?: return@withContext Result.failure(Exception("Failed to create download entry in MediaStore"))

                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    FileInputStream(sourceFile).use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                Result.success("Saved to Downloads/FmPDF/$fileName")
            } else {
                @Suppress("DEPRECATION")
                val downloadsDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "FmPDF")
                if (!downloadsDir.exists()) downloadsDir.mkdirs()
                val targetFile = File(downloadsDir, fileName)

                FileInputStream(sourceFile).use { inputStream ->
                    FileOutputStream(targetFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                Result.success("Saved to ${targetFile.absolutePath}")
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Shares the PDF file with external apps using Android FileProvider.
     */
    fun sharePdf(context: Context, project: PdfProject): Boolean {
        val path = project.pdfFilePath ?: return false
        val file = File(path)
        if (!file.exists()) return false

        return try {
            val authority = "${context.packageName}.fileprovider"
            val uri: Uri = FileProvider.getUriForFile(context, authority, file)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, project.title)
                putExtra(Intent.EXTRA_TEXT, "Here is your PDF document created with FmPDF: ${project.title}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Share PDF: ${project.title}")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Renders pages of a PDF file to Bitmaps for viewing in the in-app viewer.
     */
    suspend fun renderPdfPages(pdfPath: String, maxPages: Int = 50): List<Bitmap> = withContext(Dispatchers.IO) {
        val file = File(pdfPath)
        if (!file.exists()) return@withContext emptyList()

        val bitmaps = mutableListOf<Bitmap>()
        try {
            val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)

            val count = minOf(renderer.pageCount, maxPages)
            for (i in 0 until count) {
                val page = renderer.openPage(i)
                // Render at 2x density for crystal clear viewing
                val scale = 2
                val width = page.width * scale
                val height = page.height * scale
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE)

                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                bitmaps.add(bitmap)
            }

            renderer.close()
            pfd.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        bitmaps
    }
}
