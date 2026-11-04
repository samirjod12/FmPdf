package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.example.model.DocPage
import com.example.model.FilterType
import com.example.model.PdfPageSize
import com.example.model.PdfProject
import com.example.model.PdfQuality
import com.example.util.ImageFilterUtil
import com.example.util.PdfEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class ProjectRepository(private val context: Context) {

    private val _projects = MutableStateFlow<List<PdfProject>>(emptyList())
    val projects: StateFlow<List<PdfProject>> = _projects.asStateFlow()

    private val metaFile = File(context.filesDir, "fmpdf_projects.json")

    init {
        CoroutineScope(Dispatchers.IO).launch {
            loadProjects()
            if (_projects.value.isEmpty()) {
                seedInitialProject()
            }
        }
    }

    private suspend fun loadProjects() = withContext(Dispatchers.IO) {
        if (!metaFile.exists()) {
            _projects.value = emptyList()
            return@withContext
        }

        try {
            val jsonString = metaFile.readText()
            val jsonArray = JSONArray(jsonString)
            val list = mutableListOf<PdfProject>()

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val id = obj.getString("id")
                val title = obj.getString("title")
                val createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                val modifiedAt = obj.optLong("modifiedAt", System.currentTimeMillis())
                val pdfPath = if (obj.has("pdfFilePath") && !obj.isNull("pdfFilePath")) obj.getString("pdfFilePath") else null
                val thumbPath = if (obj.has("thumbnailPath") && !obj.isNull("thumbnailPath")) obj.getString("thumbnailPath") else null
                val fileSize = obj.optLong("fileSizeBytes", 0L)
                val pageSize = try { PdfPageSize.valueOf(obj.optString("pageSize", "A4")) } catch (e: Exception) { PdfPageSize.A4 }
                val quality = try { PdfQuality.valueOf(obj.optString("quality", "HIGH")) } catch (e: Exception) { PdfQuality.HIGH }

                val pagesArray = obj.optJSONArray("pages") ?: JSONArray()
                val pages = mutableListOf<DocPage>()
                for (j in 0 until pagesArray.length()) {
                    val pObj = pagesArray.getJSONObject(j)
                    val pId = pObj.optString("id", UUID.randomUUID().toString())
                    val pPath = pObj.getString("originalImagePath")
                    val pFilter = try { FilterType.valueOf(pObj.optString("filterType", "ORIGINAL")) } catch (e: Exception) { FilterType.ORIGINAL }
                    val pRotation = pObj.optInt("rotationDegrees", 0)
                    val pBrightness = pObj.optDouble("brightness", 0.0).toFloat()
                    val pContrast = pObj.optDouble("contrast", 1.0).toFloat()
                    val pNum = pObj.optInt("pageNumber", j + 1)
                    pages.add(DocPage(pId, pPath, pFilter, pRotation, pBrightness, pContrast, pNum))
                }

                list.add(
                    PdfProject(
                        id = id,
                        title = title,
                        createdAt = createdAt,
                        modifiedAt = modifiedAt,
                        pages = pages,
                        pdfFilePath = pdfPath,
                        thumbnailPath = thumbPath,
                        fileSizeBytes = fileSize,
                        pageSize = pageSize,
                        quality = quality
                    )
                )
            }
            // Sort by modifiedAt descending
            _projects.value = list.sortedByDescending { it.modifiedAt }
        } catch (e: Exception) {
            e.printStackTrace()
            _projects.value = emptyList()
        }
    }

    private suspend fun saveProjects() = withContext(Dispatchers.IO) {
        try {
            val jsonArray = JSONArray()
            for (p in _projects.value) {
                val obj = JSONObject()
                obj.put("id", p.id)
                obj.put("title", p.title)
                obj.put("createdAt", p.createdAt)
                obj.put("modifiedAt", p.modifiedAt)
                obj.put("pdfFilePath", p.pdfFilePath)
                obj.put("thumbnailPath", p.thumbnailPath)
                obj.put("fileSizeBytes", p.fileSizeBytes)
                obj.put("pageSize", p.pageSize.name)
                obj.put("quality", p.quality.name)

                val pagesArray = JSONArray()
                for (page in p.pages) {
                    val pObj = JSONObject()
                    pObj.put("id", page.id)
                    pObj.put("originalImagePath", page.originalImagePath)
                    pObj.put("filterType", page.filterType.name)
                    pObj.put("rotationDegrees", page.rotationDegrees)
                    pObj.put("brightness", page.brightness.toDouble())
                    pObj.put("contrast", page.contrast.toDouble())
                    pObj.put("pageNumber", page.pageNumber)
                    pagesArray.put(pObj)
                }
                obj.put("pages", pagesArray)
                jsonArray.put(obj)
            }
            metaFile.writeText(jsonArray.toString(2))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun saveProject(project: PdfProject): PdfProject = withContext(Dispatchers.IO) {
        val updatedList = _projects.value.toMutableList()
        val index = updatedList.indexOfFirst { it.id == project.id }
        if (index >= 0) {
            updatedList[index] = project.copy(modifiedAt = System.currentTimeMillis())
        } else {
            updatedList.add(0, project)
        }
        _projects.value = updatedList.sortedByDescending { it.modifiedAt }
        saveProjects()
        project
    }

    suspend fun deleteProject(projectId: String) = withContext(Dispatchers.IO) {
        val project = _projects.value.find { it.id == projectId }
        if (project != null) {
            project.pdfFilePath?.let { File(it).delete() }
            project.thumbnailPath?.let { File(it).delete() }
            for (p in project.pages) {
                File(p.originalImagePath).delete()
            }
            _projects.value = _projects.value.filterNot { it.id == projectId }
            saveProjects()
        }
    }

    suspend fun duplicateProject(projectId: String) = withContext(Dispatchers.IO) {
        val existing = _projects.value.find { it.id == projectId } ?: return@withContext
        val newProject = existing.copy(
            id = UUID.randomUUID().toString(),
            title = "${existing.title} (Copy)",
            createdAt = System.currentTimeMillis(),
            modifiedAt = System.currentTimeMillis()
        )
        saveProject(newProject)
    }

    suspend fun renameProject(projectId: String, newTitle: String) = withContext(Dispatchers.IO) {
        val existing = _projects.value.find { it.id == projectId } ?: return@withContext
        saveProject(existing.copy(title = newTitle))
    }

    fun getProjectById(id: String): PdfProject? {
        return _projects.value.find { it.id == id }
    }

    private suspend fun seedInitialProject() = withContext(Dispatchers.IO) {
        val pagesDir = File(context.filesDir, "scanned_pages").apply { if (!exists()) mkdirs() }

        // Generate sample page 1: Welcome & Overview
        val page1Bitmap = Bitmap.createBitmap(800, 1130, Bitmap.Config.ARGB_8888)
        val canvas1 = Canvas(page1Bitmap)
        canvas1.drawColor(Color.WHITE)

        val headerPaint = Paint().apply {
            color = Color.parseColor("#E53935")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas1.drawRect(0f, 0f, 800f, 180f, headerPaint)

        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = 46f
            isAntiAlias = true
            isFakeBoldText = true
        }
        canvas1.drawText("FmPDF Premium Document", 50f, 110f, textPaint)

        val subTextPaint = Paint().apply {
            color = Color.parseColor("#0F172A")
            textSize = 28f
            isAntiAlias = true
        }
        canvas1.drawText("Welcome to your ultimate mobile PDF studio.", 50f, 250f, subTextPaint)
        canvas1.drawText("Features:", 50f, 320f, Paint(subTextPaint).apply { isFakeBoldText = true; textSize = 32f })
        canvas1.drawText("• Snap and scan documents with direct Camera", 70f, 380f, subTextPaint)
        canvas1.drawText("• Apply Magic Color, B&W Clean, and Grayscale filters", 70f, 430f, subTextPaint)
        canvas1.drawText("• Download to device Downloads folder with one tap", 70f, 480f, subTextPaint)
        canvas1.drawText("• Share instantly via Email, WhatsApp, or Drive", 70f, 530f, subTextPaint)
        canvas1.drawText("• Pure White Professional UI & Android 11 Easter Egg", 70f, 580f, subTextPaint)

        val cardPaint = Paint().apply {
            color = Color.parseColor("#F1F5F9")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val cardRect = RectF(50f, 660f, 750f, 950f)
        canvas1.drawRoundRect(cardRect, 20f, 20f, cardPaint)

        val noteTitlePaint = Paint().apply {
            color = Color.parseColor("#E53935")
            textSize = 26f
            isFakeBoldText = true
            isAntiAlias = true
        }
        canvas1.drawText("Creator & Version Information", 80f, 720f, noteTitlePaint)
        canvas1.drawText("App Name: FmPDF", 80f, 780f, subTextPaint)
        canvas1.drawText("Version: 1.0 (Tap in Settings for Android 11 Easter Egg)", 80f, 830f, subTextPaint)
        canvas1.drawText("Made By: Samir Bin Aziz", 80f, 880f, Paint(subTextPaint).apply { color = Color.parseColor("#0F172A"); isFakeBoldText = true })

        val footerPaint = Paint().apply {
            color = Color.parseColor("#94A3B8")
            textSize = 20f
            isAntiAlias = true
        }
        canvas1.drawText("Page 1 of 2 — Generated with FmPDF", 240f, 1080f, footerPaint)

        val page1File = File(pagesDir, "sample_page_1.jpg")
        FileOutputStream(page1File).use { page1Bitmap.compress(Bitmap.CompressFormat.JPEG, 92, it) }

        // Generate sample page 2: Quick Tips
        val page2Bitmap = Bitmap.createBitmap(800, 1130, Bitmap.Config.ARGB_8888)
        val canvas2 = Canvas(page2Bitmap)
        canvas2.drawColor(Color.WHITE)

        val header2Paint = Paint().apply {
            color = Color.parseColor("#0F172A")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas2.drawRect(0f, 0f, 800f, 180f, header2Paint)

        val title2Paint = Paint().apply {
            color = Color.WHITE
            textSize = 44f
            isAntiAlias = true
            isFakeBoldText = true
        }
        canvas2.drawText("Document Scanning Tips", 50f, 110f, title2Paint)

        val bodyPaint = Paint().apply {
            color = Color.parseColor("#334155")
            textSize = 26f
            isAntiAlias = true
        }
        canvas2.drawText("1. Good Lighting: Ensure even light without shadows.", 50f, 270f, bodyPaint)
        canvas2.drawText("2. Magic Color: Use for faded receipts & invoices.", 50f, 340f, bodyPaint)
        canvas2.drawText("3. B&W Clean: Best for book pages & signed contracts.", 50f, 410f, bodyPaint)
        canvas2.drawText("4. Reorder Pages: Drag pages into any order before export.", 50f, 480f, bodyPaint)
        canvas2.drawText("5. Exporting: PDFs are saved in standard A4 format.", 50f, 550f, bodyPaint)

        canvas2.drawText("Page 2 of 2 — Generated with FmPDF", 240f, 1080f, footerPaint)

        val page2File = File(pagesDir, "sample_page_2.jpg")
        FileOutputStream(page2File).use { page2Bitmap.compress(Bitmap.CompressFormat.JPEG, 92, it) }

        val pages = listOf(
            DocPage(
                id = UUID.randomUUID().toString(),
                originalImagePath = page1File.absolutePath,
                filterType = FilterType.ORIGINAL,
                pageNumber = 1
            ),
            DocPage(
                id = UUID.randomUUID().toString(),
                originalImagePath = page2File.absolutePath,
                filterType = FilterType.MAGIC_COLOR,
                pageNumber = 2
            )
        )

        var sampleProject = PdfProject(
            id = "sample_project_1",
            title = "FmPDF User Guide",
            createdAt = System.currentTimeMillis() - 3600000,
            modifiedAt = System.currentTimeMillis(),
            pages = pages,
            pageSize = PdfPageSize.A4,
            quality = PdfQuality.HIGH
        )

        // Generate the PDF
        val (pdfPath, size) = PdfEngine.generatePdf(context, sampleProject)
        val thumbPath = ImageFilterUtil.createThumbnail(context, page1File.absolutePath, FilterType.ORIGINAL, 0)

        sampleProject = sampleProject.copy(
            pdfFilePath = pdfPath,
            thumbnailPath = thumbPath,
            fileSizeBytes = size
        )

        _projects.value = listOf(sampleProject)
        saveProjects()

        page1Bitmap.recycle()
        page2Bitmap.recycle()
    }
}
