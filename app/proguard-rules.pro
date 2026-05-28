# ============================================
# MLBB Scrim Host - Enhanced Security ProGuard Rules
# ============================================

# --- OBFUSCATION SETTINGS ---

# Enable aggressive obfuscation
-repackageclasses ''
-allowaccessmodification
-mergeinterfacesaggressively

# String encryption (optional, requires external tool)
# -adaptresourcefilenames
# -adaptresourcefilecontents

# --- OPTIMIZATION SETTINGS ---

# Maximum optimization
-optimizationpasses 5
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeInvisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations
-keepattributes RuntimeInvisibleParameterAnnotations
-keepattributes SourceFile
-keepattributes LineNumberTable

-dontpreverify
-verbose

# --- SECURITY: REMOVE DEBUGGING INFO ---

# Remove all logging in release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}

# Remove stack traces in release
-assumenosideeffects class java.lang.StackTraceElement {
    public java.lang.String getFileName();
    public int getLineNumber();
    public java.lang.String getMethodName();
    public boolean isNativeMethod();
}

# Remove asserts
-assumenosideeffects class java.lang.AssertionError {
    public <init>(java.lang.Object);
}

# --- KOTTON SPECIFIC ---

# Keep Kotlin metadata
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Keep Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }

# Keep Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *; }

# --- JETPACK COMPOSE ---

# Keep Compose (required for Compose to work)
-keep class androidx.compose.** { *; }
-keep class androidx.compose.runtime.** { *; }
-keepclassmembers class * extends androidx.compose.ui.node.ComposeNodeLifecycleCallback { *; }

# Keep Navigation Compose
-keep class androidx.navigation.** { *; }
-keep class * extends androidx.navigation.Navigator { *; }

# Keep ViewModels
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keep class * extends androidx.lifecycle.AndroidViewModel { *; }

# Keep DataStore
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite { *; }

# --- THIRD-PARTY LIBRARIES ---

# Coil image loading
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule { <init>(...); }

# Retrofit
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions

# OkHttp
-dontwarn okhttp3.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# ML Kit
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# --- SECURITY CLASS PROTECTION ---

# Obfuscate security class but keep essential methods
-keep class com.mlbb.scrim.security.** {
    public static boolean isRooted();
    public static boolean isDebuggerAttached();
    public static boolean isFridaDetected();
    public static boolean performSecurityCheck(android.content.Context);
}

# But obfuscate the implementation details
-keepclassmembers class com.mlbb.scrim.security.** {
    private *;
}

# --- NATIVE CODE PROTECTION ---

# If you add native code later, protect it
# -keepclasseswithmembernames class * {
#     native <methods>;
# }

# --- DATA MODELS & RETROFIT DTOs ---
# Keep all data models used for UI and DB
-keep class com.mlbb.scrim.data.model.** {
    <fields>;
    <init>(...);
}

# Keep all request/response data classes used by Retrofit/Gson/Supabase
-keep class com.mlbb.scrim.data.service.** {
    <fields>;
    <init>(...);
}

# Ensure SerializedName is respected if present, but keep all fields anyway
-keepclassmembers class com.mlbb.scrim.data.model.** {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keepclassmembers class com.mlbb.scrim.data.service.** {
    @com.google.gson.annotations.SerializedName <fields>;
}

# --- REFLECTION PROTECTION ---

# Minimize reflection exposure
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# --- FINAL OPTIMIZATION ---

# Don't optimize for size (security over size)
-dontoptimize

# Keep line numbers for crash reporting (optional)
# -keepattributes SourceFile,LineNumberTable
