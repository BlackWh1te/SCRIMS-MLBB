import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("dagger.hilt.android.plugin")
}

// Conditional Firebase plugins — only apply when google-services.json is present.
// This keeps CI and fresh clones buildable without Firebase credentials.
val hasGoogleServicesJson = file("google-services.json").exists()
    || file("src/debug/google-services.json").exists()
    || file("src/release/google-services.json").exists()
if (hasGoogleServicesJson) {
    apply(plugin = "com.google.gms.google-services")
    apply(plugin = "com.google.firebase.crashlytics")
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}

val supabaseUrl = localProperties.getProperty("SUPABASE_URL")
    ?: throw GradleException("SUPABASE_URL not found. Create local.properties with SUPABASE_URL=...")
val supabaseKey = localProperties.getProperty("SUPABASE_ANON_KEY")
    ?: throw GradleException("SUPABASE_ANON_KEY not found. Create local.properties with SUPABASE_ANON_KEY=...")

val newsApiKey = localProperties.getProperty("NEWSAPI_KEY") ?: "\"\""
val xBearerToken = localProperties.getProperty("X_BEARER_TOKEN") ?: "\"\""
val newsServiceApiKey = localProperties.getProperty("NEWS_SERVICE_API_KEY") ?: "\"mlbb-news-secret-2024\""

// Release signing config (reads from local.properties or environment variables)
val keystorePath = localProperties.getProperty("KEYSTORE_PATH") ?: System.getenv("KEYSTORE_PATH")
val keystorePassword = localProperties.getProperty("KEYSTORE_PASSWORD") ?: System.getenv("KEYSTORE_PASSWORD")
val keyAlias = localProperties.getProperty("KEY_ALIAS") ?: System.getenv("KEY_ALIAS")
val keyPassword = localProperties.getProperty("KEY_PASSWORD") ?: System.getenv("KEY_PASSWORD")

android {
    namespace = "com.mlbb.scrim"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.mlbb.scrim"
        minSdk = 24
        targetSdk = 34
        versionCode = 2
        versionName = "1.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        buildConfigField("String", "SUPABASE_URL", supabaseUrl)
        buildConfigField("String", "SUPABASE_ANON_KEY", supabaseKey)
        buildConfigField("String", "NEWSAPI_KEY", newsApiKey)
        buildConfigField("String", "X_BEARER_TOKEN", xBearerToken)
        buildConfigField("String", "NEWS_SERVICE_API_KEY", newsServiceApiKey)
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "21"
    }

    signingConfigs {
        create("release") {
            if (keystorePath != null && keystorePassword != null && keyAlias != null && keyPassword != null) {
                storeFile = file(keystorePath)
                storePassword = keystorePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            signingConfig = signingConfigs.findByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Enable code obfuscation
            isJniDebuggable = false
        }
        debug {
            isMinifyEnabled = false
            isDebuggable = true
            applicationIdSuffix = ".debug"
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.15"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
    // Core Android
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.activity:activity-compose:1.8.1")

    // Jetpack Compose
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.5")

    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.6.2")

    // Image loading
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Networking
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Logging
    implementation("com.jakewharton.timber:timber:5.0.1")

    // ML Kit Translation (on-device)
    implementation("com.google.mlkit:translate:17.0.3")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Encrypted SharedPreferences
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Room
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // Dagger Hilt
    implementation("com.google.dagger:hilt-android:2.51.1")
    ksp("com.google.dagger:hilt-compiler:2.51.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Firebase Crashlytics (requires google-services.json — see MIGRATION.md)
    implementation(platform("com.google.firebase:firebase-bom:33.1.0"))
    implementation("com.google.firebase:firebase-crashlytics-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("io.mockk:mockk:1.13.9")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.10.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}