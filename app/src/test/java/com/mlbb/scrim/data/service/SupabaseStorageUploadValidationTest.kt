package com.mlbb.scrim.data.service

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for SupabaseStorageUpload client-side validation.
 *
 * Verifies that oversized and invalid file types are rejected before upload.
 */
class SupabaseStorageUploadValidationTest {

    @Test
    fun `uploadFile rejects files larger than 10MB`() = runBlocking {
        val oversized = ByteArray(11 * 1024 * 1024) // 11 MB
        val result = SupabaseStorageUpload.uploadFile(
            bucket = "test-bucket",
            path = "test.jpg",
            fileBytes = oversized,
            contentType = "image/jpeg"
        )
        assertTrue("Should fail for oversized file", result.isFailure)
        val msg = result.exceptionOrNull()?.message ?: ""
        assertTrue("Error should mention size", msg.contains("10MB"))
    }

    @Test
    fun `uploadFile rejects unsupported MIME types`() = runBlocking {
        val bytes = ByteArray(1024)
        val result = SupabaseStorageUpload.uploadFile(
            bucket = "test-bucket",
            path = "test.gif",
            fileBytes = bytes,
            contentType = "image/gif"
        )
        assertTrue("Should fail for unsupported type", result.isFailure)
        val msg = result.exceptionOrNull()?.message ?: ""
        assertTrue("Error should mention file type", msg.contains("Unsupported"))
    }

    @Test
    fun `uploadFile accepts 10MB exact PNG`() = runBlocking {
        val bytes = ByteArray(10 * 1024 * 1024) // exactly 10 MB
        // This will fail at network layer (no server), but validation should pass
        val result = SupabaseStorageUpload.uploadFile(
            bucket = "test-bucket",
            path = "test.png",
            fileBytes = bytes,
            contentType = "image/png"
        )
        // We expect a network failure, not a validation failure
        val msg = result.exceptionOrNull()?.message ?: ""
        assertFalse("Should not be validation error", msg.contains("File too large"))
        assertFalse("Should not be type error", msg.contains("Unsupported"))
    }

    @Test
    fun `uploadFile accepts JPEG under 10MB`() = runBlocking {
        val bytes = ByteArray(1024)
        val result = SupabaseStorageUpload.uploadFile(
            bucket = "test-bucket",
            path = "test.jpg",
            fileBytes = bytes,
            contentType = "image/jpeg"
        )
        val msg = result.exceptionOrNull()?.message ?: ""
        assertFalse("Should not be validation error", msg.contains("File too large"))
        assertFalse("Should not be type error", msg.contains("Unsupported"))
    }
}
