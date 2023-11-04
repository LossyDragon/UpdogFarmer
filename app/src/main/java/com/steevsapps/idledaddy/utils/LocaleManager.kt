package com.steevsapps.idledaddy.utils

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import com.steevsapps.idledaddy.R
import com.steevsapps.idledaddy.preferences.PrefsManager
import java.util.Locale

object LocaleManager {

    private val TAG = LocaleManager::class.java.getSimpleName()

    /**
     * Set the locale from SharedPreferences.
     * Called from attachBaseContext and onConfigurationChanged of the Application class and
     * attachBaseContext of Activities and Services
     */
    @JvmStatic
    fun setLocale(context: Context): Context {
        val language: String = PrefsManager.language.ifEmpty {
            setInitialValue(context.resources)
        }

        return updateResources(context, language)
    }

    private fun setInitialValue(res: Resources): String {
        val tags = listOf(*res.getStringArray(R.array.language_option_values))
        val locale = getLocale(res)
        val languageTag = toLanguageTag(locale)
        val language: String = if (tags.contains(languageTag)) {
            languageTag
        } else if (tags.contains(locale.language)) {
            locale.language
        } else {
            "en"
        }

        PrefsManager.writeLanguage(language)

        return language
    }

    private fun updateResources(context: Context, language: String): Context {
        var ctx = context
        val locale = forLanguageTag(language)

        Locale.setDefault(locale)

        val res = ctx.resources
        val config = Configuration(res.configuration)

        config.setLocale(locale)
        ctx = ctx.createConfigurationContext(config)

        return ctx
    }

    private fun forLanguageTag(languageTag: String): Locale {
        val locale = languageTag.split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        return if (locale.size == 2) Locale(locale[0], locale[1]) else Locale(languageTag)
    }

    private fun toLanguageTag(locale: Locale): String {
        return if (locale.country.isNotEmpty()) {
            String.format("%s-%s", locale.language, locale.country)
        } else {
            locale.language
        }
    }

    private fun getLocale(res: Resources): Locale {
        val config = res.configuration
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.getLocales()[0]
        } else {
            @Suppress("DEPRECATION")
            config.locale
        }
    }
}
