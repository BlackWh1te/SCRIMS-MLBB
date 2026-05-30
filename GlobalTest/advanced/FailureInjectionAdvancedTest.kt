package com.mlbb.scrim.advanced

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Advanced failure injection tests covering network failures, API errors, timeouts, and system failures.
 * 
 * Test Categories:
 * - Network disconnect simulation
 * - API error handling
 * - Timeout scenarios
 * - Malformed data handling
 * - Unauthorized access
 * - Database unavailable
 * - Cache corruption
 * - Disk full scenarios
 * - Rate limiting
 * - Retry logic
 */
class FailureInjectionAdvancedTest {

    // ─── NETWORK FAILURE SIMULATION ───

    @Test
    fun `network disconnect causes operation failure`() {
        // Arrange
        suspend fun networkOperation(): Result<String> {
            // Simulate network disconnect
            throw java.net.UnknownHostException("Network unreachable")
        }

        // Act
        val result = try {
            Result.success(networkOperation().getOrThrow())
        } catch (e: Exception) {
            Result.failure(e)
        }

        // Assert
        assertTrue(result.isFailure, "Network disconnect should cause failure")
        assertTrue(result.exceptionOrNull() is java.net.UnknownHostException)
    }

    @Test
    fun `network timeout causes operation failure`() {
        // Arrange
        suspend fun slowNetworkOperation(): String {
            delay(5000) // Simulate slow network
            return "success"
        }

        // Act
        val result = try {
            withTimeout(100) {
                slowNetworkOperation()
            }
            Result.success("completed")
        } catch (e: TimeoutCancellationException) {
            Result.failure(e)
        }

        // Assert
        assertTrue(result.isFailure, "Network timeout should cause failure")
    }

    @Test
    fun `connection reset causes operation failure`() {
        // Arrange
        suspend fun networkOperation(): Result<String> {
            // Simulate connection reset
            throw java.net.SocketException("Connection reset")
        }

        // Act
        val result = networkOperation()

        // Assert
        assertTrue(result.isFailure, "Connection reset should cause failure")
    }

    // ─── API ERROR HANDLING ───

    @Test
    fun `API 500 error causes operation failure`() {
        // Arrange
        data class ApiResponse(val status: Int, val data: String?)

        suspend fun apiCall(): ApiResponse {
            // Simulate 500 error
            throw ApiException("Internal Server Error", 500)
        }

        // Act
        val result = try {
            val response = apiCall()
            if (response.status >= 500) {
                Result.failure(Exception("Server error: ${response.status}"))
            } else {
                Result.success(response)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }

        // Assert
        assertTrue(result.isFailure, "API 500 error should cause failure")
    }

    @Test
    fun `API 401 unauthorized error causes operation failure`() {
        // Arrange
        suspend fun authenticatedApiCall(token: String): Result<String> {
            if (token != "valid_token") {
                throw ApiException("Unauthorized", 401)
            }
            return Result.success("data")
        }

        // Act
        val result = try {
            authenticatedApiCall("invalid_token")
        } catch (e: Exception) {
            Result.failure(e)
        }

        // Assert
        assertTrue(result.isFailure, "401 error should cause failure")
    }

    @Test
    fun `API 429 rate limit error causes operation failure`() {
        // Arrange
        suspend fun rateLimitedApiCall(): Result<String> {
            throw ApiException("Too Many Requests", 429)
        }

        // Act
        val result = try {
            rateLimitedApiCall()
        } catch (e: Exception) {
            Result.failure(e)
        }

        // Assert
        assertTrue(result.isFailure, "429 error should cause failure")
    }

    @Test
    fun `API 404 not found error causes operation failure`() {
        // Arrange
        suspend fun getResourceApiCall(resourceId: String): Result<String> {
            if (resourceId == "nonexistent") {
                throw ApiException("Not Found", 404)
            }
            return Result.success("data")
        }

        // Act
        val result = try {
            getResourceApiCall("nonexistent")
        } catch (e: Exception) {
            Result.failure(e)
        }

        // Assert
        assertTrue(result.isFailure, "404 error should cause failure")
    }

    // ─── MALFORMED DATA HANDLING ───

    @Test
    fun `malformed JSON causes parsing failure`() {
        // Arrange
        val invalidJson = "{ invalid json }"

        // Act
        val result = try {
            // Simulate JSON parsing
            if (!invalidJson.startsWith("{") || !invalidJson.endsWith("}")) {
                throw IllegalArgumentException("Invalid JSON format")
            }
            Result.success("parsed")
        } catch (e: Exception) {
            Result.failure(e)
        }

        // Assert
        assertTrue(result.isFailure, "Malformed JSON should cause parsing failure")
    }

    @Test
    fun `null data handling causes graceful degradation`() {
        // Arrange
        suspend fun fetchData(): String? {
            return null // Simulate null data
        }

        // Act
        val result = try {
            val data = fetchData()
            if (data == null) {
                Result.failure(NullPointerException("Data is null"))
            } else {
                Result.success(data)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }

        // Assert
        assertTrue(result.isFailure, "Null data should cause failure")
    }

    @Test
    fun `empty data handling causes graceful degradation`() {
        // Arrange
        suspend fun fetchData(): String {
            return "" // Simulate empty data
        }

        // Act
        val result = try {
            val data = fetchData()
            if (data.isEmpty()) {
                Result.failure(IllegalArgumentException("Data is empty"))
            } else {
                Result.success(data)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }

        // Assert
        assertTrue(result.isFailure, "Empty data should cause failure")
    }

    // ─── DATABASE FAILURE SCENARIOS ───

    @Test
    fun `database unavailable causes operation failure`() {
        // Arrange
        suspend fun databaseOperation(): Result<String> {
            // Simulate database unavailable
            throw java.sql.SQLException("Database connection failed")
        }

        // Act
        val result = try {
            databaseOperation()
        } catch (e: Exception) {
            Result.failure(e)
        }

        // Assert
        assertTrue(result.isFailure, "Database unavailability should cause failure")
    }

    @Test
    fun `database constraint violation causes operation failure`() {
        // Arrange
        suspend fun insertDuplicate(): Result<String> {
            // Simulate duplicate key constraint violation
            throw java.sql.SQLException("Duplicate entry", "23000")
        }

        // Act
        val result = try {
            insertDuplicate()
        } catch (e: Exception) {
            Result.failure(e)
        }

        // Assert
        assertTrue(result.isFailure, "Constraint violation should cause failure")
    }

    @Test
    fun `database timeout causes operation failure`() {
        // Arrange
        suspend fun slowDatabaseQuery(): String {
            withTimeout(100) {
                delay(5000) // Simulate slow query
            }
            return "result"
        }

        // Act
        val result = try {
            Result.success(slowDatabaseQuery())
        } catch (e: TimeoutCancellationException) {
            Result.failure(e)
        }

        // Assert
        assertTrue(result.isFailure, "Database timeout should cause failure")
    }

    // ─── CACHE CORRUPTION SCENARIOS ───

    @Test
    fun `cache corruption causes fallback to source`() {
        // Arrange
        var cacheCorrupted = false
        var sourceUsed = false

        suspend fun getFromCache(): String? {
            if (cacheCorrupted) {
                throw RuntimeException("Cache corrupted")
            }
            return "cached_data"
        }

        suspend fun getFromSource(): String {
            sourceUsed = true
            return "source_data"
        }

        // Act
        cacheCorrupted = true
        val result = try {
            getFromCache()
        } catch (e: Exception) {
            getFromSource() // Fallback to source
        }

        // Assert
        assertTrue(sourceUsed, "Should fallback to source when cache is corrupted")
        assertEquals("source_data", result)
    }

    @Test
    fun `cache miss causes source fetch`() {
        // Arrange
        var sourceFetched = false

        suspend fun getFromCache(): String? {
            return null // Cache miss
        }

        suspend fun getFromSource(): String {
            sourceFetched = true
            return "source_data"
        }

        // Act
        val result = getFromCache() ?: getFromSource()

        // Assert
        assertTrue(sourceFetched, "Should fetch from source on cache miss")
        assertEquals("source_data", result)
    }

    @Test
    fun `cache expiration causes refresh`() {
        // Arrange
        var cacheExpired = false
        var refreshed = false

        suspend fun getFromCache(): String? {
            if (cacheExpired) {
                return null // Expired
            }
            return "cached_data"
        }

        suspend fun refreshCache(): String {
            refreshed = true
            return "refreshed_data"
        }

        // Act
        cacheExpired = true
        val result = getFromCache() ?: refreshCache()

        // Assert
        assertTrue(refreshed, "Should refresh cache when expired")
        assertEquals("refreshed_data", result)
    }

    // ─── DISK FULL SCENARIOS ───

    @Test
    fun `disk full causes file write failure`() {
        // Arrange
        suspend fun writeFile(data: String): Result<Unit> {
            // Simulate disk full
            throw java.io.IOException("No space left on device")
        }

        // Act
        val result = try {
            writeFile("test data")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }

        // Assert
        assertTrue(result.isFailure, "Disk full should cause write failure")
    }

    @Test
    fun `disk full causes graceful degradation`() {
        // Arrange
        var writeFailed = false
        var fallbackUsed = false

        suspend fun writeToDisk(data: String): Result<Unit> {
            if (writeFailed) {
                throw java.io.IOException("No space left on device")
            }
            return Result.success(Unit)
        }

        suspend fun writeToMemory(data: String): Result<Unit> {
            fallbackUsed = true
            return Result.success(Unit)
        }

        // Act
        writeFailed = true
        val result = try {
            writeToDisk("test data")
        } catch (e: Exception) {
            writeToMemory("test data")
        }

        // Assert
        assertTrue(fallbackUsed, "Should use fallback when disk is full")
        assertTrue(result.isSuccess, "Fallback should succeed")
    }

    // ─── RETRY LOGIC TESTS ───

    @Test
    fun `retry logic handles transient failures`() {
        // Arrange
        var attemptCount = 0
        val maxAttempts = 3

        suspend fun flakyOperation(): String {
            attemptCount++
            if (attemptCount < maxAttempts) {
                throw RuntimeException("Transient failure")
            }
            return "success"
        }

        // Act
        val result = try {
            withRetry(maxAttempts) {
                flakyOperation()
            }
        } catch (e: Exception) {
            "failed"
        }

        // Assert
        assertEquals("success", result, "Retry should eventually succeed")
        assertEquals(maxAttempts, attemptCount)
    }

    @Test
    fun `retry logic gives up after max attempts`() {
        // Arrange
        var attemptCount = 0
        val maxAttempts = 3

        suspend fun failingOperation(): String {
            attemptCount++
            throw RuntimeException("Persistent failure")
        }

        // Act
        val result = try {
            withRetry(maxAttempts) {
                failingOperation()
            }
            "success"
        } catch (e: Exception) {
            "failed"
        }

        // Assert
        assertEquals("failed", result, "Should give up after max attempts")
        assertEquals(maxAttempts, attemptCount)
    }

    @Test
    fun `retry with exponential backoff`() {
        // Arrange
        var attemptCount = 0
        val delays = mutableListOf<Long>()
        val maxAttempts = 3

        suspend fun flakyOperationWithBackoff(): String {
            attemptCount++
            if (attemptCount < maxAttempts) {
                throw RuntimeException("Transient failure")
            }
            return "success"
        }

        // Act
        val startTime = System.currentTimeMillis()
        val result = try {
            withRetry(maxAttempts, backoff = true) {
                flakyOperationWithBackoff()
            }
        } catch (e: Exception) {
            "failed"
        }
        val totalTime = System.currentTimeMillis() - startTime

        // Assert
        assertEquals("success", result)
        assertTrue(totalTime > 100, "Exponential backoff should add delay")
    }

    // ─── RATE LIMITING TESTS ───

    @Test
    fun `rate limiting prevents excessive requests`() {
        // Arrange
        var requestCount = 0
        val maxRequestsPerSecond = 5
        val rateLimiter = RateLimiter(maxRequestsPerSecond)

        // Act
        val results = mutableListOf<Result<String>>()
        repeat(10) {
            runBlocking {
                if (rateLimiter.tryAcquire()) {
                    requestCount++
                    results.add(Result.success("request_$it"))
                } else {
                    results.add(Result.failure(RuntimeException("Rate limited")))
                }
            }
        }

        // Assert
        assertTrue(requestCount <= maxRequestsPerSecond, "Rate limiter should prevent excessive requests")
        assertTrue(results.any { it.isFailure }, "Some requests should be rate limited")
    }

    @Test
    fun `rate limiting allows requests after reset`() {
        // Arrange
        var requestCount = 0
        val maxRequestsPerSecond = 5
        val rateLimiter = RateLimiter(maxRequestsPerSecond)

        // Act - Exhaust rate limit
        repeat(maxRequestsPerSecond) {
            runBlocking {
                if (rateLimiter.tryAcquire()) {
                    requestCount++
                }
            }
        }

        // Wait for reset
        delay(1100)

        // Try again after reset
        val allowedAfterReset = runBlocking {
            rateLimiter.tryAcquire()
        }

        // Assert
        assertTrue(allowedAfterReset, "Rate limiter should allow requests after reset")
    }

    // ─── CIRCUIT BREAKER TESTS ───

    @Test
    fun `circuit breaker opens after consecutive failures`() {
        // Arrange
        var failureCount = 0
        val circuitBreaker = CircuitBreaker(threshold = 3)

        suspend fun failingOperation(): String {
            failureCount++
            throw RuntimeException("Operation failed")
        }

        // Act
        repeat(5) {
            try {
                if (circuitBreaker.allowRequest()) {
                    failingOperation()
                } else {
                    // Circuit breaker is open
                }
            } catch (e: Exception) {
                circuitBreaker.recordFailure()
            }
        }

        // Assert
        assertTrue(circuitBreaker.isOpen, "Circuit breaker should open after threshold failures")
    }

    @Test
    fun `circuit breaker prevents requests when open`() {
        // Arrange
        val circuitBreaker = CircuitBreaker(threshold = 1)
        circuitBreaker.recordFailure() // Force open

        // Act
        val allowed = circuitBreaker.allowRequest()

        // Assert
        assertTrue(!allowed, "Circuit breaker should prevent requests when open")
    }

    // ─── FLOW ERROR HANDLING TESTS ───

    @Test
    fun `flow catches and handles errors`() {
        // Arrange
        suspend fun failingFlow(): Flow<Int> = flow {
            emit(1)
            emit(2)
            throw RuntimeException("Flow error")
            emit(3)
        }

        // Act
        val results = mutableListOf<Int>()
        val errors = mutableListOf<Throwable>()

        failingFlow()
            .catch { e -> errors.add(e) }
            .collect { results.add(it) }

        // Assert
        assertEquals(listOf(1, 2), results, "Should emit values before error")
        assertEquals(1, errors.size, "Should catch the error")
    }

    @Test
    fun `flow retry handles transient failures`() {
        // Arrange
        var attemptCount = 0
        suspend fun retryFlow(): Flow<Int> = flow {
            attemptCount++
            if (attemptCount < 3) {
                throw RuntimeException("Transient failure")
            }
            emit(42)
        }

        // Act
        val result = retryFlow()
            .retry(3) { e -> e is RuntimeException }
            .first()

        // Assert
        assertEquals(42, result, "Retry should eventually succeed")
        assertEquals(3, attemptCount)
    }

    // ─── HELPER CLASSES ───

    class ApiException(message: String, val statusCode: Int) : Exception(message)

    suspend fun <T> withRetry(
        maxAttempts: Int,
        backoff: Boolean = false,
        block: suspend () -> T
    ): T {
        var lastException: Exception? = null
        repeat(maxAttempts) { attempt ->
            try {
                return block()
            } catch (e: Exception) {
                lastException = e
                if (backoff && attempt < maxAttempts - 1) {
                    delay((2.0.pow(attempt) * 100).toLong()) // Exponential backoff
                }
            }
        }
        throw lastException ?: RuntimeException("Max attempts exceeded")
    }

    class RateLimiter(private val maxRequests: Int) {
        private val requestTimes = mutableListOf<Long>()

        fun tryAcquire(): Boolean {
            val now = System.currentTimeMillis()
            requestTimes.removeIf { now - it > 1000 } // Remove requests older than 1 second
            if (requestTimes.size < maxRequests) {
                requestTimes.add(now)
                return true
            }
            return false
        }
    }

    class CircuitBreaker(private val threshold: Int) {
        private var failures = 0
        private var state = State.CLOSED

        enum class State { CLOSED, OPEN }

        fun allowRequest(): Boolean {
            return when (state) {
                State.CLOSED -> true
                State.OPEN -> false
            }
        }

        fun recordFailure() {
            failures++
            if (failures >= threshold) {
                state = State.OPEN
            }
        }

        fun isOpen(): Boolean = state == State.OPEN
    }
}
