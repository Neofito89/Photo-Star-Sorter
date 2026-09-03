package com.example.util

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LocaleHelperTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        // Reset SharedPreferences
        val prefs = context.getSharedPreferences(LocaleHelper.PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
    }

    @Test
    fun `detectSupportedLocale returns exact match for supported languages`() {
        assertEquals(AppLanguage.ENGLISH, LocaleHelper.detectSupportedLocale(Locale.ENGLISH))
        assertEquals(AppLanguage.ENGLISH, LocaleHelper.detectSupportedLocale(Locale.US))
        assertEquals(AppLanguage.SPANISH, LocaleHelper.detectSupportedLocale(Locale.forLanguageTag("es")))
        assertEquals(AppLanguage.SPANISH, LocaleHelper.detectSupportedLocale(Locale.forLanguageTag("es-ES")))
        assertEquals(AppLanguage.ITALIAN, LocaleHelper.detectSupportedLocale(Locale.ITALIAN))
        assertEquals(AppLanguage.ITALIAN, LocaleHelper.detectSupportedLocale(Locale.ITALY))
        assertEquals(AppLanguage.GALICIAN, LocaleHelper.detectSupportedLocale(Locale.forLanguageTag("gl")))
        assertEquals(AppLanguage.GALICIAN, LocaleHelper.detectSupportedLocale(Locale.forLanguageTag("gl-ES")))
        assertEquals(AppLanguage.GERMAN, LocaleHelper.detectSupportedLocale(Locale.GERMAN))
        assertEquals(AppLanguage.GERMAN, LocaleHelper.detectSupportedLocale(Locale.GERMANY))
    }

    @Test
    fun `detectSupportedLocale defaults to English for unsupported languages`() {
        assertEquals(AppLanguage.ENGLISH, LocaleHelper.detectSupportedLocale(Locale.FRENCH))
        assertEquals(AppLanguage.ENGLISH, LocaleHelper.detectSupportedLocale(Locale.JAPANESE))
        assertEquals(AppLanguage.ENGLISH, LocaleHelper.detectSupportedLocale(Locale.CHINESE))
        assertEquals(AppLanguage.ENGLISH, LocaleHelper.detectSupportedLocale(Locale.forLanguageTag("ru")))
        assertEquals(AppLanguage.ENGLISH, LocaleHelper.detectSupportedLocale(Locale.forLanguageTag("pt")))
    }

    @Test
    fun `app language persistence and retrieval works correctly`() {
        // Initially default is SYSTEM
        assertEquals(AppLanguage.SYSTEM, LocaleHelper.getSavedAppLanguage(context))

        // Set to Spanish and verify persistence
        LocaleHelper.setAppLanguage(context, AppLanguage.SPANISH)
        assertEquals(AppLanguage.SPANISH, LocaleHelper.getSavedAppLanguage(context))

        // Set to Deutsch and verify persistence
        LocaleHelper.setAppLanguage(context, AppLanguage.GERMAN)
        assertEquals(AppLanguage.GERMAN, LocaleHelper.getSavedAppLanguage(context))

        // Set to Italiano
        LocaleHelper.setAppLanguage(context, AppLanguage.ITALIAN)
        assertEquals(AppLanguage.ITALIAN, LocaleHelper.getSavedAppLanguage(context))

        // Set to Galego
        LocaleHelper.setAppLanguage(context, AppLanguage.GALICIAN)
        assertEquals(AppLanguage.GALICIAN, LocaleHelper.getSavedAppLanguage(context))

        // Set back to SYSTEM
        LocaleHelper.setAppLanguage(context, AppLanguage.SYSTEM)
        assertEquals(AppLanguage.SYSTEM, LocaleHelper.getSavedAppLanguage(context))
    }

    @Test
    fun `native language names match required specification`() {
        assertEquals("English", AppLanguage.ENGLISH.nativeName)
        assertEquals("Español", AppLanguage.SPANISH.nativeName)
        assertEquals("Italiano", AppLanguage.ITALIAN.nativeName)
        assertEquals("Galego", AppLanguage.GALICIAN.nativeName)
        assertEquals("Deutsch", AppLanguage.GERMAN.nativeName)
    }

    @Test
    fun `all language resources have expected translated strings`() {
        val supportedLocales = listOf(
            Locale.ENGLISH,
            Locale.forLanguageTag("es"),
            Locale.ITALIAN,
            Locale.forLanguageTag("gl"),
            Locale.GERMAN
        )

        for (loc in supportedLocales) {
            val config = context.resources.configuration
            config.setLocale(loc)
            val localizedContext = context.createConfigurationContext(config)

            val appName = localizedContext.getString(R.string.app_name)
            val settingsTitle = localizedContext.getString(R.string.settings_title)
            val appLang = localizedContext.getString(R.string.settings_app_language)
            val systemDefault = localizedContext.getString(R.string.settings_system_default)

            assertNotNull(appName)
            assertTrue(appName.isNotBlank())
            assertNotNull(settingsTitle)
            assertTrue(settingsTitle.isNotBlank())
            assertNotNull(appLang)
            assertTrue(appLang.isNotBlank())
            assertNotNull(systemDefault)
            assertTrue(systemDefault.isNotBlank())
        }
    }
}
