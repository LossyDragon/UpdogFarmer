package com.steevsapps.idledaddy

import android.content.Context
import android.content.res.Configuration
import androidx.multidex.MultiDexApplication
import com.steevsapps.idledaddy.preferences.PrefsManager
import com.steevsapps.idledaddy.utils.LocaleManager

class IdleDaddy : MultiDexApplication() {

    override fun attachBaseContext(base: Context) {
        // Init SharedPreferences manager
        PrefsManager.init(base)

        // Super below pref init
        super.attachBaseContext(LocaleManager.setLocale(base))
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        LocaleManager.setLocale(this)
    }
}
