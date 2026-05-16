package com.mlbb.scrim.data.localization

import android.util.Log
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class TranslationManager {

    private val activeTranslators = mutableMapOf<Pair<String, String>, Translator>()

    companion object {
        private const val TAG = "TranslationManager"

        // Map app language codes to ML Kit language codes
        private val languageCodeMap = mapOf(
            "en" to TranslateLanguage.ENGLISH,
            "ar" to TranslateLanguage.ARABIC,
            "de" to TranslateLanguage.GERMAN,
            "es" to TranslateLanguage.SPANISH,
            "fr" to TranslateLanguage.FRENCH,
            "ko" to TranslateLanguage.KOREAN,
            "pt" to TranslateLanguage.PORTUGUESE,
            "ru" to TranslateLanguage.RUSSIAN,
            "tr" to TranslateLanguage.TURKISH,
            "zh" to TranslateLanguage.CHINESE
        )
    }

    fun getSupportedTargetLanguages(): List<String> {
        return languageCodeMap.keys.toList()
    }

    private fun getMlKitLanguageCode(appCode: String): String {
        return languageCodeMap[appCode] ?: TranslateLanguage.ENGLISH
    }

    suspend fun translateText(text: String, targetLanguageCode: String): String {
        if (text.isBlank()) return text
        if (targetLanguageCode == "en") return text

        return withContext(Dispatchers.IO) {
            try {
                val targetMlKitCode = getMlKitLanguageCode(targetLanguageCode)
                val options = TranslatorOptions.Builder()
                    .setSourceLanguage(TranslateLanguage.ENGLISH)
                    .setTargetLanguage(targetMlKitCode)
                    .build()

                val translator = activeTranslators.getOrPut("en" to targetLanguageCode) {
                    Translation.getClient(options)
                }

                // Ensure model is downloaded
                downloadModelIfNeeded(translator)

                // Translate
                translateWithTranslator(translator, text)
            } catch (e: Exception) {
                Log.e(TAG, "Translation failed for lang=$targetLanguageCode", e)
                text // Return original on failure
            }
        }
    }

    suspend fun translateArticle(
        title: String,
        description: String,
        content: String,
        targetLanguageCode: String
    ): Triple<String, String, String> {
        return withContext(Dispatchers.IO) {
            try {
                val translatedTitle = translateText(title, targetLanguageCode)
                val translatedDesc = translateText(description, targetLanguageCode)
                val translatedContent = translateText(content, targetLanguageCode)
                Triple(translatedTitle, translatedDesc, translatedContent)
            } catch (e: Exception) {
                Log.e(TAG, "Article translation failed", e)
                Triple(title, description, content)
            }
        }
    }

    private suspend fun downloadModelIfNeeded(translator: Translator) {
        return suspendCancellableCoroutine { continuation ->
            val conditions = DownloadConditions.Builder()
                .requireWifi()
                .build()

            translator.downloadModelIfNeeded(conditions)
                .addOnSuccessListener {
                    continuation.resume(Unit)
                }
                .addOnFailureListener { e ->
                    continuation.resumeWithException(e)
                }
        }
    }

    private suspend fun translateWithTranslator(translator: Translator, text: String): String {
        return suspendCancellableCoroutine { continuation ->
            translator.translate(text)
                .addOnSuccessListener { translatedText ->
                    continuation.resume(translatedText)
                }
                .addOnFailureListener { e ->
                    continuation.resumeWithException(e)
                }
        }
    }

    fun closeTranslators() {
        activeTranslators.values.forEach { it.close() }
        activeTranslators.clear()
    }
}
