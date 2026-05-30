package com.scrimslegends.app.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import timber.log.Timber
import java.io.ByteArrayOutputStream

/**
 * Utility for image processing and compression.
 */
object ImageUtils {

    /**
     * Compresses a byte array representing an image.
     * Scales it down to a maximum dimension and re-encodes as JPEG with quality reduction.
     *
     * @param bytes Original image bytes
     * @param maxDimension Maximum width or height (e.g. 1280)
     * @param quality JPEG quality (1-100)
     * @return Compressed image bytes
     */
    fun compressImage(bytes: ByteArray, maxDimension: Int = 1280, quality: Int = 80): ByteArray {
        try {
            // Decode with bounds only to check dimensions
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)

            // Calculate scale factor
            val srcWidth = options.outWidth
            val srcHeight = options.outHeight
            var inSampleSize = 1

            if (srcWidth > maxDimension || srcHeight > maxDimension) {
                val halfWidth = srcWidth / 2
                val halfHeight = srcHeight / 2
                while (halfWidth / inSampleSize >= maxDimension || halfHeight / inSampleSize >= maxDimension) {
                    inSampleSize *= 2
                }
            }

            // Decode with scaling
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = inSampleSize
            }
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions) ?: return bytes

            // Further scale to exact maxDimension if needed
            val scaledBitmap = if (bitmap.width > maxDimension || bitmap.height > maxDimension) {
                val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
                val targetWidth: Int
                val targetHeight: Int
                if (ratio > 1) {
                    targetWidth = maxDimension
                    targetHeight = (maxDimension / ratio).toInt()
                } else {
                    targetHeight = maxDimension
                    targetWidth = (maxDimension * ratio).toInt()
                }
                Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
            } else {
                bitmap
            }

            // Compress as JPEG
            val out = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            
            val result = out.toByteArray()
            
            // Cleanup
            if (scaledBitmap != bitmap) scaledBitmap.recycle()
            bitmap.recycle()
            
            return result
        } catch (e: Exception) {
            Timber.e(e, "Compression failed, returning original")
            return bytes
        }
    }
}
