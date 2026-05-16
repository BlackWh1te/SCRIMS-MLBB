package com.mlbb.scrim.data.service

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

// X API v2 (Twitter API) response models
data class XSearchResponse(
    @SerializedName("data")
    val data: List<XTweet>? = null,
    @SerializedName("includes")
    val includes: XIncludes? = null,
    @SerializedName("meta")
    val meta: XMeta? = null
)

data class XTweet(
    @SerializedName("id")
    val id: String = "",
    @SerializedName("text")
    val text: String = "",
    @SerializedName("created_at")
    val createdAt: String = "",
    @SerializedName("public_metrics")
    val publicMetrics: XPublicMetrics? = null,
    @SerializedName("attachments")
    val attachments: XAttachments? = null
)

data class XPublicMetrics(
    @SerializedName("like_count")
    val likeCount: Int = 0,
    @SerializedName("retweet_count")
    val retweetCount: Int = 0
)

data class XAttachments(
    @SerializedName("media_keys")
    val mediaKeys: List<String>? = null
)

data class XIncludes(
    @SerializedName("media")
    val media: List<XMedia>? = null
)

data class XMedia(
    @SerializedName("media_key")
    val mediaKey: String = "",
    @SerializedName("type")
    val type: String = "",
    @SerializedName("url")
    val url: String? = null,
    @SerializedName("preview_image_url")
    val previewImageUrl: String? = null
)

data class XMeta(
    @SerializedName("result_count")
    val resultCount: Int = 0,
    @SerializedName("next_token")
    val nextToken: String? = null
)

interface TwitterApiService {
    @GET("2/tweets/search/recent")
    suspend fun searchRecentTweets(
        @Query("query") query: String,
        @Query("tweet.fields") tweetFields: String = "created_at,public_metrics,attachments",
        @Query("expansions") expansions: String = "attachments.media_keys",
        @Query("media.fields") mediaFields: String = "url,preview_image_url,type",
        @Query("max_results") maxResults: Int = 20,
        @Header("Authorization") authorization: String
    ): Response<XSearchResponse>
}

object TwitterApiClient {
    private const val X_API_BASE_URL = "https://api.x.com/"

    val service: TwitterApiService by lazy {
        Retrofit.Builder()
            .baseUrl(X_API_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TwitterApiService::class.java)
    }
}
