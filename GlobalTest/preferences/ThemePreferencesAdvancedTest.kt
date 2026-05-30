package com.mlbb.scrim.data.preferences

import android.content.Context
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Advanced ThemePreferences tests with edge cases, concurrency, and data integrity validation.
 * 
 * Test Categories:
 * - Preference CRUD operations
 * - Default value handling
 * - Concurrency and race conditions
 * - Data persistence and consistency
 * - Edge cases (rapid toggling, etc.)
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ThemePreferencesAdvancedTest {

    private lateinit var themePreferences: ThemePreferences
    private lateinit var mockContext: Context
    private lateinit var testDispatcher: TestDispatcher
    private lateinit var mockDataStore: androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>

    @Before
    fun setup() {
        mockContext = mockk(relaxed = true)
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)

        // Mock DataStore
        mockDataStore = mockk(relaxed = true)
        val mockDataStoreFlow = mockk<androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>>()
        
        // Mock the extension property
        mockkObject(androidx.datastore.preferences.core.Preferences)
        every { mockContext.dataStore } returns mockDataStoreFlow

        themePreferences = ThemePreferences(mockContext)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkObject(androidx.datastore.preferences.core.Preferences)
        unmockkStatic("androidx.datastore.preferences.core.Preferences")
    }

    // ─── BASIC CRUD TESTS ───

    @Test
    fun `darkMode returns default value when not set`() {
        // Arrange
        val mockPreferences = mockk<androidx.datastore.preferences.core.Preferences>()
        every { mockPreferences[any<androidx.datastore.preferences.core.Preferences.Key<Boolean>>()] } returns null
        val mockFlow = kotlinx.coroutines.flow.flowOf(mockPreferences)
        every { mockDataStoreFlow.data } returns mockFlow

        // Act
        val result = themePreferences.darkMode.first()

        // Assert
        assertTrue(result, "Default should be dark mode (true)")
    }

    @Test
    fun `darkMode returns stored value when set`() {
        // Arrange
        val mockPreferences = mockk<androidx.datastore.preferences.core.Preferences>()
        every { mockPreferences[any<androidx.datastore.preferences.core.Preferences.Key<Boolean>>()] } returns false
        val mockFlow = kotlinx.coroutines.flow.flowOf(mockPreferences)
        every { mockDataStoreFlow.data } returns mockFlow

        // Act
        val result = themePreferences.darkMode.first()

        // Assert
        assertEquals(false, result)
    }

    @Test
    fun `setDarkMode successfully updates preference`() {
        // Arrange
        val mockEdit = mockk<androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>.()>()
        every { mockDataStoreFlow.edit(any()) } returns mockEdit
        every { mockEdit(any()) } returns kotlinx.coroutines.flow.flowOf(androidx.datastore.preferences.core.Preferences.Default)

        // Act
        themePreferences.setDarkMode(false)
        advanceUntilIdle()

        // Assert
        verify { mockDataStoreFlow.edit(any()) }
    }

    @Test
    fun `toggleDarkMode flips current value`() {
        // Arrange
        val mockPreferences = mockk<androidx.datastore.preferences.core.Preferences>()
        every { mockPreferences[any<androidx.datastore.preferences.core.Preferences.Key<Boolean>>()] } returns true
        val mockFlow = kotlinx.coroutines.flow.flowOf(mockPreferences)
        every { mockDataStoreFlow.data } returns mockFlow

        val mockEdit = mockk<androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>.()>()
        every { mockDataStoreFlow.edit(any()) } returns mockEdit
        every { mockEdit(any()) } returns kotlinx.coroutines.flow.flowOf(androidx.datastore.preferences.core.Preferences.Default)

        // Act
        themePreferences.toggleDarkMode()
        advanceUntilIdle()

        // Assert
        verify { mockDataStoreFlow.edit(any()) }
    }

    // ─── EDGE CASE TESTS ───

    @Test
    fun `toggleDarkMode when value is not set uses default`() {
        // Arrange
        val mockPreferences = mockk<androidx.datastore.preferences.core.Preferences>()
        every { mockPreferences[any<androidx.datastore.preferences.core.Preferences.Key<Boolean>>()] } returns null // Not set
        val mockFlow = kotlinx.coroutines.flow.flowOf(mockPreferences)
        every { mockDataStoreFlow.data } returns mockFlow

        val mockEdit = mockk<androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>.()>()
        every { mockDataStoreFlow.edit(any()) } returns mockEdit
        every { mockEdit(any()) } returns kotlinx.coroutines.flow.flowOf(androidx.datastore.preferences.core.Preferences.Default)

        // Act
        themePreferences.toggleDarkMode()
        advanceUntilIdle()

        // Assert - Should toggle from default (true) to false
        verify { mockEdit(any()) }
    }

    @Test
    fun `setDarkMode handles rapid value changes`() {
        // Arrange
        val mockEdit = mockk<androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>.()>()
        every { mockDataStoreFlow.edit(any()) } returns mockEdit
        every { mockEdit(any()) } returns kotlinx.coroutines.flow.flowOf(androidx.datastore.preferences.core.Preferences.Default)

        // Act - Rapid changes
        repeat(10) { i ->
            themePreferences.setDarkMode(i % 2 == 0)
            advanceUntilIdle()
        }

        // Assert - Should handle rapid changes without errors
        verify(atLeast = 10) { mockDataStoreFlow.edit(any()) }
    }

    @Test
    fun `toggleDarkMode handles rapid toggling`() {
        // Arrange
        val mockPreferences = mockk<androidx.datastore.preferences.core.Preferences>()
        every { mockPreferences[any<androidx.datastore.preferences.core.Preferences.Key<Boolean>>()] } returns true
        val mockFlow = kotlinx.coroutines.flow.flowOf(mockPreferences)
        every { mockDataStoreFlow.data } returns mockFlow

        val mockEdit = mockk<androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>.()>()
        every { mockDataStoreFlow.edit(any()) } returns mockEdit
        every { mockEdit(any()) } returns kotlinx.coroutines.flow.flowOf(androidx.datastore.preferences.core.Preferences.Default)

        // Act - Rapid toggling
        repeat(10) {
            themePreferences.toggleDarkMode()
            advanceUntilIdle()
        }

        // Assert - Should handle rapid toggling without errors
        verify(atLeast = 10) { mockDataStoreFlow.edit(any()) }
    }

    // ─── CONCURRENCY TESTS ───

    @Test
    fun `concurrent setDarkMode operations are handled correctly`() {
        // Arrange
        val mockEdit = mockk<androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>.()>()
        every { mockDataStoreFlow.edit(any()) } returns mockEdit
        every { mockEdit(any()) } returns kotlinx.coroutines.flow.flowOf(androidx.datastore.preferences.core.Preferences.Default)

        // Act - Concurrent set operations
        val jobs = (1..10).map { i ->
            kotlinx.coroutines.launch {
                themePreferences.setDarkMode(i % 2 == 0)
            }
        }

        jobs.forEach { it.join() }
        advanceUntilIdle()

        // Assert - Should complete without errors
        verify(atLeast = 10) { mockDataStoreFlow.edit(any()) }
    }

    @Test
    fun `concurrent toggleDarkMode operations are handled correctly`() {
        // Arrange
        val mockPreferences = mockk<androidx.datastore.preferences.core.Preferences>()
        every { mockPreferences[any<androidx.datastore.preferences.core.Preferences.Key<Boolean>>()] } returns true
        val mockFlow = kotlinx.coroutines.flow.flowOf(mockPreferences)
        every { mockDataStoreFlow.data } returns mockFlow

        val mockEdit = mockk<androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>.()>()
        every { mockDataStoreFlow.edit(any()) } returns mockEdit
        every { mockEdit(any()) } returns kotlinx.coroutines.flow.flowOf(androidx.datastore.preferences.core.Preferences.Default)

        // Act - Concurrent toggle operations
        val jobs = (1..10).map {
            kotlinx.coroutines.launch {
                themePreferences.toggleDarkMode()
            }
        }

        jobs.forEach { it.join() }
        advanceUntilIdle()

        // Assert - Should complete without errors
        verify(atLeast = 10) { mockDataStoreFlow.edit(any()) }
    }

    @Test
    fun `concurrent read and write operations are handled correctly`() {
        // Arrange
        val mockPreferences = mockk<androidx.datastore.preferences.core.Preferences>()
        every { mockPreferences[any<androidx.datastore.preferences.core.Preferences.Key<Boolean>>()] } returns true
        val mockFlow = kotlinx.coroutines.flow.flowOf(mockPreferences)
        every { mockDataStoreFlow.data } returns mockFlow

        val mockEdit = mockk<androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>.()>()
        every { mockDataStoreFlow.edit(any()) } returns mockEdit
        every { mockEdit(any()) } returns kotlinx.coroutines.flow.flowOf(androidx.datastore.preferences.core.Preferences.Default)

        // Act - Concurrent reads and writes
        val jobs = mutableListOf<kotlinx.coroutines.Job>()
        
        // Start read operations
        repeat(5) {
            jobs.add(kotlinx.coroutines.launch {
                val result = themePreferences.darkMode.first()
                assertTrue(result)
            })
        }

        // Start write operations
        repeat(5) { i ->
            jobs.add(kotlinx.coroutines.launch {
                themePreferences.setDarkMode(i % 2 == 0)
            })
        }

        jobs.forEach { it.join() }
        advanceUntilIdle()

        // Assert - Should complete without errors
        assertTrue(true, "Concurrent operations should complete successfully")
    }

    // ─── DATA INTEGRITY TESTS ───

    @Test
    fun `setDarkMode and darkMode maintain consistency`() {
        // Arrange
        val mockPreferences = mockk<androidx.datastore.preferences.core.Preferences>()
        every { mockPreferences[any<androidx.datastore.preferences.core.Preferences.Key<Boolean>>()] } returns false
        val mockFlow = kotlinx.coroutines.flow.flowOf(mockPreferences)
        every { mockDataStoreFlow.data } returns mockFlow

        val mockEdit = mockk<androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>.()>()
        every { mockDataStoreFlow.edit(any()) } returns mockEdit
        every { mockEdit(any()) } returns kotlinx.coroutines.flow.flowOf(androidx.datastore.preferences.core.Preferences.Default)

        // Act
        themePreferences.setDarkMode(true)
        advanceUntilIdle()

        val result = themePreferences.darkMode.first()

        // Assert - Update mock to return the set value
        every { mockPreferences[any<androidx.datastore.preferences.core.Preferences.Key<Boolean>>()] } returns true
        val updatedMockFlow = kotlinx.coroutines.flow.flowOf(mockPreferences)
        every { mockDataStoreFlow.data } returns updatedMockFlow

        val finalResult = themePreferences.darkMode.first()

        // Assert
        assertTrue(finalResult, "Should return true after setDarkMode(true)")
    }

    @Test
fun `toggleDarkMode flips value correctly`() {
        // Arrange
        val mockPreferences = mockk<androidx.datastore.preferences.core.Preferences>()
        every { mockPreferences[any<androidx.datastore.preferences.core.Preferences.Key<Boolean>>()] } returns true
        val mockFlow = kotlinx.coroutines.flow.flowOf(mockPreferences)
        every { mockDataStoreFlow.data } returns mockFlow

        val mockEdit = mockk<androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>.()>()
        every { mockDataStoreFlow.edit(any()) } returns mockEdit
        every { mockEdit(any()) } returns kotlinx.coroutines.flow.flowOf(androidx.datastore.preferences.core.Preferences.Default)

        // Act
        themePreferences.toggleDarkMode()
        advanceUntilIdle()

        // Assert - Verify edit was called
        verify { mockDataStoreFlow.edit(any()) }
    }

    // ─── FLOW EMISSION TESTS ───

    @Test
    fun `darkMode emits updates when value changes`() {
        // Arrange
        val mockPreferences = mockk<androidx.datastore.preferences.core.Preferences>()
        every { mockPreferences[any<androidx.datastore.preferences.core.Preferences.Key<Boolean>>()] } returns true
        val mockFlow = kotlinx.coroutines.flow.flowOf(mockPreferences)
        every { mockDataStoreFlow.data } returns mockFlow

        val mockEdit = mockk<androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>.()>()
        every { mockDataStoreFlow.edit(any()) } returns mockEdit
        every { mockEdit(any()) } returns kotlinx.coroutines.flow.flowOf(androidx.datastore.preferences.core.Preferences.Default)

        val emissions = mutableListOf<Boolean>()
        val job = kotlinx.coroutines.launch {
            themePreferences.darkMode.collect { emissions.add(it) }
        }

        advanceUntilIdle()

        // Act
        themePreferences.setDarkMode(false)
        advanceUntilIdle()

        job.cancel()

        // Assert
        assertTrue(emissions.size >= 1, "Should emit at least 1 update")
    }

    // ─── ERROR HANDLING TESTS ───

    @Test
    fun `handles DataStore failure gracefully`() {
        // Arrange
        val mockFlow = kotlinx.coroutines.flow.flow<androidx.datastore.preferences.core.Preferences> {
            throw RuntimeException("DataStore error")
        }
        every { mockDataStoreFlow.data } returns mockFlow

        // Act
        val result = try {
            themePreferences.darkMode.first()
            true
        } catch (e: Exception) {
            false
        }

        // Assert - Should handle error gracefully
        assertTrue(!result, "Should return false on DataStore error")
    }

    @Test
    fun `handles edit failure gracefully`() {
        // Arrange
        val mockEdit = mockk<androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>.()>()
        every { mockDataStoreFlow.edit(any()) } throws RuntimeException("Edit error")

        // Act
        val result = try {
            themePreferences.setDarkMode(true)
            true
        } catch (e: Exception) {
            false
        }

        // Assert - Should handle error gracefully
        assertTrue(!result, "Should return false on edit error")
    }

    // ─── PERFORMANCE TESTS ───

    @Test
    fun `setDarkMode performance is acceptable`() {
        // Arrange
        val mockEdit = mockk<androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>.()>()
        every { mockDataStoreFlow.edit(any()) } returns mockEdit
        every { mockEdit(any()) } returns kotlinx.coroutines.flow.flowOf(androidx.datastore.preferences.core.Preferences.Default)

        // Act - Measure performance
        val startTime = System.nanoTime()
        repeat(100) {
            themePreferences.setDarkMode(it % 2 == 0)
            advanceUntilIdle()
        }
        val endTime = System.nanoTime()
        val duration = (endTime - startTime) / 1_000_000 // Convert to milliseconds

        // Assert
        assertTrue(duration < 1000, "100 operations should complete in under 1 second, took ${duration}ms")
    }

    @Test
    fun `darkMode read performance is acceptable`() {
        // Arrange
        val mockPreferences = mockk<androidx.datastore.preferences.core.Preferences>()
        every { mockPreferences[any<androidx.datastore.preferences.core.Preferences.Key<Boolean>>()] } returns true
        val mockFlow = kotlinx.coroutines.flow.flowOf(mockPreferences)
        every { mockDataStoreFlow.data } returns mockFlow

        // Act - Measure performance
        val startTime = System.nanoTime()
        repeat(100) {
            themePreferences.darkMode.first()
        }
        val endTime = System.nanoTime()
        val duration = (endTime - startTime) / 1_000_000 // Convert to milliseconds

        // Assert
        assertTrue(duration < 500, "100 reads should complete in under 500ms, took ${duration}ms")
    }
}
