package com.mlbb.scrim.data.service

import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import timber.log.Timber

/**
 * Tests for RetryInterceptor backoff and retryable code detection.
 */
class RetryInterceptorTest {

    private val interceptor = RetryInterceptor(maxRetries = 2, initialDelayMs = 10L)

    @Before
    fun setup() {
        if (Timber.forest().isEmpty()) {
            Timber.plant(object : Timber.Tree() {
                override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {}
            })
        }
    }

    private fun fakeChain(response: Response, throwOnAttempt: Int = -1): Interceptor.Chain {
        var attempt = 0
        return object : Interceptor.Chain {
            override fun request(): Request = Request.Builder().url("https://example.com/test").build()
            override fun proceed(request: Request): Response {
                attempt++
                if (throwOnAttempt == attempt) {
                    throw java.io.IOException("Simulated failure")
                }
                return response
            }
            override fun connection() = null
            override fun call() = throw UnsupportedOperationException()
            override fun connectTimeoutMillis() = 0
            override fun withConnectTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit) = this
            override fun readTimeoutMillis() = 0
            override fun withReadTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit) = this
            override fun writeTimeoutMillis() = 0
            override fun withWriteTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit) = this
        }
    }

    private fun response(code: Int): Response {
        return Response.Builder()
            .request(Request.Builder().url("https://example.com/test").build())
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message("Test")
            .build()
    }

    @Test
    fun `returns immediately on HTTP 200`() {
        val result = interceptor.intercept(fakeChain(response(200)))
        assertEquals(200, result.code)
    }

    @Test
    fun `retries on HTTP 503 then succeeds`() {
        var callCount = 0
        val chain = object : Interceptor.Chain {
            override fun request(): Request = Request.Builder().url("https://example.com/test").build()
            override fun proceed(request: Request): Response {
                callCount++
                return if (callCount < 2) response(503) else response(200)
            }
            override fun connection() = null
            override fun call() = throw UnsupportedOperationException()
            override fun connectTimeoutMillis() = 0
            override fun withConnectTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit) = this
            override fun readTimeoutMillis() = 0
            override fun withReadTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit) = this
            override fun writeTimeoutMillis() = 0
            override fun withWriteTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit) = this
        }
        val result = interceptor.intercept(chain)
        assertEquals(200, result.code)
        assertEquals(2, callCount)
    }

    @Test
    fun `returns last response after exhausting retries on persistent 503`() {
        val result = interceptor.intercept(fakeChain(response(503)))
        assertEquals(503, result.code)
    }

    @Test
    fun `does not retry on HTTP 400`() {
        val result = interceptor.intercept(fakeChain(response(400)))
        assertEquals(400, result.code)
    }

    @Test
    fun `retries on timeout exception then succeeds`() {
        var callCount = 0
        val chain = object : Interceptor.Chain {
            override fun request(): Request = Request.Builder().url("https://example.com/test").build()
            override fun proceed(request: Request): Response {
                callCount++
                if (callCount < 2) throw java.net.SocketTimeoutException("timeout")
                return response(200)
            }
            override fun connection() = null
            override fun call() = throw UnsupportedOperationException()
            override fun connectTimeoutMillis() = 0
            override fun withConnectTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit) = this
            override fun readTimeoutMillis() = 0
            override fun withReadTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit) = this
            override fun writeTimeoutMillis() = 0
            override fun withWriteTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit) = this
        }
        val result = interceptor.intercept(chain)
        assertEquals(200, result.code)
        assertEquals(2, callCount)
    }
}
