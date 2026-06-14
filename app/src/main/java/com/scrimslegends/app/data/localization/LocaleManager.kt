package com.scrimslegends.app.data.localization

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import com.scrimslegends.app.data.preferences.AppSettings
import java.util.Locale

object LocaleManager {

    fun setLocale(context: Context, languageCode: String): Context {
        val locale = Locale("en")
        Locale.setDefault(locale)

        // P3-4 FIX: minSdk is 24 (N), so we can use LocaleList directly without version checks.
        val config = Configuration(context.resources.configuration)
        config.setLocales(LocaleList(locale))
        return context.createConfigurationContext(config)
    }

    fun getCurrentLocale(context: Context): Locale {
        // P3-4 FIX: minSdk is 24 (N), so locales API is always available.
        return context.resources.configuration.locales[0]
    }

    fun applySavedLocale(context: Context): Context {
        val settings = AppSettings(context)
        // Use synchronous method to avoid blocking the main thread during attachBaseContext
        val code = settings.getLanguageCodeSync()
        return setLocale(context, code)
    }
}
