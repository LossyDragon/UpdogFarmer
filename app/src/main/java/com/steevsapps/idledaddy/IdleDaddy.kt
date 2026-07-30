package com.steevsapps.idledaddy

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import com.steevsapps.idledaddy.di.appModule
import com.steevsapps.idledaddy.di.viewModelModule
import com.steevsapps.idledaddy.preferences.PrefsManager
import com.steevsapps.idledaddy.utils.CrashHandler
import com.steevsapps.idledaddy.utils.LocaleManager
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext.startKoin

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

    override fun onCreate() {
        super.onCreate()

        // Init CrashHandler
        CrashHandler.install(this@IdleDaddy)

        startKoin {
            androidContext(this@IdleDaddy)
            modules(appModule, viewModelModule)
        }
    }
}
