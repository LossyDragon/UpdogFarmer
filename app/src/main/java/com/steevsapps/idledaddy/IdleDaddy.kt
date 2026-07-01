package com.steevsapps.idledaddy

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import com.steevsapps.idledaddy.preferences.PrefsManager
import com.steevsapps.idledaddy.utils.LocaleManager

class IdleDaddy : Application() {
    override fun attachBaseContext(base: Context) {
        // Init SharedPreferences manager
        PrefsManager.init(base)
        super.attachBaseContext(LocaleManager.setLocale(base))
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        LocaleManager.setLocale(this)
    }
}
