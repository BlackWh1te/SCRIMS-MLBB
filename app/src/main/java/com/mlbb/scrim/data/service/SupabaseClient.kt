package com.mlbb.scrim.data.service

import android.content.Context
import com.mlbb.scrim.security.SecureStorage
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.mlbb.scrim.BuildConfig
import java.util.concurrent.TimeUnit

/**
 * Supabase client configuration using REST API.
 *
 * Uses Retrofit/OkHttp (already in dependencies) to call Supabase's auto-generated REST API.
 * No additional Kotlin client library needed.
 *
 * Database is live at: https://efhbyrhxtsadbqjsfogc.supabase.co
 */
object SupabaseConfig {
    val SUPABASE_URL = BuildConfig.SUPABASE_URL
    val SUPABASE_ANON_KEY = BuildConfig.SUPABASE_ANON_KEY

    // REST API base URL (Supabase exposes PostgREST at /rest/v1)
    val REST_API_URL = "$SUPABASE_URL/rest/v1/"

    // Auth API base URL (Supabase Auth API at /auth/v1)
    val AUTH_API_URL = "$SUPABASE_URL/auth/v1/"

    // Table names matching the schema.sql
    const val TABLE_PROFILES = "profiles"
    const val TABLE_TEAMS = "teams"
    const val TABLE_TEAM_MEMBERS = "team_members"
    const val TABLE_TEAM_INVITATIONS = "team_invitations"
    const val TABLE_PLAYER_STATS = "player_stats"
    const val TABLE_SCRIMS = "scrims"
    const val TABLE_SCRIM_APPLICATIONS = "scrim_applications"
    const val TABLE_SCRIM_ROSTERS = "scrim_rosters"
    const val TABLE_MATCHES = "matches"
    const val TABLE_MESSAGES = "messages"
    const val TABLE_MATCH_RESULTS = "match_results"

    // Storage bucket names
    const val BUCKET_SCREENSHOTS = "match-screenshots"
    const val BUCKET_AVATARS = "user-avatars"
    const val BUCKET_TEAM_LOGOS = "team-logos"
}

/**
 * Provides the current authenticated Supabase access token to Retrofit.
 *
 * Falls back to the anon key when no user session exists.
 */
object SupabaseSession {
    private const val KEY_ACCESS_TOKEN = "supabase_access_token"
    private const val KEY_REFRESH_TOKEN = "supabase_refresh_token"
    private const val KEY_USER_ID = "supabase_user_id"

    @Volatile
    private var secureStorage: SecureStorage? = null

    fun initialize(context: Context) {
        secureStorage = SecureStorage.getInstance(context.applicationContext)
    }

    fun getAccessTokenOrNull(): String? {
        return secureStorage?.getEncrypted(KEY_ACCESS_TOKEN, "")?.takeIf { it.isNotBlank() }
    }

    fun getRefreshTokenOrNull(): String? {
        return secureStorage?.getEncrypted(KEY_REFRESH_TOKEN, "")?.takeIf { it.isNotBlank() }
    }

    fun saveTokens(accessToken: String, refreshToken: String) {
        secureStorage?.storeEncrypted(KEY_ACCESS_TOKEN, accessToken)
        secureStorage?.storeEncrypted(KEY_REFRESH_TOKEN, refreshToken)
    }
}

/**
 * Authenticator that handles 401 errors by refreshing the Supabase token.
 */
class SupabaseAuthenticator : okhttp3.Authenticator {
    override fun authenticate(route: okhttp3.Route?, response: okhttp3.Response): okhttp3.Request? {
        if (response.count() > 2) return null // Prevent infinite retry loops

        val refreshToken = SupabaseSession.getRefreshTokenOrNull() ?: return null
        
        // Use a separate client without authenticator to avoid loops
        val authClient = SupabaseAuthRetrofitClient.retrofit.create(SupabaseAuthService::class.java)
        val refreshResponse = kotlinx.coroutines.runBlocking {
            try {
                authClient.refreshToken(RefreshTokenRequest(refreshToken))
            } catch (e: Exception) {
                null
            }
        }

        if (refreshResponse?.isSuccessful == true) {
            val body = refreshResponse.body()
            if (body?.accessToken != null && body.refreshToken != null) {
                SupabaseSession.saveTokens(body.accessToken, body.refreshToken)
                
                return response.request.newBuilder()
                    .header("Authorization", "Bearer ${body.accessToken}")
                    .build()
            }
        }

        return null
    }

    private fun okhttp3.Response.count(): Int {
        var result = 1
        var r = priorResponse
        while (r != null) {
            result++
            r = r.priorResponse
        }
        return result
    }
}

/**
 * Retrofit client for Supabase REST API.
 */
object SupabaseRetrofitClient {

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .authenticator(SupabaseAuthenticator())
            .addInterceptor { chain ->
                val bearerToken = SupabaseSession.getAccessTokenOrNull() ?: SupabaseConfig.SUPABASE_ANON_KEY
                val request = chain.request().newBuilder()
                    .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                    .addHeader("Authorization", "Bearer $bearerToken")
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=representation")
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(HttpLoggingInterceptor().apply {
                // Log full requests/responses in debug builds
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()
    }

    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(SupabaseConfig.REST_API_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}

/**
 * Retrofit client for Supabase Auth API.
 */
object SupabaseAuthRetrofitClient {

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                    .addHeader("Content-Type", "application/json")
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()
    }

    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(SupabaseConfig.AUTH_API_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}
