package com.steevsapps.idledaddy.utils

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import com.steevsapps.idledaddy.R
import com.steevsapps.idledaddy.preferences.PrefsManager.getLanguage
import com.steevsapps.idledaddy.preferences.PrefsManager.writeLanguage
import java.util.Locale

object LocaleManager {
    /**
     * Set the locale from SharedPreferences.
     * Called from attachBaseContext and onConfigurationChanged of the Application class and
     * attachBaseContext of Activities and Services
     */
    fun setLocale(context: Context): Context {
        val language = getLanguage().ifEmpty { setInitialValue(context.resources) }
        return updateResources(context, language)
    }

    private fun setInitialValue(res: Resources): String {
        val tags = res.getStringArray(R.array.language_option_values).toList()
        val locale = getLocale(res)
        val languageTag = toLanguageTag(locale)
        val language = when {
            tags.contains(languageTag) -> languageTag
            tags.contains(locale.language) -> locale.language
            else -> "en"
        }
        writeLanguage(language)
        return language
    }

    private fun updateResources(context: Context, language: String): Context {
        val locale = Locale.forLanguageTag(language)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }

    private fun toLanguageTag(locale: Locale): String {
        if (locale.country.isNotEmpty()) {
            return "${locale.language}-${locale.country}"
        }
        return locale.language
    }

    private fun getLocale(res: Resources): Locale = res.configuration.locales[0]
}