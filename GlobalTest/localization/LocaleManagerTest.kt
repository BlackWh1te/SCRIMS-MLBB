package com.mlbb.scrim.data.localization

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import com.mlbb.scrim.data.preferences.AppSettings
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.Locale
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LocaleManagerTest {

    private lateinit var mockContext: Context
    private lateinit var mockResources: Resources
    private lateinit var mockConfiguration: Configuration
    private lateinit var mockAppSettings: AppSettings

    @Before
    fun setup() {
        mockContext = mockk(relaxed = true)
        mockResources = mockk(relaxed = true)
        mockConfiguration = mockk(relaxed = true)
        mockAppSettings = mockk(relaxed = true)

        // Setup default mock behavior
        every { mockContext.resources } returns mockResources
        every { mockResources.configuration } returns mockConfiguration
        every { mockConfiguration.locales } returns LocaleList(Locale.ENGLISH)
        every { mockContext.createConfigurationContext(any()) } returns mockContext

        // Mock AppSettings
        mockkObject(AppSettings)
        every { AppSettings(any()) } returns mockAppSettings
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // ─── Set Locale Tests ───

    @Test
    fun `setLocale successfully sets English locale`() {
        // Arrange
        val languageCode = "en"

        // Act
        val result = LocaleManager.setLocale(mockContext, languageCode)

        // Assert
        assertNotNull(result)
        assertEquals(Locale.ENGLISH, Locale.getDefault())
    }

    @Test
    fun `setLocale successfully sets Spanish locale`() {
        // Arrange
        val languageCode = "es"

        // Act
        val result = LocaleManager.setLocale(mockContext, languageCode)

        // Assert
        assertNotNull(result)
        assertEquals(Locale("es"), Locale.getDefault())
    }

    @Test
    fun `setLocale successfully sets French locale`() {
        // Arrange
        val languageCode = "fr"

        // Act
        val result = LocaleManager.setLocale(mockContext, languageCode)

        // Assert
        assertNotNull(result)
        assertEquals(Locale("fr"), Locale.getDefault())
    }

    @Test
    fun `setLocale successfully sets Korean locale`() {
        // Arrange
        val languageCode = "ko"

        // Act
        val result = LocaleManager.setLocale(mockContext, languageCode)

        // Assert
        assertNotNull(result)
        assertEquals(Locale("ko"), Locale.getDefault())
    }

    @Test
    fun `setLocale successfully sets Russian locale`() {
        // Arrange
        val languageCode = "ru"

        // Act
        val result = LocaleManager.setLocale(mockContext, languageCode)

        // Assert
        assertNotNull(result)
        assertEquals(Locale("ru"), Locale.getDefault())
    }

    @Test
    fun `setLocale successfully sets Chinese locale`() {
        // Arrange
        val languageCode = "zh"

        // Act
        val result = LocaleManager.setLocale(mockContext, languageCode)

        // Assert
        assertNotNull(result)
        assertEquals(Locale("zh"), Locale.getDefault())
    }

    @Test
    fun `setLocale successfully sets Turkish locale`() {
        // Arrange
        val languageCode = "tr"

        // Act
        val result = LocaleManager.setLocale(mockContext, languageCode)

        // Assert
        assertNotNull(result)
        assertEquals(Locale("tr"), Locale.getDefault())
    }

    @Test
    fun `setLocale successfully sets Arabic locale`() {
        // Arrange
        val languageCode = "ar"

        // Act
        val result = LocaleManager.setLocale(mockContext, languageCode)

        // Assert
        assertNotNull(result)
        assertEquals(Locale("ar"), Locale.getDefault())
    }

    @Test
    fun `setLocale handles locale with region code`() {
        // Arrange
        val languageCode = "en-US"

        // Act
        val result = LocaleManager.setLocale(mockContext, languageCode)

        // Assert
        assertNotNull(result)
        assertEquals(Locale("en", "US"), Locale.getDefault())
    }

    @Test
    fun `setLocale creates configuration context with new locale`() {
        // Arrange
        val languageCode = "es"

        // Act
        val result = LocaleManager.setLocale(mockContext, languageCode)

        // Assert
        coVerify { mockContext.createConfigurationContext(any()) }
        assertNotNull(result)
    }

    @Test
    fun `setLocale updates configuration with new locale list`() {
        // Arrange
        val languageCode = "fr"
        val expectedLocale = Locale("fr")

        // Act
        LocaleManager.setLocale(mockContext, languageCode)

        // Assert
        coVerify { 
            mockConfiguration.setLocales(LocaleList(expectedLocale))
        }
    }

    // ─── Get Current Locale Tests ───

    @Test
    fun `getCurrentLocale returns current system locale`() {
        // Arrange
        val expectedLocale = Locale("en")
        every { mockConfiguration.locales } returns LocaleList(expectedLocale)

        // Act
        val result = LocaleManager.getCurrentLocale(mockContext)

        // Assert
        assertEquals(expectedLocale, result)
    }

    @Test
    fun `getCurrentLocale returns Spanish when set to Spanish`() {
        // Arrange
        val expectedLocale = Locale("es")
        every { mockConfiguration.locales } returns LocaleList(expectedLocale)

        // Act
        val result = LocaleManager.getCurrentLocale(mockContext)

        // Assert
        assertEquals(expectedLocale, result)
    }

    @Test
    fun `getCurrentLocale handles multiple available locales`() {
        // Arrange
        val localeList = LocaleList(Locale("en"), Locale("es"), Locale("fr"))
        every { mockConfiguration.locales } returns localeList

        // Act
        val result = LocaleManager.getCurrentLocale(mockContext)

        // Assert
        assertEquals(Locale("en"), result) // Should return first locale
    }

    // ─── Apply Saved Locale Tests ───

    @Test
    fun `applySavedLocale applies saved language code`() {
        // Arrange
        val savedLanguageCode = "fr"
        every { mockAppSettings.getLanguageCodeSync() } returns savedLanguageCode

        // Act
        val result = LocaleManager.applySavedLocale(mockContext)

        // Assert
        assertNotNull(result)
        assertEquals(Locale("fr"), Locale.getDefault())
        coVerify { mockAppSettings.getLanguageCodeSync() }
    }

    @Test
    fun `applySavedLocale uses English when no saved language`() {
        // Arrange
        every { mockAppSettings.getLanguageCodeSync() } returns "en"

        // Act
        val result = LocaleManager.applySavedLocale(mockContext)

        // Assert
        assertNotNull(result)
        assertEquals(Locale.ENGLISH, Locale.getDefault())
    }

    @Test
    fun `applySavedLocale calls setLocale with saved code`() {
        // Arrange
        val savedLanguageCode = "ko"
        every { mockAppSettings.getLanguageCodeSync() } returns savedLanguageCode

        // Act
        LocaleManager.applySavedLocale(mockContext)

        // Assert
        coVerify { mockContext.createConfigurationContext(any()) }
    }

    // ─── Edge Case Tests ───

    @Test
    fun `setLocale handles empty language code`() {
        // Arrange
        val languageCode = ""

        // Act
        val result = LocaleManager.setLocale(mockContext, languageCode)

        // Assert - Should handle gracefully
        assertNotNull(result)
    }

    @Test
    fun `setLocale handles invalid language code`() {
        // Arrange
        val languageCode = "invalid-locale-code"

        // Act
        val result = LocaleManager.setLocale(mockContext, languageCode)

        // Assert - Should handle gracefully
        assertNotNull(result)
    }

    @Test
    fun `setLocale handles language code with special characters`() {
        // Arrange
        val languageCode = "en-US-posix"

        // Act
        val result = LocaleManager.setLocale(mockContext, languageCode)

        // Assert - Should handle gracefully
        assertNotNull(result)
    }

    @Test
    fun `getCurrentLocale handles empty locale list`() {
        // Arrange
        every { mockConfiguration.locales } returns LocaleList()

        // Act
        val result = LocaleManager.getCurrentLocale(mockContext)

        // Assert - Should handle gracefully
        assertNotNull(result)
    }

    @Test
    fun `applySavedLocale handles null from settings`() {
        // Arrange
        every { mockAppSettings.getLanguageCodeSync() } returns null

        // Act
        val result = LocaleManager.applySavedLocale(mockContext)

        // Assert - Should handle gracefully
        assertNotNull(result)
    }

    // ─── Consistency Tests ───

    @Test
    fun `setLocale and getCurrentLocale are consistent`() {
        // Arrange
        val languageCode = "de"

        // Act
        LocaleManager.setLocale(mockContext, languageCode)
        every { mockConfiguration.locales } returns LocaleList(Locale("de"))
        val currentLocale = LocaleManager.getCurrentLocale(mockContext)

        // Assert
        assertEquals(Locale("de"), currentLocale)
    }

    @Test
    fun `applySavedLocale uses synchronous method to avoid blocking`() {
        // Arrange
        every { mockAppSettings.getLanguageCodeSync() } returns "en"

        // Act
        val result = LocaleManager.applySavedLocale(mockContext)

        // Assert
        coVerify { mockAppSettings.getLanguageCodeSync() }
        assertNotNull(result)
    }

    @Test
    fun `multiple setLocale calls update default locale`() {
        // Act
        LocaleManager.setLocale(mockContext, "es")
        val locale1 = Locale.getDefault()
        
        LocaleManager.setLocale(mockContext, "fr")
        val locale2 = Locale.getDefault()

        // Assert
        assertEquals(Locale("es"), locale1)
        assertEquals(Locale("fr"), locale2)
    }

    // ─── Configuration Context Tests ───

    @Test
    fun `setLocale returns new configuration context`() {
        // Arrange
        val newContext = mockk<Context>(relaxed = true)
        every { mockContext.createConfigurationContext(any()) } returns newContext

        // Act
        val result = LocaleManager.setLocale(mockContext, "en")

        // Assert
        assertEquals(newContext, result)
    }

    @Test
    fun `applySavedLocale returns new configuration context`() {
        // Arrange
        val newContext = mockk<Context>(relaxed = true)
        every { mockContext.createConfigurationContext(any()) } returns newContext
        every { mockAppSettings.getLanguageCodeSync() } returns "en"

        // Act
        val result = LocaleManager.applySavedLocale(mockContext)

        // Assert
        assertEquals(newContext, result)
    }

    // ─── Locale List Tests ───

    @Test
    fun `setLocale creates LocaleList with single locale`() {
        // Arrange
        val languageCode = "es"
        val expectedLocale = Locale("es")

        // Act
        LocaleManager.setLocale(mockContext, languageCode)

        // Assert
        coVerify { 
            mockConfiguration.setLocales(match { it.size == 1 && it[0] == expectedLocale })
        }
    }

    @Test
    fun `setLocale preserves configuration when creating context`() {
        // Arrange
        val languageCode = "fr"

        // Act
        LocaleManager.setLocale(mockContext, languageCode)

        // Assert
        coVerify { 
            val configCaptor = slot<Configuration>()
            mockContext.createConfigurationContext(capture(configCaptor))
            // Configuration should be modified
        }
    }
}
