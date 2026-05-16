package com.mlbb.scrim.data.localization

import java.util.Locale

enum class Language(
    val code: String,
    val displayName: String,
    val flag: String,
    val locale: Locale
) {
    ENGLISH("en", "English", "\uD83C\uDDFA\uD83C\uDDF8", Locale("en")),
    RUSSIAN("ru", "\u0420\u0443\u0441\u0441\u043a\u0438\u0439", "\uD83C\uDDF7\uD83C\uDDFA", Locale("ru")),
    SPANISH("es", "Espa\u00f1ol", "\uD83C\uDDEA\uD83C\uDDF8", Locale("es")),
    FRENCH("fr", "Fran\u00e7ais", "\uD83C\uDDEB\uD83C\uDDF7", Locale("fr")),
    GERMAN("de", "Deutsch", "\uD83C\uDDE9\uD83C\uDDEA", Locale("de")),
    PORTUGUESE("pt", "Portugu\u00eas", "\uD83C\uDDF5\uD83C\uDDF9", Locale("pt")),
    TURKISH("tr", "T\u00fcrk\u00e7e", "\uD83C\uDDF9\uD83C\uDDF7", Locale("tr")),
    ARABIC("ar", "\u0627\u0644\u0639\u0631\u0628\u064a\u0629", "\uD83C\uDDF8\uD83C\uDDE6", Locale("ar")),
    CHINESE("zh", "\u4e2d\u6587", "\uD83C\uDDE8\uD83C\uDDF3", Locale("zh")),
    KOREAN("ko", "\ud55c\uad6d\uc5b4", "\uD83C\uDDF0\uD83C\uDDF7", Locale("ko"));

    companion object {
        fun fromCode(code: String): Language {
            return values().find { it.code == code } ?: ENGLISH
        }
    }
}
