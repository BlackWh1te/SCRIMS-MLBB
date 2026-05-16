package com.mlbb.scrim

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import com.mlbb.scrim.data.localization.LocaleManager
import com.mlbb.scrim.data.preferences.AppSettings
import com.mlbb.scrim.ui.navigation.AuthNavigation
import com.mlbb.scrim.ui.theme.MLBBScrimHostTheme
import com.mlbb.scrim.viewmodel.AuthViewModel

import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var lastAppliedLanguage: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lastAppliedLanguage = AppSettings(this).getLanguageCodeSync()

        setContent {
            val context = LocalContext.current
            val appSettings = remember { AppSettings(context) }
            val darkMode by appSettings.darkMode.collectAsState(initial = true)
            val languageCode by appSettings.languageCode.collectAsState(initial = lastAppliedLanguage ?: "en")

            // Auto-recreate activity when language changes so resources reload.
            LaunchedEffect(languageCode) {
                val current = lastAppliedLanguage
                if (current != null && current != languageCode) {
                    lastAppliedLanguage = languageCode
                    recreate()
                } else {
                    lastAppliedLanguage = languageCode
                }
            }

            MLBBScrimHostTheme(darkTheme = darkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val authViewModel = ViewModelProvider(
                        this@MainActivity,
                        androidx.lifecycle.SavedStateViewModelFactory(application, this@MainActivity)
                    )[AuthViewModel::class.java]
                    AuthNavigation(
                        viewModel = authViewModel,
                        context = this@MainActivity
                    )
                }
            }
        }
    }

    override fun attachBaseContext(newBase: android.content.Context) {
        val localeContext = LocaleManager.applySavedLocale(newBase)
        super.attachBaseContext(localeContext)
    }
}


