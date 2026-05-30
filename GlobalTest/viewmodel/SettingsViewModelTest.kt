package com.mlbb.scrim.viewmodel

import android.app.Application
import com.mlbb.scrim.data.preferences.AppSettings
import com.mlbb.scrim.data.preferences.ThemePreferences
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private lateinit var viewModel: SettingsViewModel
    private lateinit var mockApplication: Application
    private lateinit var mockAppSettings: AppSettings
    private lateinit var mockThemePreferences: ThemePreferences
    private lateinit var testDispatcher: TestDispatcher

    @Before
    fun setup() {
        mockApplication = mockk(relaxed = true)
        mockAppSettings = mockk(relaxed = true)
        mockThemePreferences = mockk(relaxed = true)
        testDispatcher = StandardTestDispatcher()

        Dispatchers.setMain(testDispatcher)

        // Mock static constructors
        mockkObject(AppSettings)
        mockkObject(ThemePreferences)
        
        every { AppSettings(any()) } returns mockAppSettings
        every { ThemePreferences(any()) } returns mockThemePreferences

        viewModel = SettingsViewModel(mockApplication)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    // ─── Initialization Tests ───

    @Test
    fun `ViewModel initializes with default values`() {
        // Arrange
        every { mockAppSettings.isDarkMode() } returns false
        every { mockAppSettings.getLanguage() } returns "en"
        every { mockAppSettings.getNotificationsEnabled() } returns true

        // Act
        val isDarkMode = viewModel.isDarkMode.value
        val language = viewModel.language.value
        val notificationsEnabled = viewModel.notificationsEnabled.value

        // Assert
        assertFalse(isDarkMode)
        assertEquals("en", language)
        assertTrue(notificationsEnabled)
    }

    // ─── Theme Tests ───

    @Test
    fun `toggleDarkMode successfully toggles dark mode`() {
        // Arrange
        every { mockAppSettings.isDarkMode() } returns false
        every { mockAppSettings.setDarkMode(any()) } just Runs

        // Act
        viewModel.toggleDarkMode()

        // Assert
        coVerify { mockAppSettings.setDarkMode(true) }
    }

    @Test
    fun `setDarkMode successfully sets dark mode`() {
        // Arrange
        every { mockAppSettings.setDarkMode(any()) } just Runs

        // Act
        viewModel.setDarkMode(true)

        // Assert
        coVerify { mockAppSettings.setDarkMode(true) }
    }

    // ─── Language Tests ───

    @Test
    fun `setLanguage successfully sets language`() {
        // Arrange
        every { mockAppSettings.setLanguage(any()) } just Runs

        // Act
        viewModel.setLanguage("es")

        // Assert
        coVerify { mockAppSettings.setLanguage("es") }
    }

    // ─── Notification Tests ───

    @Test
    fun `toggleNotifications successfully toggles notifications`() {
        // Arrange
        every { mockAppSettings.getNotificationsEnabled() } returns true
        every { mockAppSettings.setNotificationsEnabled(any()) } just Runs

        // Act
        viewModel.toggleNotifications()

        // Assert
        coVerify { mockAppSettings.setNotificationsEnabled(false) }
    }

    @Test
    fun `setNotificationsEnabled successfully sets notification preference`() {
        // Arrange
        every { mockAppSettings.setNotificationsEnabled(any()) } just Runs

        // Act
        viewModel.setNotificationsEnabled(false)

        // Assert
        coVerify { mockAppSettings.setNotificationsEnabled(false) }
    }

    // ─── Cache Tests ───

    @Test
    fun `clearCache successfully clears cache`() {
        // Arrange
        every { mockAppSettings.clearCache() } just Runs

        // Act
        viewModel.clearCache()

        // Assert
        coVerify { mockAppSettings.clearCache() }
    }

    // ─── Data Tests ───

    @Test
    fun `clearUserData successfully clears user data`() {
        // Arrange
        every { mockAppSettings.clearUserData() } just Runs

        // Act
        viewModel.clearUserData()

        // Assert
        coVerify { mockAppSettings.clearUserData() }
    }
}
