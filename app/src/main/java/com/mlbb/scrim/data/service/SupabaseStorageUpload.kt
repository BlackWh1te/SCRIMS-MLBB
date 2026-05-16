package com.mlbb.scrim.data.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Uploads files to Supabase Storage buckets.
 *
 * Uses raw OkHttp because Supabase Storage expects a binary body (not multipart).
 * Includes Authorization header with user session token for authenticated access.
 *
 * NOTE: Large files may take time; currently runs on Dispatchers.IO to prevent ANR.
 * Future improvement: Add a progress listener for better UI feedback during large uploads.
 */
object SupabaseStorageUpload {

    private val client = OkHttpClient()

    /**
     * Upload a file to a Supabase Storage bucket.
     *
     * @param bucket Storage bucket name (e.g. "match-screenshots")
     * @param path Object path inside the bucket (e.g. "screenshots/scrim1_team1_123.png")
     * @param fileBytes Raw file bytes
     * @param contentType MIME type (e.g. "image/png", "image/jpeg")
     * @return Public URL of the uploaded object on success
     */
    suspend fun uploadFile(
        bucket: String,
        path: String,
        fileBytes: ByteArray,
        contentType: String = "image/png"
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val requestBody = fileBytes.toRequestBody(contentType.toMediaTypeOrNull())
            // Get the user's access token for authenticated upload
            val bearerToken = SupabaseSession.getAccessTokenOrNull() ?: SupabaseConfig.SUPABASE_ANON_KEY
            val request = Request.Builder()
                .url("${SupabaseConfig.SUPABASE_URL}/storage/v1/object/$bucket/$path")
                .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer $bearerToken")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful || response.code == 200 || response.code == 201) {
                val publicUrl = "${SupabaseConfig.SUPABASE_URL}/storage/v1/object/public/$bucket/$path"
                Result.success(publicUrl)
            } else {
                val errorBody = response.body?.string()
                Result.failure(
                    Exception("Upload failed: HTTP ${response.code} – ${errorBody ?: response.message}")
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
