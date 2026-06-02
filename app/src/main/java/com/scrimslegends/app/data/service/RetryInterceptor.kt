package com.scrimslegends.app.data.service

import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber
import java.io.IOException
import java.net.SocketTimeoutException

/**
 * OkHttp interceptor that retries idempotent GET/HEAD requests on transient failures.
 *
 * Uses exponential backoff to avoid hammering the server during outages.
 * Only retries safe, idempotent methods to prevent duplicate mutations.
 */
class RetryInterceptor(
    private val maxRetries: Int = 3,
    private val initialDelayMs: Long = 300L
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        // Only retry safe, idempotent methods — never POST/PUT/PATCH/DELETE, as retrying
        // those can cause duplicate mutations (double-send messages, double-create records, etc.)
        if (request.method != "GET" && request.method != "HEAD") {
            return chain.proceed(request)
        }

        var lastException: IOException? = null

        for (attempt in 0..maxRetries) {
            try {
                val response = chain.proceed(request)
                // Retry on 5xx or 408/429 (server errors, timeout, rate limit)
                if (attempt < maxRetries && response.code in RETRYABLE_CODES) {
                    Timber.w("Retry %d/%d for %s (HTTP %d)", attempt + 1, maxRetries, request.url, response.code)
                    response.body?.close()
                    // Flat delay capped at initialDelayMs to avoid blocking OkHttp dispatcher threads
                    try {
                        Thread.sleep(initialDelayMs.coerceAtMost(500L))
                    } catch (e: InterruptedException) {
                        Thread.currentThread().interrupt()
                        throw IOException("Retry interrupted for ${request.url}", e)
                    }
                    continue
                }
                return response
            } catch (e: SocketTimeoutException) {
                Timber.w(e, "Retry %d/%d for %s (timeout)", attempt + 1, maxRetries, request.url)
                lastException = e
                if (attempt < maxRetries) {
                    try {
                        Thread.sleep(initialDelayMs.coerceAtMost(500L))
                    } catch (ie: InterruptedException) {
                        Thread.currentThread().interrupt()
                        throw IOException("Retry interrupted for ${request.url}", ie)
                    }
                }
            } catch (e: IOException) {
                Timber.w(e, "Retry %d/%d for %s (IO error)", attempt + 1, maxRetries, request.url)
                lastException = e
                if (attempt < maxRetries) {
                    try {
                        Thread.sleep(initialDelayMs.coerceAtMost(500L))
                    } catch (ie: InterruptedException) {
                        Thread.currentThread().interrupt()
                        throw IOException("Retry interrupted for ${request.url}", ie)
                    }
                }
            }
        }

        throw lastException ?: IOException("Retry exhausted for ${request.url}")
    }

    companion object {
        private val RETRYABLE_CODES = setOf(408, 429, 500, 502, 503, 504)
    }
}
