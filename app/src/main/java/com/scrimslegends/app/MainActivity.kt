package com.scrimslegends.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.scrimslegends.app.data.localization.LocaleManager
import com.scrimslegends.app.data.preferences.AppSettings
import com.scrimslegends.app.ui.navigation.AuthNavigation
import com.scrimslegends.app.ui.theme.ScrimsLegendsTheme
import com.scrimslegends.app.viewmodel.AuthViewModel
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.scrimslegends.app.data.service.OtpApiClient

import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var currentLanguageCode: String = "en"
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestPostNotificationsPermissionIfNeeded()

        lifecycleScope.launch {
            val settings = AppSettings(this@MainActivity)
            settings.languageCode.collect { code ->
                if (code != currentLanguageCode) {
                    currentLanguageCode = code
                    LocaleManager.setLocale(this@MainActivity, code)
                    recreate()
                }
            }
        }

        // Wake up the Render backend asynchronously
        lifecycleScope.launch {
            try {
                OtpApiClient.service.wakeUp()
            } catch (e: Exception) {
                // Ignore exceptions, this is just a ping
            }
        }

        setContent {
            val appSettings = remember { AppSettings(this@MainActivity) }
            val darkMode by appSettings.darkMode.collectAsState(initial = true)

            ScrimsLegendsTheme(darkTheme = darkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val authViewModel: AuthViewModel = androidx.hilt.navigation.compose.hiltViewModel()
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
        currentLanguageCode = AppSettings(newBase).getLanguageCodeSync()
        super.attachBaseContext(localeContext)
    }

    private fun requestPostNotificationsPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        val hasPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}


