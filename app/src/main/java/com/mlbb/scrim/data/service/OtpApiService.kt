package com.mlbb.scrim.data.service

import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.GET
import java.util.concurrent.TimeUnit

data class SendOtpBackendRequest(
    val email: String
)

data class VerifyOtpBackendRequest(
    val email: String,
    val otp: String,
    val password: String,
    val username: String,
    @SerializedName("inGameId")
    val inGameId: String
)

data class OtpBackendResponse(
    @SerializedName("status")
    val status: String = "",
    @SerializedName("message")
    val message: String = ""
)

interface OtpApiService {
    @POST("auth/send-otp")
    suspend fun sendOtp(@Body request: SendOtpBackendRequest): Response<OtpBackendResponse>

    @POST("auth/verify-otp")
    suspend fun verifyOtp(@Body request: VerifyOtpBackendRequest): Response<OtpBackendResponse>

    // Endpoint to wake up the Render backend
    @GET("/")
    suspend fun wakeUp()
}

object OtpApiClient {
    /**
     * URL for the deployed Node.js OTP service.
     */
    private const val OTP_BASE_URL = "https://news-service-yq17.onrender.com/"

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()
    }

    val service: OtpApiService by lazy {
        Retrofit.Builder()
            .baseUrl(OTP_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OtpApiService::class.java)
    }
}
