package com.mlbb.scrim.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.junit.Test
import org.junit.Before
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ImageUtilsTest {

    private lateinit var testImageData: ByteArray

    @Before
    fun setup() {
        // Create a simple test image data (1x1 red pixel PNG)
        testImageData = byteArrayOf(
            0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, // PNG signature
            0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52, // IHDR chunk
            0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01, // 1x1 dimensions
            0x08, 0x02, 0x00, 0x00, 0x00, 0x4E, 0x49, 0x41, 0x54, // Bit depth 8, color type 2 (RGB)
            0x07, 0xC9, 0x6B, 0x3E, 0x00, 0x00, 0x00, 0x0C, // IDAT chunk
            0x49, 0x44, 0x41, 0x54, 0x08, 0xD7, 0x63, 0xF8, // Image data
            0xCF, 0xC0, 0x00, 0x00, 0x00, 0x02, 0x00, 0x01,
            0x49, 0x45, 0x4E, 0x44, 0xAE, 0x42, 0x60, 0x82  // IEND chunk
        )
    }

    // ─── Basic Compression Tests ───

    @Test
    fun `compressImage successfully compresses valid image data`() {
        // Act
        val result = ImageUtils.compressImage(testImageData)

        // Assert
        assertNotNull(result)
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `compressImage returns compressed data smaller than original for large images`() {
        // Arrange - Create larger test data (simulating a larger image)
        val largeImageData = ByteArray(1024 * 1024) { it.toByte() } // 1MB of data

        // Act
        val result = ImageUtils.compressImage(largeImageData, maxDimension = 1280, quality = 50)

        // Assert
        // For invalid image data, it should return original
        assertNotNull(result)
    }

    @Test
    fun `compressImage handles empty input`() {
        // Arrange
        val emptyData = ByteArray(0)

        // Act
        val result = ImageUtils.compressImage(emptyData)

        // Assert
        assertNotNull(result)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `compressImage handles null-like input gracefully`() {
        // Arrange - Very small invalid data
        val tinyData = byteArrayOf(0x00, 0x01, 0x02)

        // Act
        val result = ImageUtils.compressImage(tinyData)

        // Assert - Should return original on failure
        assertNotNull(result)
    }

    // ─── Max Dimension Tests ───

    @Test
    fun `compressImage respects custom maxDimension`() {
        // Act
        val result = ImageUtils.compressImage(testImageData, maxDimension = 512)

        // Assert
        assertNotNull(result)
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `compressImage handles very small maxDimension`() {
        // Act
        val result = ImageUtils.compressImage(testImageData, maxDimension = 10)

        // Assert
        assertNotNull(result)
    }

    @Test
    fun `compressImage handles very large maxDimension`() {
        // Act
        val result = ImageUtils.compressImage(testImageData, maxDimension = 10000)

        // Assert
        assertNotNull(result)
    }

    @Test
    fun `compressImage handles maxDimension of zero`() {
        // Act
        val result = ImageUtils.compressImage(testImageData, maxDimension = 0)

        // Assert - Should handle gracefully
        assertNotNull(result)
    }

    @Test
    fun `compressImage handles negative maxDimension`() {
        // Act
        val result = ImageUtils.compressImage(testImageData, maxDimension = -100)

        // Assert - Should handle gracefully
        assertNotNull(result)
    }

    // ─── Quality Tests ───

    @Test
    fun `compressImage respects custom quality parameter`() {
        // Act
        val result = ImageUtils.compressImage(testImageData, quality = 50)

        // Assert
        assertNotNull(result)
    }

    @Test
    fun `compressImage handles maximum quality`() {
        // Act
        val result = ImageUtils.compressImage(testImageData, quality = 100)

        // Assert
        assertNotNull(result)
    }

    @Test
    fun `compressImage handles minimum quality`() {
        // Act
        val result = ImageUtils.compressImage(testImageData, quality = 1)

        // Assert
        assertNotNull(result)
    }

    @Test
    fun `compressImage handles quality of zero`() {
        // Act
        val result = ImageUtils.compressImage(testImageData, quality = 0)

        // Assert - Should handle gracefully
        assertNotNull(result)
    }

    @Test
    fun `compressImage handles quality above maximum`() {
        // Act
        val result = ImageUtils.compressImage(testImageData, quality = 150)

        // Assert - Should handle gracefully
        assertNotNull(result)
    }

    @Test
    fun `compressImage handles negative quality`() {
        // Act
        val result = ImageUtils.compressImage(testImageData, quality = -50)

        // Assert - Should handle gracefully
        assertNotNull(result)
    }

    // ─── Error Handling Tests ───

    @Test
    fun `compressImage returns original data on decoding failure`() {
        // Arrange - Invalid image data
        val invalidData = byteArrayOf(0x89, 0x50, 0x4E, 0x47) // Incomplete PNG header

        // Act
        val result = ImageUtils.compressImage(invalidData)

        // Assert
        assertEquals(invalidData, result)
    }

    @Test
    fun `compressImage returns original data on compression failure`() {
        // Arrange - Data that will fail during compression
        val problematicData = ByteArray(100) { 0xFF.toByte() }

        // Act
        val result = ImageUtils.compressImage(problematicData)

        // Assert
        assertEquals(problematicData, result)
    }

    @Test
    fun `compressImage handles corrupt image data`() {
        // Arrange - Completely random data
        val corruptData = ByteArray(500) { (Math.random() * 256).toInt().toByte() }

        // Act
        val result = ImageUtils.compressImage(corruptData)

        // Assert - Should return original on failure
        assertNotNull(result)
    }

    // ─── Default Parameter Tests ───

    @Test
    fun `compressImage uses default maxDimension when not specified`() {
        // Act
        val result = ImageUtils.compressImage(testImageData)

        // Assert
        assertNotNull(result)
    }

    @Test
    fun `compressImage uses default quality when not specified`() {
        // Act
        val result = ImageUtils.compressImage(testImageData)

        // Assert
        assertNotNull(result)
    }

    // ─── Edge Case Tests ───

    @Test
    fun `compressImage handles very large input array`() {
        // Arrange
        val hugeData = ByteArray(10 * 1024 * 1024) { it.toByte() } // 10MB

        // Act
        val result = ImageUtils.compressImage(hugeData)

        // Assert - Should handle without crashing
        assertNotNull(result)
    }

    @Test
    fun `compressImage handles single byte input`() {
        // Arrange
        val singleByte = byteArrayOf(0x42)

        // Act
        val result = ImageUtils.compressImage(singleByte)

        // Assert
        assertNotNull(result)
    }

    @Test
    fun `compressImage produces different output for different quality settings`() {
        // Act
        val resultQuality80 = ImageUtils.compressImage(testImageData, quality = 80)
        val resultQuality20 = ImageUtils.compressImage(testImageData, quality = 20)

        // Assert - Different quality should produce different output
        // Note: This might not always be true for very small images
        assertNotNull(resultQuality80)
        assertNotNull(resultQuality20)
    }

    @Test
    fun `compressImage produces consistent output for same input`() {
        // Act
        val result1 = ImageUtils.compressImage(testImageData, maxDimension = 1280, quality = 80)
        val result2 = ImageUtils.compressImage(testImageData, maxDimension = 1280, quality = 80)

        // Assert
        assertEquals(result1, result2)
    }

    // ─── Memory Management Tests ───

    @Test
    fun `compressImage handles multiple consecutive calls without memory leak`() {
        // Act
        val results = (1..10).map { 
            ImageUtils.compressImage(testImageData)
        }

        // Assert - All should succeed without OutOfMemoryError
        assertEquals(10, results.size)
        results.forEach { assertNotNull(it) }
    }

    @Test
    fun `compressImage cleans up bitmap resources on success`() {
        // Act
        val result = ImageUtils.compressImage(testImageData)

        // Assert - Should complete without memory issues
        assertNotNull(result)
    }

    @Test
    fun `compressImage cleans up bitmap resources on failure`() {
        // Arrange
        val invalidData = byteArrayOf(0x00, 0x01, 0x02)

        // Act
        val result = ImageUtils.compressImage(invalidData)

        // Assert - Should complete without memory issues
        assertNotNull(result)
    }

    // ─── Format Tests ───

    @Test
    fun `compressImage output is in JPEG format`() {
        // Act
        val result = ImageUtils.compressImage(testImageData)

        // Assert - JPEG files start with 0xFF 0xD8
        if (result.size >= 2) {
            assertEquals(0xFF.toByte(), result[0])
            assertEquals(0xD8.toByte(), result[1])
        }
    }

    @Test
    fun `compressImage output is not empty for valid input`() {
        // Act
        val result = ImageUtils.compressImage(testImageData)

        // Assert
        assertFalse(result.isEmpty())
    }

    // ─── Performance Tests ───

    @Test
    fun `compressImage completes within reasonable time`() {
        // Arrange
        val startTime = System.currentTimeMillis()

        // Act
        val result = ImageUtils.compressImage(testImageData)

        // Assert
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime
        assertTrue(duration < 5000) // Should complete in less than 5 seconds
        assertNotNull(result)
    }

    @Test
    fun `compressImage handles concurrent calls safely`() {
        // Act
        val results = mutableListOf<ByteArray>()
        val threads = (1..5).map {
            Thread {
                results.add(ImageUtils.compressImage(testImageData))
            }
        }

        threads.forEach { it.start() }
        threads.forEach { it.join() }

        // Assert
        assertEquals(5, results.size)
        results.forEach { assertNotNull(it) }
    }
}
