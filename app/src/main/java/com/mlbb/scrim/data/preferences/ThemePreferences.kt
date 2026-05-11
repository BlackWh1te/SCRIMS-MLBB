package com.mlbb.scrim.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Extension to create DataStore
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_preferences")

class ThemePreferences(private val context: Context) {
    
    private object PreferencesKeys {
        val DARK_MODE = booleanPreferencesKey("dark_mode")
    }
    
    val darkMode: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.DARK_MODE] ?: true // Default to dark mode
    }
    
    suspend fun setDarkMode(isDark: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DARK_MODE] = isDark
        }
    }
    
    suspend fun toggleDarkMode() {
        context.dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.DARK_MODE] ?: true
            preferences[PreferencesKeys.DARK_MODE] = !current
        }
    }
}