package com.example.util

import android.app.LocaleManager
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import java.util.Locale

/**
 * Supported app languages.
 */
enum class AppLanguage(val code: String, val nativeName: String) {
    SYSTEM("system", "System default"),
    ENGLISH("en", "English"),
    SPANISH("es", "Español"),
    ITALIAN("it", "Italiano"),
    GALICIAN("gl", "Galego"),
    GERMAN("de", "Deutsch");

    companion object {
        val SUPPORTED_CODES = setOf("en", "es", "it", "gl", "de")

        fun fromCode(code: String?): AppLanguage {
            return entries.find { it.code.equals(code, ignoreCase = true) } ?: SYSTEM
        }

        /**
         * Resolves the effective language code.
         * If 'system' is selected, inspects the device system locale.
         * If system locale language is one of [en, es, it, gl, de], returns it.
         * Otherwise, defaults to 'en' (English).
         */
        fun resolveEffectiveLocaleCode(savedPreference: String, systemLocale: Locale): String {
            if (savedPreference != "system" && SUPPORTED_CODES.contains(savedPreference)) {
                return savedPreference
            }
            // Check system language
            val sysLang = systemLocale.language.lowercase()
            return if (SUPPORTED_CODES.contains(sysLang)) {
                sysLang
            } else {
                "en" // Fallback to English
            }
        }
    }
}

object LocaleHelper {

    const val PREFS_NAME = "photo_star_sorter_locale_prefs"
    const val KEY_LANGUAGE = "app_language"

    /**
     * Retrieves the stored AppLanguage preference (or SYSTEM by default).
     */
    fun getSavedAppLanguage(context: Context): AppLanguage {
        val prefs = getPrefs(context)
        val code = prefs.getString(KEY_LANGUAGE, AppLanguage.SYSTEM.code)
        return AppLanguage.fromCode(code)
    }

    fun getSavedLanguageCode(context: Context): String {
        return getSavedAppLanguage(context).code
    }

    /**
     * Resolves the effective language tag (e.g. "en", "es", "it", "gl", "de").
     */
    fun getEffectiveLanguageCode(context: Context): String {
        val savedLang = getSavedAppLanguage(context)
        val systemLocale = getDeviceSystemLocale(context)
        return AppLanguage.resolveEffectiveLocaleCode(savedLang.code, systemLocale)
    }

    /**
     * Persists and applies the selected language.
     */
    fun setAppLanguage(context: Context, language: AppLanguage) {
        val prefs = getPrefs(context)
        prefs.edit().putString(KEY_LANGUAGE, language.code).apply()
        applyLocale(context, language.code)
    }

    /**
     * Applies the locale to the system per-app language APIs (API 33+)
     * and updates runtime resources configuration.
     */
    fun applyLocale(context: Context, languageCode: String = getSavedAppLanguage(context).code) {
        val systemLocale = getDeviceSystemLocale(context)
        val effectiveCode = AppLanguage.resolveEffectiveLocaleCode(languageCode, systemLocale)

        // 1. Android 13+ (API 33+) official Per-App Language API
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                val localeManager = context.getSystemService(Context.LOCALE_SERVICE) as? LocaleManager
                if (languageCode == AppLanguage.SYSTEM.code) {
                    localeManager?.applicationLocales = LocaleList.getEmptyLocaleList()
                } else {
                    localeManager?.applicationLocales = LocaleList.forLanguageTags(effectiveCode)
                }
            } catch (_: Exception) {}
        }

        // 2. Update resources / configuration directly for immediate Compose recomposition
        updateResourcesLocale(context, effectiveCode)
    }

    /**
     * Wraps context configuration with the effective locale for Activity attachBaseContext.
     */
    fun onAttach(baseContext: Context): Context {
        val effectiveCode = getEffectiveLanguageCode(baseContext)
        val locale = Locale.forLanguageTag(effectiveCode)
        Locale.setDefault(locale)

        val config = Configuration(baseContext.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return baseContext.createConfigurationContext(config)
    }

    fun wrapContext(context: Context): Context = onAttach(context)

    private fun updateResourcesLocale(context: Context, languageCode: String) {
        val locale = Locale.forLanguageTag(languageCode)
        Locale.setDefault(locale)

        val res = context.resources
        val config = Configuration(res.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)

        @Suppress("DEPRECATION")
        res.updateConfiguration(config, res.displayMetrics)

        val appCtx = context.applicationContext
        if (appCtx != null && appCtx !== context) {
            val appRes = appCtx.resources
            val appConfig = Configuration(appRes.configuration)
            appConfig.setLocale(locale)
            appConfig.setLayoutDirection(locale)
            @Suppress("DEPRECATION")
            appRes.updateConfiguration(appConfig, appRes.displayMetrics)
        }
    }

    /**
     * Detects if the given locale is one of the 5 supported languages.
     * If not, returns English.
     */
    fun detectSupportedLocale(locale: Locale): AppLanguage {
        val lang = locale.language.lowercase()
        return when (lang) {
            "en" -> AppLanguage.ENGLISH
            "es" -> AppLanguage.SPANISH
            "it" -> AppLanguage.ITALIAN
            "gl" -> AppLanguage.GALICIAN
            "de" -> AppLanguage.GERMAN
            else -> AppLanguage.ENGLISH
        }
    }

    private fun getDeviceSystemLocale(context: Context? = null): Locale {
        val systemLocales = Resources.getSystem().configuration.locales
        return if (!systemLocales.isEmpty) {
            systemLocales.get(0)
        } else {
            Locale.getDefault()
        }
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}
