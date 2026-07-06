package com.steevsapps.idledaddy.ui.screen.login

import android.annotation.SuppressLint
import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.graphics.Bitmap
import android.graphics.Color
import android.os.IBinder
import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import com.steevsapps.idledaddy.R
import com.steevsapps.idledaddy.preferences.PrefsManager
import com.steevsapps.idledaddy.steam.SteamService
import com.steevsapps.idledaddy.utils.Utils
import `in`.dragonbra.javasteam.enums.EResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

enum class LoginType {
    CREDENTIAL,
    QR,
    ;
}

data class LoginUiState(
    val loginType: LoginType = LoginType.CREDENTIAL,
    val loginInProgress: Boolean = false,
    val twoFactorRequired: Boolean = false,
    @StringRes val passwordError: Int? = null,
    @StringRes val twoFactorError: Int? = null,
    val qrCode: ImageBitmap? = null,
    val qrFailed: Boolean = false,
    val loggedIn: Boolean = false,
    val snackbar: LoginSnackbar? = null,
)

enum class LoginSnackbar(@StringRes val message: Int, val indefinite: Boolean = false) {
    TIMEOUT(R.string.timeout_error),
    QR_FAILED(R.string.qr_login_failed),
    LOGIN_FAILED(R.string.login_failed),
    DEVICE_CONFIRMATION(R.string.device_confirmation_prompt, indefinite = true),
    ;
}

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    var uiState by mutableStateOf(LoginUiState())
        private set

    val savedUsername: String = PrefsManager.getUsername()
    val savedPassword: String = PrefsManager.getPassword()
    private var pendingPassword = ""

    @SuppressLint("StaticFieldLeak") // I know...
    private var service: SteamService? = null
    private var serviceBound = false

    private var timeoutJob: Job? = null

    private val loginEventListener = object : SteamService.LoginEventListener {
        override fun onLoginResult(result: EResult) {
            onLoginEvent(result)
        }

        override fun onQrChallenge(url: String) {
            uiState = uiState.copy(qrCode = generateQrBitmap(url))
        }

        override fun onDeviceConfirmation() {
            uiState = uiState.copy(snackbar = LoginSnackbar.DEVICE_CONFIRMATION)
        }
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName?, iBinder: IBinder) {
            service = (iBinder as SteamService.LocalBinder).service.also {
                it.loginEventListener = loginEventListener
            }
        }

        override fun onServiceDisconnected(componentName: ComponentName?) {
            service = null
        }
    }

    init {
        val app = getApplication<Application>()
        val serviceIntent = SteamService.createIntent(app)
        ContextCompat.startForegroundService(app, serviceIntent)
        app.bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE)
        serviceBound = true
    }

    override fun onCleared() {
        stopTimeout()
        service?.loginEventListener = null
        if (serviceBound) {
            getApplication<Application>().unbindService(connection)
            serviceBound = false
        }
        pendingPassword = ""
    }

    fun doLogin(username: String, password: String, twoFactorCode: String) {
        val service = service ?: return

        if (uiState.twoFactorRequired) {
            // SteamService is already mid-login and waiting on a Steam Guard code
            val code = twoFactorCode.trim()
            if (code.isNotEmpty()) {
                uiState = uiState.copy(loginInProgress = true, twoFactorError = null)
                service.submitTwoFactorCode(code)
                startTimeout()
            }
            return
        }

        // Steam strips all non-ASCII characters from usernames and passwords
        val user = Utils.removeSpecialChars(username).trim()
        val pass = Utils.removeSpecialChars(password).trim()
        if (user.isNotEmpty() && pass.isNotEmpty()) {
            pendingPassword = pass
            uiState = uiState.copy(loginInProgress = true, passwordError = null)
            service.login(user, pass)
            startTimeout()
        }
    }

    fun toggleLoginType() {
        val qrActive = uiState.loginType == LoginType.CREDENTIAL
        uiState = uiState.copy(
            loginType = if (qrActive) LoginType.QR else LoginType.CREDENTIAL,
            qrCode = null,
            qrFailed = false,
        )
        if (qrActive) {
            service?.loginWithQr()
            startTimeout()
        } else {
            stopTimeout()
        }
    }

    fun retryQrLogin() {
        uiState = uiState.copy(qrCode = null, qrFailed = false)
        service?.loginWithQr()
        startTimeout()
    }

    fun snackbarShown() {
        uiState = uiState.copy(snackbar = null)
    }

    private fun onLoginEvent(result: EResult) {
        stopTimeout()

        if (result == EResult.OK) {
            // Save password for autofill next time (SteamService persists the username itself,
            // since the QR flow never has one typed in)
            if (uiState.loginType == LoginType.CREDENTIAL) {
                PrefsManager.writePassword(pendingPassword)
            }
            uiState = uiState.copy(loginInProgress = false, loggedIn = true)
            return
        }

        var state = uiState.copy(
            loginInProgress = false,
            passwordError = null,
            twoFactorError = null,
        )
        when {
            state.loginType == LoginType.QR -> {
                // QR sessions just expire/fail outright; there's no field to attach an error to
                state = state.copy(qrCode = null, qrFailed = true, snackbar = LoginSnackbar.QR_FAILED)
            }

            result == EResult.InvalidPassword -> {
                state = state.copy(passwordError = R.string.invalid_password)
            }

            result == EResult.AccountLoginDeniedNeedTwoFactor ||
                result == EResult.AccountLogonDenied ||
                result == EResult.AccountLogonDeniedNoMail ||
                result == EResult.AccountLogonDeniedVerifiedEmailRequired -> {
                state = state.copy(
                    twoFactorRequired = true,
                    twoFactorError = R.string.steamguard_required,
                )
            }

            result == EResult.TwoFactorCodeMismatch || result == EResult.InvalidLoginAuthCode -> {
                state = state.copy(twoFactorError = R.string.invalid_code)
            }

            else -> {
                state = state.copy(snackbar = LoginSnackbar.LOGIN_FAILED)
            }
        }
        uiState = state
    }

    private fun startTimeout() {
        Log.i(TAG, "Starting login timeout")
        timeoutJob?.cancel()
        timeoutJob = viewModelScope.launch {
            delay(30.seconds)
            uiState = uiState.copy(
                loginInProgress = false,
                qrCode = null,
                qrFailed = uiState.loginType == LoginType.QR,
                snackbar = LoginSnackbar.TIMEOUT,
            )
        }
    }

    private fun stopTimeout() {
        Log.i(TAG, "Stopping login timeout")
        timeoutJob?.cancel()
        timeoutJob = null
    }

    private fun generateQrBitmap(content: String): ImageBitmap? {
        try {
            val size = 512
            val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size)
            val bitmap = createBitmap(size, size, Bitmap.Config.RGB_565)
            for (x in 0..<size) {
                for (y in 0..<size) {
                    bitmap[x, y] = if (matrix.get(x, y)) Color.BLACK else Color.WHITE
                }
            }
            return bitmap.asImageBitmap()
        } catch (e: WriterException) {
            Log.e(TAG, "Failed to generate QR code", e)
            return null
        }
    }

    companion object {
        private val TAG: String = LoginViewModel::class.java.getSimpleName()
    }
}