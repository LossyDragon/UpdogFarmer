package com.steevsapps.idledaddy

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import com.steevsapps.idledaddy.ui.screen.login.LoginScreen

class LoginActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "onCreate")
        setContent {
            LoginScreen(onLoggedIn = ::finish)
        }
    }

    companion object {
        private val TAG: String = LoginActivity::class.java.getSimpleName()
        fun createIntent(c: Context?): Intent = Intent(c, LoginActivity::class.java)
    }
}