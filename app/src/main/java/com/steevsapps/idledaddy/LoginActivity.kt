package com.steevsapps.idledaddy

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.IntentCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import com.steevsapps.idledaddy.preferences.PrefsManager
import com.steevsapps.idledaddy.steam.SteamService
import com.steevsapps.idledaddy.utils.Utils
import androidx.core.graphics.set
import androidx.core.graphics.createBitmap
import androidx.core.view.isVisible
import `in`.dragonbra.javasteam.enums.EResult

class LoginActivity : BaseActivity() {
    private var loginInProgress = false
    private var twoFactorRequired = false
    private var qrLoginActive = false

    private lateinit var viewModel: LoginViewModel

    // Views
    private lateinit var coordinatorLayout: CoordinatorLayout
    private lateinit var passwordContainer: LinearLayout
    private lateinit var usernameInput: TextInputLayout
    private lateinit var usernameEditText: TextInputEditText
    private lateinit var passwordInput: TextInputLayout
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var twoFactorInput: TextInputLayout
    private lateinit var twoFactorEditText: TextInputEditText
    private lateinit var loginButton: Button
    private lateinit var progress: ProgressBar
    private lateinit var qrContainer: LinearLayout
    private lateinit var qrImage: ImageView

    // Used to receive messages from SteamService
    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            when (intent.action) {
                SteamService.QR_CHALLENGE_EVENT -> {
                    val url = intent.getStringExtra(SteamService.QR_URL)
                    if (url != null) {
                        qrImage.setImageBitmap(generateQrBitmap(url))
                    }
                }

                SteamService.DEVICE_CONFIRMATION_EVENT -> {
                    Snackbar.make(
                        coordinatorLayout,
                        R.string.device_confirmation_prompt,
                        Snackbar.LENGTH_INDEFINITE
                    ).show()
                }

                SteamService.LOGIN_EVENT -> onLoginEvent(intent)
            }
        }
    }

    private fun onLoginEvent(intent: Intent) {
        stopTimeout()
        progress.isVisible = false
        val result = IntentCompat.getSerializableExtra(intent, SteamService.RESULT, EResult::class.java)
        if (result != EResult.OK) {
            loginButton.isEnabled = true
            usernameInput.isErrorEnabled = false
            passwordInput.isErrorEnabled = false
            twoFactorInput.isErrorEnabled = false

            when {
                qrLoginActive -> {
                    // QR sessions just expire/fail outright; there's no field to attach an error to
                    Snackbar.make(coordinatorLayout, R.string.qr_login_failed, Snackbar.LENGTH_LONG)
                        .show()
                }

                result == EResult.InvalidPassword -> {
                    passwordInput.error = getString(R.string.invalid_password)
                }

                result == EResult.AccountLoginDeniedNeedTwoFactor ||
                        result == EResult.AccountLogonDenied ||
                        result == EResult.AccountLogonDeniedNoMail ||
                        result == EResult.AccountLogonDeniedVerifiedEmailRequired -> {
                    twoFactorRequired = result == EResult.AccountLoginDeniedNeedTwoFactor
                    twoFactorInput.isVisible = true
                    twoFactorInput.error = getString(R.string.steamguard_required)
                    twoFactorEditText.requestFocus()
                }

                result == EResult.TwoFactorCodeMismatch || result == EResult.InvalidLoginAuthCode -> {
                    twoFactorInput.error = getString(R.string.invalid_code)
                }
            }
        } else {
            // Save password for autofill next time (SteamService persists the username itself,
            // since the QR flow never has one typed into usernameEditText)
            if (!qrLoginActive) {
                val password = Utils.removeSpecialChars(passwordEditText.text.toString().trim())
                PrefsManager.writePassword(password)
            }
            finish()
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
        passwordContainer = findViewById(R.id.password_container)
        usernameInput = findViewById(R.id.username_input)
        usernameEditText = findViewById(R.id.username_edittext)
        passwordInput = findViewById(R.id.password_input)
        passwordEditText = findViewById(R.id.password_edittext)
        twoFactorInput = findViewById(R.id.two_factor_input)
        twoFactorEditText = findViewById(R.id.two_factor_edittext)
        loginButton = findViewById(R.id.login)
        progress = findViewById(R.id.progress)
        qrContainer = findViewById(R.id.qr_container)
        qrImage = findViewById(R.id.qr_image)

        if (savedInstanceState != null) {
            loginInProgress = savedInstanceState.getBoolean(LOGIN_IN_PROGRESS)
            twoFactorRequired = savedInstanceState.getBoolean(TWO_FACTOR_REQUIRED)
            qrLoginActive = savedInstanceState.getBoolean(QR_LOGIN_ACTIVE)
            loginButton.isEnabled = !loginInProgress
            twoFactorInput.isVisible = twoFactorRequired
            progress.isVisible = loginInProgress && !qrLoginActive
            updateLoginMode()
        } else {
            // Restore saved username if any
            usernameEditText.setText(PrefsManager.getUsername())
            passwordEditText.setText(PrefsManager.getPassword())
        }

        setupViewModel()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(LOGIN_IN_PROGRESS, loginInProgress)
        outState.putBoolean(TWO_FACTOR_REQUIRED, twoFactorRequired)
        outState.putBoolean(QR_LOGIN_ACTIVE, qrLoginActive)
    }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver)
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter(SteamService.LOGIN_EVENT)
        filter.addAction(SteamService.QR_CHALLENGE_EVENT)
        filter.addAction(SteamService.DEVICE_CONFIRMATION_EVENT)
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filter)
    }

    @Suppress("unused")
    fun doLogin(v: View?) {
        if (twoFactorInput.isVisible) {
            // SteamService is already mid-login and waiting on a Steam Guard code
            val code = twoFactorEditText.text.toString().trim()
            if (code.isNotEmpty()) {
                loginButton.isEnabled = false
                progress.isVisible = true
                service!!.submitTwoFactorCode(code)
                startTimeout()
            }
            return
        }

        // Steam strips all non-ASCII characters from usernames and passwords
        val username = Utils.removeSpecialChars(usernameEditText.text.toString()).trim()
        val password = Utils.removeSpecialChars(passwordEditText.text.toString()).trim()
        if (username.isNotEmpty() && password.isNotEmpty()) {
            loginButton.isEnabled = false
            progress.isVisible = true
            service!!.login(username, password)
            startTimeout()
        }
    }

    /**
     * Toggle between the password form and the QR code sign-in view
     */
    @Suppress("unused")
    fun toggleQrLogin(v: View?) {
        qrLoginActive = !qrLoginActive
        updateLoginMode()
        if (qrLoginActive) {
            qrImage.setImageBitmap(null)
            service!!.loginWithQr()
            startTimeout()
        } else {
            stopTimeout()
        }
    }

    private fun updateLoginMode() {
        passwordContainer.isVisible = !qrLoginActive
        qrContainer.isVisible = qrLoginActive
    }

    /**
     * Render a QR code bitmap for the given content (the Steam QR login challenge URL)
     */
    private fun generateQrBitmap(content: String): Bitmap? {
        try {
            val size = 512
            val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size)
            val bitmap = createBitmap(size, size, Bitmap.Config.RGB_565)
            for (x in 0..<size) {
                for (y in 0..<size) {
                    bitmap[x, y] = if (matrix.get(x, y)) Color.BLACK else Color.WHITE
                }
            }
            return bitmap
        } catch (e: WriterException) {
            Log.e(TAG, "Failed to generate QR code", e)
            return null
        }
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(this)[LoginViewModel::class.java]
        viewModel.timeout.observe(this, Observer {
            loginInProgress = false
            loginButton.isEnabled = true
            progress.isVisible = false
            Snackbar.make(coordinatorLayout, R.string.timeout_error, Snackbar.LENGTH_LONG).show()
        })
    }

    companion object {
        private val TAG: String = LoginActivity::class.java.getSimpleName()

        private const val LOGIN_IN_PROGRESS = "LOGIN_IN_PROGRESS"
        private const val TWO_FACTOR_REQUIRED = "TWO_FACTOR_REQUIRED"
        private const val QR_LOGIN_ACTIVE = "QR_LOGIN_ACTIVE"

        fun createIntent(c: Context?): Intent = Intent(c, LoginActivity::class.java)
    }
}
