package com.mlbb.scrim.advanced

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Advanced concurrency tests covering race conditions, parallel execution, cancellation, and synchronization.
 * 
 * Test Categories:
 * - Race conditions
 * - Parallel execution
 * - Cancellation scenarios
 * - Timeout handling
 * - Mutex behavior
 * - Concurrent data structure access
 * - Deadlock prevention
 * - Memory consistency
 */
class ConcurrencyAdvancedTest {

    // ─── RACE CONDITION TESTS ───

    @Test
    fun `concurrent counter increment without synchronization produces race condition`() {
        // Arrange
        var counter = 0
        val incrementCount = 1000
        val threadCount = 10

        // Act - Increment counter concurrently without synchronization
        val jobs = List(threadCount) {
            CoroutineScope(Dispatchers.Default).launch {
                repeat(incrementCount) {
                    counter++ // This is a race condition
                }
            }
        }

        jobs.forEach { it.join() }

        // Assert - Due to race condition, counter may not equal expected value
        // Expected: 10000, Actual: likely less due to lost updates
        assertTrue(counter < incrementCount * threadCount, "Race condition should cause lost updates")
    }

    @Test
    fun `concurrent counter increment with Mutex prevents race condition`() {
        // Arrange
        var counter = 0
        val mutex = Mutex()
        val incrementCount = 1000
        val threadCount = 10

        // Act - Increment counter concurrently with Mutex synchronization
        val jobs = List(threadCount) {
            CoroutineScope(Dispatchers.Default).launch {
                repeat(incrementCount) {
                    mutex.withLock {
                        counter++
                    }
                }
            }
        }

        jobs.forEach { it.join() }

        // Assert - With Mutex, counter should equal expected value
        assertEquals(incrementCount * threadCount, counter)
    }

    @Test
    fun `concurrent map access without synchronization can cause data loss`() {
        // Arrange
        val map = mutableMapOf<String, Int>()
        val itemCount = 1000
        val threadCount = 10

        // Act - Insert into map concurrently without synchronization
        val jobs = List(threadCount) {
            CoroutineScope(Dispatchers.Default).launch {
                repeat(itemCount) { i ->
                    map["key_${it}_$i"] = i
                }
            }
        }

        jobs.forEach { it.join() }

        // Assert - Due to race condition, map may have fewer entries than expected
        assertTrue(map.size < itemCount * threadCount, "Race condition should cause data loss")
    }

    @Test
    fun `concurrent map access with ConcurrentHashMap prevents data loss`() {
        // Arrange
        val map = java.util.concurrent.ConcurrentHashMap<String, Int>()
        val itemCount = 1000
        val threadCount = 10

        // Act - Insert into ConcurrentHashMap concurrently
        val jobs = List(threadCount) {
            CoroutineScope(Dispatchers.Default).launch {
                repeat(itemCount) { i ->
                    map["key_${it}_$i"] = i
                }
            }
        }

        jobs.forEach { it.join() }

        // Assert - With ConcurrentHashMap, all entries should be preserved
        assertEquals(itemCount * threadCount, map.size)
    }

    // ─── PARALLEL EXECUTION TESTS ───

    @Test
    fun `parallel execution completes faster than sequential`() {
        // Arrange
        suspend fun heavyTask(taskId: Int): Int {
            delay(100) // Simulate heavy work
            return taskId * 2
        }

        // Act - Sequential execution
        val sequentialStart = System.currentTimeMillis()
        val sequentialResults = (1..10).map { heavyTask(it) }
        val sequentialTime = System.currentTimeMillis() - sequentialStart

        // Act - Parallel execution
        val parallelStart = System.currentTimeMillis()
        val parallelResults = (1..10).map { 
            CoroutineScope(Dispatchers.Default).async { heavyTask(it) }
        }.awaitAll()
        val parallelTime = System.currentTimeMillis() - parallelStart

        // Assert
        assertEquals(sequentialResults, parallelResults)
        assertTrue(parallelTime < sequentialTime, "Parallel execution should be faster")
    }

    @Test
    fun `parallel execution with error handling`() {
        // Arrange
        suspend fun taskThatFails(id: Int): Int {
            delay(10)
            if (id == 5) throw RuntimeException("Task $id failed")
            return id
        }

        // Act - Parallel execution with error handling
        val results = mutableListOf<Result<Int>>()
        val jobs = (1..10).map { id ->
            CoroutineScope(Dispatchers.Default).async {
                try {
                    Result.success(taskThatFails(id))
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }
        }

        jobs.forEach { 
            results.add(it.await()) 
        }

        // Assert
        assertEquals(10, results.size)
        assertTrue(results.any { it.isFailure }, "At least one task should fail")
        assertTrue(results.any { it.isSuccess }, "Other tasks should succeed")
    }

    // ─── CANCELLATION TESTS ───

    @Test
    fun `cancellation stops coroutine execution`() {
        // Arrange
        var executedCount = 0
        val job = CoroutineScope(Dispatchers.Default).launch {
            repeat(1000) { i ->
                ensureActive() // Check for cancellation
                executedCount++
                delay(10)
            }
        }

        // Act - Cancel job after some time
        delay(50)
        job.cancel()
        job.join()

        // Assert
        assertTrue(executedCount < 1000, "Job should be cancelled before completion")
    }

    @Test
    fun `cancellation with timeout`() {
        // Arrange
        var completed = false

        // Act
        try {
            withTimeout(100) {
                delay(1000) // This will timeout
                completed = true
            }
        } catch (e: TimeoutCancellationException) {
            // Expected
        }

        // Assert
        assertTrue(!completed, "Task should be cancelled by timeout")
    }

    @Test
    fun `cancellation of parent cancels children`() {
        // Arrange
        var child1Executed = false
        var child2Executed = false

        // Act
        val parentJob = CoroutineScope(Dispatchers.Default).launch {
            val child1 = launch {
                delay(100)
                child1Executed = true
            }
            val child2 = launch {
                delay(100)
                child2Executed = true
            }
            delay(10)
            cancel() // Cancel parent
        }

        parentJob.join()

        // Assert
        assertTrue(!child1Executed, "Child 1 should be cancelled")
        assertTrue(!child2Executed, "Child 2 should be cancelled")
    }

    // ─── TIMEOUT TESTS ───

    @Test
    fun `withTimeoutOrNull returns null on timeout`() {
        // Arrange
        suspend fun longRunningTask(): String {
            delay(1000)
            return "completed"
        }

        // Act
        val result = withTimeoutOrNull(100) {
            longRunningTask()
        }

        // Assert
        assertEquals(null, result, "Should return null on timeout")
    }

    @Test
    fun `withTimeout throws exception on timeout`() {
        // Arrange
        suspend fun longRunningTask(): String {
            delay(1000)
            return "completed"
        }

        // Act
        val exception = try {
            withTimeout(100) {
                longRunningTask()
            }
            null
        } catch (e: TimeoutCancellationException) {
            e
        }

        // Assert
        assertNotNull(exception, "Should throw TimeoutCancellationException")
    }

    @Test
    fun `task completes before timeout`() {
        // Arrange
        suspend fun quickTask(): String {
            delay(50)
            return "completed"
        }

        // Act
        val result = withTimeout(1000) {
            quickTask()
        }

        // Assert
        assertEquals("completed", result)
    }

    // ─── CHANNEL TESTS ───

    @Test
    fun `channel buffers and delivers messages`() {
        // Arrange
        val channel = Channel<Int>(capacity = 10)

        // Act
        val producerJob = CoroutineScope(Dispatchers.Default).launch {
            repeat(5) { i ->
                channel.send(i)
            }
            channel.close()
        }

        val consumerJob = CoroutineScope(Dispatchers.Default).launch {
            val received = mutableListOf<Int>()
            for (value in channel) {
                received.add(value)
            }
            assertEquals(listOf(0, 1, 2, 3, 4), received)
        }

        producerJob.join()
        consumerJob.join()
    }

    @Test
    fun `channel handles backpressure`() {
        // Arrange
        val channel = Channel<Int>(capacity = 2) // Small buffer

        // Act
        val producerJob = CoroutineScope(Dispatchers.Default).launch {
            repeat(10) { i ->
                channel.send(i) // Will block when buffer is full
            }
            channel.close()
        }

        val received = mutableListOf<Int>()
        val consumerJob = CoroutineScope(Dispatchers.Default).launch {
            for (value in channel) {
                received.add(value)
                delay(10) // Slow consumer
            }
        }

        producerJob.join()
        consumerJob.join()

        // Assert
        assertEquals(10, received.size)
    }

    // ─── DEADLOCK PREVENTION TESTS ───

    @Test
    fun `proper lock ordering prevents deadlock`() {
        // Arrange
        val mutex1 = Mutex()
        val mutex2 = Mutex()
        var deadlockDetected = false

        // Act - Always acquire locks in the same order
        val job1 = CoroutineScope(Dispatchers.Default).launch {
            mutex1.withLock {
                delay(10)
                mutex2.withLock {
                    // Critical section
                }
            }
        }

        val job2 = CoroutineScope(Dispatchers.Default).launch {
            mutex1.withLock { // Same order as job1
                delay(10)
                mutex2.withLock {
                    // Critical section
                }
            }
        }

        try {
            withTimeout(1000) {
                job1.join()
                job2.join()
            }
        } catch (e: TimeoutCancellationException) {
            deadlockDetected = true
        }

        // Assert
        assertTrue(!deadlockDetected, "Proper lock ordering should prevent deadlock")
    }

    @Test
    fun `incorrect lock ordering can cause deadlock`() {
        // Arrange
        val mutex1 = Mutex()
        val mutex2 = Mutex()
        var deadlockDetected = false

        // Act - Acquire locks in different order (potential deadlock)
        val job1 = CoroutineScope(Dispatchers.Default).launch {
            mutex1.withLock {
                delay(10)
                mutex2.withLock {
                    // Critical section
                }
            }
        }

        val job2 = CoroutineScope(Dispatchers.Default).launch {
            mutex2.withLock { // Different order than job1
                delay(10)
                mutex1.withLock {
                    // Critical section
                }
            }
        }

        try {
            withTimeout(100) {
                job1.join()
                job2.join()
            }
        } catch (e: TimeoutCancellationException) {
            deadlockDetected = true
        }

        // Assert
        assertTrue(deadlockDetected, "Incorrect lock ordering should cause deadlock")
    }

    // ─── MEMORY CONSISTENCY TESTS ───

    @Test
    fun `volatile ensures visibility across threads`() {
        // Arrange
        @Volatile
        var volatileFlag = false
        var regularFlag = false

        // Act
        val setterJob = CoroutineScope(Dispatchers.Default).launch {
            delay(100)
            volatileFlag = true
            regularFlag = true
        }

        val checkerJob = CoroutineScope(Dispatchers.Default).launch {
            while (!volatileFlag) {
                // Wait for volatile flag
            }
            // At this point, regularFlag should also be visible
            // (though not guaranteed without volatile)
        }

        setterJob.join()
        checkerJob.join()

        // Assert
        assertTrue(volatileFlag, "Volatile flag should be set")
    }

    // ─── STRUCTURED CONCURRENCY TESTS ───

    @Test
    fun `structured concurrency ensures child completion`() {
        // Arrange
        var childCompleted = false

        // Act
        runBlocking {
            launch {
                delay(100)
                childCompleted = true
            }
        }

        // Assert
        assertTrue(childCompleted, "Child coroutine should complete before parent returns")
    }

    @Test
    fun `structured concurrency propagates exceptions`() {
        // Arrange
        var exceptionCaught = false

        // Act
        try {
            runBlocking {
                launch {
                    delay(10)
                    throw RuntimeException("Child failed")
                }
            }
        } catch (e: RuntimeException) {
            exceptionCaught = true
        }

        // Assert
        assertTrue(exceptionCaught, "Exception should propagate to parent")
    }

    // ─── DISPATCHER TESTS ───

    @Test
    fun `IO dispatcher is suitable for blocking operations`() {
        // Arrange
        var threadName = ""

        // Act
        runBlocking(Dispatchers.IO) {
            threadName = Thread.currentThread().name
        }

        // Assert
        assertTrue(threadName.contains("DefaultDispatcher") || threadName.contains("IO"), 
                   "Should use IO dispatcher thread")
    }

    @Test
    fun `Main dispatcher is suitable for UI operations`() {
        // Arrange
        var threadName = ""

        // Act
        runBlocking(Dispatchers.Main) {
            threadName = Thread.currentThread().name
        }

        // Assert
        assertTrue(threadName.contains("main") || threadName.contains("Main"), 
                   "Should use main thread")
    }

    // ─── ASYNC-AWAIT TESTS ───

    @Test
    fun `async allows parallel computation with results`() {
        // Arrange
        suspend fun computeValue(x: Int): Int {
            delay(100)
            return x * 2
        }

        // Act
        val result = runBlocking {
            val deferred1 = async { computeValue(10) }
            val deferred2 = async { computeValue(20) }
            deferred1.await() + deferred2.await()
        }

        // Assert
        assertEquals(60, result) // (10 * 2) + (20 * 2) = 20 + 40 = 60
    }

    @Test
    fun `async with lazy start defers execution`() {
        // Arrange
        var executionCount = 0
        suspend fun countingTask(): Int {
            executionCount++
            delay(10)
            return executionCount
        }

        // Act
        val deferred = CoroutineScope(Dispatchers.Default).async(start = CoroutineStart.LAZY) {
            countingTask()
        }

        delay(50) // Give time to ensure lazy start hasn't executed
        assertTrue(executionCount == 0, "Lazy async should not start immediately")

        deferred.await()

        // Assert
        assertEquals(1, executionCount, "Task should execute only when awaited")
    }
}
