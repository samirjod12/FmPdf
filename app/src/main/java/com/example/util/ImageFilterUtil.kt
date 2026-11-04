package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import com.example.model.FilterType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object ImageFilterUtil {

    suspend fun saveImageToFile(context: Context, bitmap: Bitmap, quality: Int = 90): String = withContext(Dispatchers.IO) {
        val imagesDir = File(context.filesDir, "scanned_pages").apply { if (!exists()) mkdirs() }
        val file = File(imagesDir, "page_${UUID.randomUUID()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
        }
        file.absolutePath
    }

    suspend fun loadBitmap(filePath: String, maxDimension: Int = 2048): Bitmap? = withContext(Dispatchers.IO) {
        val file = File(filePath)
        if (!file.exists()) return@withContext null

        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(filePath, options)

        var sampleSize = 1
        val maxSide = maxOf(options.outWidth, options.outHeight)
        while (maxSide / sampleSize > maxDimension) {
            sampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        BitmapFactory.decodeFile(filePath, decodeOptions)
    }

    fun applyFilter(
        source: Bitmap,
        filter: FilterType,
        brightness: Float = 0f,
        contrast: Float = 1f,
        rotationDegrees: Int = 0
    ): Bitmap {
        var baseBitmap = source
        if (rotationDegrees % 360 != 0) {
            val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
            baseBitmap = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        }

        val result = Bitmap.createBitmap(baseBitmap.width, baseBitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        val cm = ColorMatrix()

        when (filter) {
            FilterType.ORIGINAL -> {
                // Identity
            }
            FilterType.MAGIC_COLOR -> {
                // Boost contrast and whiten background (CamScanner magic color style)
                val magicCm = ColorMatrix()
                val scale = 1.35f
                val translate = -35f + (brightness * 80f)
                magicCm.set(floatArrayOf(
                    scale, 0f, 0f, 0f, translate + 18f,
                    0f, scale, 0f, 0f, translate + 18f,
                    0f, 0f, scale, 0f, translate + 18f,
                    0f, 0f, 0f, 1f, 0f
                ))
                val satCm = ColorMatrix().apply { setSaturation(1.15f) }
                magicCm.postConcat(satCm)
                cm.postConcat(magicCm)
            }
            FilterType.BW_CLEAN -> {
                // High contrast black & white document thresholding
                val bwCm = ColorMatrix()
                bwCm.setSaturation(0f)
                val cScale = 2.0f
                val cTranslate = -120f + (brightness * 60f)
                val highContrast = ColorMatrix(floatArrayOf(
                    cScale, 0f, 0f, 0f, cTranslate,
                    0f, cScale, 0f, 0f, cTranslate,
                    0f, 0f, cScale, 0f, cTranslate,
                    0f, 0f, 0f, 1f, 0f
                ))
                bwCm.postConcat(highContrast)
                cm.postConcat(bwCm)
            }
            FilterType.GRAYSCALE -> {
                cm.setSaturation(0f)
            }
            FilterType.WARM -> {
                val warmCm = ColorMatrix(floatArrayOf(
                    1.12f, 0f, 0f, 0f, 10f,
                    0f, 1.05f, 0f, 0f, 5f,
                    0f, 0f, 0.90f, 0f, -10f,
                    0f, 0f, 0f, 1f, 0f
                ))
                cm.postConcat(warmCm)
            }
            FilterType.VIBRANT -> {
                val sat = ColorMatrix().apply { setSaturation(1.4f) }
                val con = ColorMatrix(floatArrayOf(
                    1.15f, 0f, 0f, 0f, -10f,
                    0f, 1.15f, 0f, 0f, -10f,
                    0f, 0f, 1.15f, 0f, -10f,
                    0f, 0f, 0f, 1f, 0f
                ))
                sat.postConcat(con)
                cm.postConcat(sat)
            }
            FilterType.INVERT -> {
                val invertCm = ColorMatrix(floatArrayOf(
                    -1f, 0f, 0f, 0f, 255f,
                    0f, -1f, 0f, 0f, 255f,
                    0f, 0f, -1f, 0f, 255f,
                    0f, 0f, 0f, 1f, 0f
                ))
                cm.postConcat(invertCm)
            }
        }

        // Apply custom brightness/contrast if specified
        if (contrast != 1f || brightness != 0f) {
            val userCm = ColorMatrix(floatArrayOf(
                contrast, 0f, 0f, 0f, brightness * 100f,
                0f, contrast, 0f, 0f, brightness * 100f,
                0f, 0f, contrast, 0f, brightness * 100f,
                0f, 0f, 0f, 1f, 0f
            ))
            cm.postConcat(userCm)
        }

        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(baseBitmap, 0f, 0f, paint)

        if (baseBitmap !== source) {
            baseBitmap.recycle()
        }

        return result
    }

    suspend fun createThumbnail(context: Context, imagePath: String, filter: FilterType, rotation: Int): String? = withContext(Dispatchers.IO) {
        val original = loadBitmap(imagePath, maxDimension = 600) ?: return@withContext null
        val filtered = applyFilter(original, filter, rotationDegrees = rotation)

        val thumbDir = File(context.filesDir, "thumbnails").apply { if (!exists()) mkdirs() }
        val thumbFile = File(thumbDir, "thumb_${UUID.randomUUID()}.jpg")

        FileOutputStream(thumbFile).use { out ->
            filtered.compress(Bitmap.CompressFormat.JPEG, 80, out)
        }

        if (filtered !== original) filtered.recycle()
        original.recycle()

        thumbFile.absolutePath
    }
}
