package com.babysplit.app.core.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object ReceiptCompressor {

    /**
     * Compresses an image URI into a local WebP/JPEG file (<400KB) and returns the local absolute path.
     */
    suspend fun compressAndSaveReceipt(context: Context, imageUri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(imageUri) ?: return@withContext null
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            // Scale down if width/height exceeds 1920px
            val maxDimension = 1920
            val width = originalBitmap.width
            val height = originalBitmap.height
            val scale = if (width > maxDimension || height > maxDimension) {
                val max = kotlin.math.max(width, height)
                maxDimension.toFloat() / max
            } else 1.0f

            val scaledBitmap = if (scale < 1.0f) {
                Bitmap.createScaledBitmap(
                    originalBitmap,
                    (width * scale).toInt(),
                    (height * scale).toInt(),
                    true
                )
            } else originalBitmap

            val receiptsDir = File(context.filesDir, "receipts").apply { if (!exists()) mkdirs() }
            val outputFile = File(receiptsDir, "receipt_${UUID.randomUUID()}.jpg")

            val outputStream = FileOutputStream(outputFile)
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            outputStream.flush()
            outputStream.close()

            outputFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
