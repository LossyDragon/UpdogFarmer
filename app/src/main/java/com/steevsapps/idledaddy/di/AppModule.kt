package com.steevsapps.idledaddy.di

import com.steevsapps.idledaddy.steam.SteamServiceConnection
import com.steevsapps.idledaddy.steam.SteamWebHandler
import com.steevsapps.idledaddy.ui.screen.games.GamesViewModel
import com.steevsapps.idledaddy.ui.screen.home.HomeViewModel
import com.steevsapps.idledaddy.ui.screen.login.LoginViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val viewModelModule = module {
    viewModelOf(::GamesViewModel)
    viewModelOf(::HomeViewModel)
    viewModelOf(::LoginViewModel)
}

val appModule = module {
    single { SteamServiceConnection(androidContext()) }
    single { SteamWebHandler() }
}