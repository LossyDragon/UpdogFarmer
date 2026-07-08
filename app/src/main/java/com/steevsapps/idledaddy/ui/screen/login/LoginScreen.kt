package com.steevsapps.idledaddy.ui.screen.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.steevsapps.idledaddy.R
import com.steevsapps.idledaddy.ui.theme.IdleTheme
import org.koin.androidx.compose.koinViewModel

private const val TWO_FACTOR_LENGTH = 5

@Composable
fun LoginScreen(
    onBack: () -> Unit,
    onLoggedIn: () -> Unit,
) {
    val viewModel = koinViewModel<LoginViewModel>()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current

    LaunchedEffect(state.loggedIn) {
        if (state.loggedIn) {
            onLoggedIn()
        }
    }

    LaunchedEffect(state.snackbar) {
        val snackbar = state.snackbar ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(
            message = resources.getString(snackbar.message),
            duration = if (snackbar.indefinite) {
                SnackbarDuration.Indefinite
            } else {
                SnackbarDuration.Long
            },
        )
        viewModel.snackbarShown()
    }

    LoginScreenComponent(
        onBack = onBack,
        loginType = state.loginType,
        loginInProgress = state.loginInProgress,
        twoFactorRequired = state.twoFactorRequired,
        passwordError = state.passwordError?.let { stringResource(it) },
        twoFactorError = state.twoFactorError?.let { stringResource(it) },
        qrCode = state.qrCode,
        qrFailed = state.qrFailed,
        initialUsername = viewModel.savedUsername,
        initialPassword = viewModel.savedPassword,
        snackbarHostState = snackbarHostState,
        onToggleLoginType = viewModel::toggleLoginType,
        onLogin = viewModel::doLogin,
        onRetryQr = viewModel::retryQrLogin,
    )
}

@Composable
private fun LoginScreenComponent(
    onBack: () -> Unit,
    initialPassword: String,
    initialUsername: String,
    loginInProgress: Boolean,
    loginType: LoginType,
    onLogin: (String, String, String) -> Unit,
    onRetryQr: () -> Unit = {},
    onToggleLoginType: () -> Unit,
    passwordError: String?,
    qrCode: ImageBitmap?,
    qrFailed: Boolean = false,
    snackbarHostState: SnackbarHostState,
    twoFactorError: String?,
    twoFactorRequired: Boolean,
) {
    IdleTheme {
        Scaffold(
            modifier = Modifier.imePadding(),
            topBar = {
                TopAppBar(
                    title = { Text(text = stringResource(R.string.login)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null
                            )
                        }
                    }
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            ) {
                /* Not AppBar title */
                Text(
                    modifier = Modifier.padding(vertical = 16.dp),
                    text = stringResource(R.string.login_to_steam),
                    style = MaterialTheme.typography.headlineSmall,
                )
                /* The login type between Credential or QR sign in */
                when (loginType) {
                    LoginType.CREDENTIAL -> CredentialsComponent(
                        loginInProgress = loginInProgress,
                        twoFactorRequired = twoFactorRequired,
                        passwordError = passwordError,
                        twoFactorError = twoFactorError,
                        initialUsername = initialUsername,
                        initialPassword = initialPassword,
                        onLogin = onLogin,
                        onToggleLoginType = onToggleLoginType,
                    )

                    LoginType.QR -> QrComponent(
                        qrCode = qrCode,
                        qrFailed = qrFailed,
                        onToggleLoginType = onToggleLoginType,
                        onRetry = onRetryQr,
                    )
                }
            }
        }
    }
}

@Composable
private fun CredentialsComponent(
    loginInProgress: Boolean,
    twoFactorRequired: Boolean,
    passwordError: String?,
    twoFactorError: String?,
    initialUsername: String,
    initialPassword: String,
    onLogin: (username: String, password: String, twoFactorCode: String) -> Unit,
    onToggleLoginType: () -> Unit,
) {
    var username by rememberSaveable { mutableStateOf(initialUsername) }
    var password by rememberSaveable { mutableStateOf(initialPassword) }
    var twoFactorCode by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        /* Username Field */
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(text = stringResource(R.string.username)) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next,
            ),
            singleLine = true,
        )
        /* Password Field */
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(text = stringResource(R.string.password)) },
            isError = passwordError != null,
            supportingText = passwordError?.let { { Text(text = it) } },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = if (twoFactorRequired) ImeAction.Next else ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(
                onDone = { onLogin(username, password, twoFactorCode) },
            ),
            singleLine = true,
        )
        /* Two-Factor Code Field */
        if (twoFactorRequired) {
            OutlinedTextField(
                value = twoFactorCode,
                onValueChange = { twoFactorCode = it.take(TWO_FACTOR_LENGTH) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = stringResource(R.string.steamguard)) },
                isError = twoFactorError != null,
                supportingText = twoFactorError?.let { { Text(text = it) } },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    capitalization = KeyboardCapitalization.Characters,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = { onLogin(username, password, twoFactorCode) },
                ),
                singleLine = true,
            )
        }
        /* Login Button row with ProgressIndicator */
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Button(
                onClick = { onLogin(username, password, twoFactorCode) },
                enabled = !loginInProgress,
                shape = MaterialTheme.shapes.small,
            ) {
                Text(text = stringResource(R.string.login))
            }
            if (loginInProgress) {
                CircularProgressIndicator(modifier = Modifier.size(32.dp))
            }
        }
        /* QR Sign in button */
        TextButton(
            onClick = onToggleLoginType,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.small,
        ) {
            Text(text = stringResource(R.string.login_with_qr))
        }
    }
}

@Composable
private fun QrComponent(
    onRetry: () -> Unit,
    onToggleLoginType: () -> Unit,
    qrCode: ImageBitmap?,
    qrFailed: Boolean,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        /* Box to align a QR Image, ProgressIndicator, or Retry button */
        Box(
            modifier = Modifier
                .padding(top = 8.dp)
                .size(220.dp),
            contentAlignment = Alignment.Center,
        ) {
            when {
                qrFailed -> {
                    Button(
                        onClick = onRetry,
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Text(text = stringResource(R.string.retry))
                    }
                }

                qrCode != null -> {
                    Image(
                        bitmap = qrCode,
                        contentDescription = stringResource(R.string.login_with_qr),
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                else -> {
                    CircularProgressIndicator()
                }
            }
        }
        /* QR text prompt */
        if (!qrFailed) {
            Text(
                text = stringResource(R.string.qr_login_instructions),
                textAlign = TextAlign.Center,
            )
        }
        /* Credential button */
        TextButton(
            onClick = onToggleLoginType,
            shape = MaterialTheme.shapes.small,
        ) {
            Text(text = stringResource(R.string.login_with_password))
        }
    }
}

/**
 * Preview
 */

private class LoginPreview : PreviewParameterProvider<LoginUiState> {
    private val states = listOf(
        "Credentials" to LoginUiState(),
        "Password error" to LoginUiState(passwordError = R.string.invalid_password),
        "Steam Guard" to LoginUiState(
            loginInProgress = true,
            twoFactorRequired = true,
            twoFactorError = R.string.steamguard_required,
        ),
        "QR" to LoginUiState(loginType = LoginType.QR),
        "QR failed" to LoginUiState(loginType = LoginType.QR, qrFailed = true),
    )

    override val values = states.asSequence().map { it.second }

    override fun getDisplayName(index: Int) = states[index].first
}

@Preview
@Composable
private fun Preview(@PreviewParameter(LoginPreview::class) state: LoginUiState) {
    LoginScreenComponent(
        onBack = {},
        initialPassword = "idledaddy",
        initialUsername = "steev",
        loginInProgress = state.loginInProgress,
        loginType = state.loginType,
        onLogin = { _, _, _ -> },
        onToggleLoginType = {},
        passwordError = state.passwordError?.let { stringResource(it) },
        qrCode = state.qrCode,
        qrFailed = state.qrFailed,
        snackbarHostState = remember { SnackbarHostState() },
        twoFactorError = state.twoFactorError?.let { stringResource(it) },
        twoFactorRequired = state.twoFactorRequired,
    )
}