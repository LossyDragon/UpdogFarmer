package com.steevsapps.idledaddy

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.steevsapps.idledaddy.preferences.PrefsManager
import com.steevsapps.idledaddy.steam.SteamGuard
import com.steevsapps.idledaddy.steam.SteamService
import com.steevsapps.idledaddy.steam.SteamWebHandler
import com.steevsapps.idledaddy.utils.Utils
import `in`.dragonbra.javasteam.enums.EOSType
import `in`.dragonbra.javasteam.enums.EResult
import `in`.dragonbra.javasteam.steam.handlers.steamuser.LogOnDetails


class LoginActivity : BaseActivity() {

    private lateinit var viewModel: LoginViewModel

    private var loginInProgress = false
    private var timeDifference: Int = 0
    private var twoFactorRequired = false

    // Views
    private lateinit var coordinatorLayout: CoordinatorLayout
    private lateinit var loginButton: Button
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var passwordInput: TextInputLayout
    private lateinit var progress: ProgressBar
    private lateinit var twoFactorEditText: TextInputEditText
    private lateinit var twoFactorInput: TextInputLayout
    private lateinit var usernameEditText: TextInputEditText
    private lateinit var usernameInput: TextInputLayout

    // Used to receive messages from SteamService
    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (SteamService.LOGIN_EVENT == intent.action) {
                stopTimeout()

                progress.visibility = View.GONE

                val result: EResult? = intent.getSerializableExtra(SteamService.RESULT) as EResult?
                if (result != EResult.OK) {
                    loginButton.setEnabled(true)
                    usernameInput.isErrorEnabled = false
                    passwordInput.isErrorEnabled = false
                    twoFactorInput.isErrorEnabled = false
                    when (result) {
                        EResult.InvalidPassword ->
                            passwordInput.error = getString(R.string.invalid_password)
                        EResult.AccountLoginDeniedNeedTwoFactor,
                        EResult.AccountLogonDenied,
                        EResult.AccountLogonDeniedNoMail,
                        EResult.AccountLogonDeniedVerifiedEmailRequired -> {
                            twoFactorRequired = result == EResult.AccountLoginDeniedNeedTwoFactor
                            if (twoFactorRequired && timeDifference >= 0) {
                                // Fill in the SteamGuard code
                                twoFactorEditText.setText(
                                    SteamGuard.generateSteamGuardCodeForTime(
                                        Utils.currentUnixTime + timeDifference
                                    )
                                )
                            }
                            twoFactorInput.visibility = View.VISIBLE
                            twoFactorInput.error = getString(R.string.steamguard_required)
                            twoFactorEditText.requestFocus()
                        }
                        EResult.TwoFactorCodeMismatch,
                        EResult.InvalidLoginAuthCode ->
                            twoFactorInput.error = getString(R.string.invalid_code)
                        else -> Unit
                    }
                } else {
                    // Save username
                    val username =
                        Utils.removeSpecialChars(usernameEditText.getText().toString()).trim()
                    val password =
                        Utils.removeSpecialChars(passwordEditText.getText().toString()).trim()

                    PrefsManager.writeUsername(username)
                    PrefsManager.writePassword(this@LoginActivity, password)

                    finish()
                }
            }
        }
    }

    /**
     * Start timeout handler in case the server doesn't respond
     */
    private fun startTimeout() {
        loginInProgress = true
        viewModel.startTimeout()
    }

    /**
     * Stop the timeout handler
     */
    private fun stopTimeout() {
        loginInProgress = false
        viewModel.stopTimeout()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_login)

        coordinatorLayout = findViewById(R.id.coordinator)
        usernameInput = findViewById(R.id.username_input)
        usernameEditText = findViewById(R.id.username_edittext)
        passwordInput = findViewById(R.id.password_input)
        passwordEditText = findViewById(R.id.password_edittext)
        twoFactorInput = findViewById(R.id.two_factor_input)
        twoFactorEditText = findViewById(R.id.two_factor_edittext)
        loginButton = findViewById(R.id.login)
        progress = findViewById(R.id.progress)

        if (savedInstanceState != null) {
            loginInProgress = savedInstanceState.getBoolean(LOGIN_IN_PROGRESS)
            twoFactorRequired = savedInstanceState.getBoolean(TWO_FACTOR_REQUIRED)
            loginButton.setEnabled(!loginInProgress)
            twoFactorInput.visibility = if (twoFactorRequired) View.VISIBLE else View.GONE
            progress.visibility = if (loginInProgress) View.VISIBLE else View.GONE
        } else {
            // Restore saved username if any
            usernameEditText.setText(PrefsManager.username)
            passwordEditText.setText(PrefsManager.getPassword(this))
        }

        setupViewModel()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        with(outState) {
            putBoolean(LOGIN_IN_PROGRESS, loginInProgress)
            putBoolean(TWO_FACTOR_REQUIRED, twoFactorRequired)
        }
    }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver)
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter(SteamService.LOGIN_EVENT)
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filter)
    }

    @Suppress("UNUSED_PARAMETER")
    fun doLogin(v: View) {
        // Steam strips all non-ASCII characters from usernames and passwords
        val username = Utils.removeSpecialChars(usernameEditText.getText().toString()).trim()
        val password = Utils.removeSpecialChars(passwordEditText.getText().toString()).trim()

        if (username.isNotEmpty() && password.isNotEmpty()) {
            loginButton.setEnabled(false)
            progress.visibility = View.VISIBLE

            LogOnDetails().apply {
                this.username = username
                this.password = password
                clientOSType = EOSType.LinuxUnknown
                if (twoFactorRequired) {
                    twoFactorCode = twoFactorEditText.getText().toString().trim()
                } else {
                    authCode = twoFactorEditText.getText().toString().trim()
                }
                isShouldRememberPassword = true
            }.also(service!!::login)

            startTimeout()
        }
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(this)[LoginViewModel::class.java].apply {
            init(SteamWebHandler.getInstance())
            getTimeDifference().observe(this@LoginActivity) {
                timeDifference = it
            }
            timeout.observe(this@LoginActivity) {
                loginInProgress = false
                loginButton.setEnabled(true)
                progress.visibility = View.GONE

                Snackbar.make(
                    coordinatorLayout,
                    R.string.timeout_error,
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }

    companion object {
        private val TAG = LoginActivity::class.java.getSimpleName()

        private const val LOGIN_IN_PROGRESS = "LOGIN_IN_PROGRESS"
        private const val TWO_FACTOR_REQUIRED = "TWO_FACTOR_REQUIRED"

        @JvmStatic
        fun createIntent(c: Context?): Intent = Intent(c, LoginActivity::class.java)
    }
}
