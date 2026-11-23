package com.steevsapps.idledaddy.ui.component.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.steevsapps.idledaddy.R
import com.steevsapps.idledaddy.steam.LoginPhase
import com.steevsapps.idledaddy.ui.theme.IdleTheme
import com.steevsapps.idledaddy.ui.theme.LocalAmoled
import `in`.dragonbra.javasteam.enums.EResult
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

@Composable
fun StatusCard(
    isLoggedIn: Boolean,
    isBlocked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    nextRetryAtMillis: Long = 0L,
    loginPhase: LoginPhase = LoginPhase.Idle,
    loginUsername: String = "",
    loginError: EResult? = null,
    webLoginFailed: Boolean = false,
) {
    val amoled = LocalAmoled.current
    // Occupied (incl. the brief reconnect gap while retrying) wins over a login in
    // progress and the logged-out state so we don't flash "logging in"/"tap to login".
    val loggingIn = !isBlocked && loginPhase != LoginPhase.Idle
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        enabled = !isLoggedIn && !isBlocked,
        colors = CardDefaults.cardColors(
            containerColor = if (amoled) Color(0xFF101010) else MaterialTheme.colorScheme.surfaceContainerHigh,
            disabledContainerColor = if (amoled) Color(0xFF101010) else MaterialTheme.colorScheme.surfaceContainerHigh,
            disabledContentColor = MaterialTheme.colorScheme.onSurface,
        ),

        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = stringResource(R.string.status))
                Text(
                    text = when {
                        isBlocked -> stringResource(R.string.logged_in_elsewhere)
                        loggingIn -> stringResource(R.string.status_logging_in_as, loginUsername)
                        isLoggedIn -> stringResource(R.string.logged_in)
                        else -> stringResource(R.string.tap_to_login)
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (loggingIn) {
                    // Where the login currently is
                    Text(
                        text = stringResource(
                            when (loginPhase) {
                                LoginPhase.WaitingToReconnect -> R.string.status_login_reconnect_wait
                                LoginPhase.LoggingOn -> R.string.status_login_logging_on
                                LoginPhase.WebAuth -> R.string.status_login_web
                                LoginPhase.Connecting, LoginPhase.Idle -> R.string.status_login_connecting
                            }
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (!isBlocked && !isLoggedIn && loginError != null) {
                    // Why the last attempt (or the session) ended
                    Text(
                        text = stringResource(R.string.status_login_error, loginError.name),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                if (!isBlocked && !loggingIn && isLoggedIn && webLoginFailed) {
                    Text(
                        text = stringResource(R.string.web_login_failed),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                if (isBlocked && nextRetryAtMillis > 0L) {
                    // Live countdown until the next resume attempt.
                    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
                    LaunchedEffect(nextRetryAtMillis) {
                        while (true) {
                            now = System.currentTimeMillis()
                            delay(1.seconds)
                        }
                    }
                    val remaining = ((nextRetryAtMillis - now) / 1000L).coerceAtLeast(0L)
                    Text(
                        text = stringResource(
                            R.string.status_retry_countdown,
                            formatCountdown(remaining)
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (loggingIn) {
                // Same 48dp footprint as the icon
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(4.dp)
                        .size(40.dp),
                )
            } else {
                // Without the website login farming can't run, so that isn't a healthy state
                val healthy = isLoggedIn && !isBlocked && !webLoginFailed
                val image = if (healthy) Icons.Default.CheckCircle else Icons.Default.Error
                val color = if (healthy) Color(0xFF90BA3C) else Color(0xFFFFBB33)
                Icon(
                    modifier = Modifier.size(48.dp),
                    imageVector = image,
                    tint = color,
                    contentDescription = stringResource(R.string.status),
                )
            }
        }
    }
}

/** Format remaining seconds as "m:ss" (>= 1 min) or "Ns". */
private fun formatCountdown(seconds: Long): String = if (seconds >= 60) {
    "%d:%02d".format(seconds / 60, seconds % 60)
} else {
    "${seconds}s"
}

/**
 * Preview
 */

private data class StatusPreviewState(
    val loggedIn: Boolean,
    val isBlocked: Boolean = false,
    val nextRetryAtMillis: Long = 0L,
    val amoled: Boolean = false,
    val loginPhase: LoginPhase = LoginPhase.Idle,
    val loginUsername: String = "",
    val loginError: EResult? = null,
    val webLoginFailed: Boolean = false,
)

private class StatusPreview : PreviewParameterProvider<StatusPreviewState> {
    private val states = listOf(
        "Logged out" to StatusPreviewState(loggedIn = false),
        "Connecting" to StatusPreviewState(
            loggedIn = false,
            loginPhase = LoginPhase.Connecting,
            loginUsername = "gaben",
        ),
        "Reconnecting after error" to StatusPreviewState(
            loggedIn = false,
            loginPhase = LoginPhase.WaitingToReconnect,
            loginUsername = "gaben",
            loginError = EResult.ServiceUnavailable,
        ),
        "Logged out with error" to StatusPreviewState(
            loggedIn = false,
            loginError = EResult.NoConnection,
        ),
        "Web sign in failed" to StatusPreviewState(loggedIn = true, webLoginFailed = true),
        "Web sign in" to StatusPreviewState(
            loggedIn = true,
            loginPhase = LoginPhase.WebAuth,
            loginUsername = "gaben",
        ),
        "Logged in" to StatusPreviewState(loggedIn = true),
        "Blocked" to StatusPreviewState(loggedIn = true, isBlocked = true),
        "Blocked with countdown" to StatusPreviewState(
            loggedIn = true,
            isBlocked = true,
            nextRetryAtMillis = System.currentTimeMillis() + 125_000L,
        ),
        "AMOLED logged out" to StatusPreviewState(loggedIn = false, amoled = true),
        "AMOLED logged in" to StatusPreviewState(loggedIn = true, amoled = true),
    )

    override val values = states.asSequence().map { it.second }

    override fun getDisplayName(index: Int) = states[index].first
}

@Preview
@Composable
private fun Preview(
    @PreviewParameter(StatusPreview::class) state: StatusPreviewState,
) {
    IdleTheme(amoled = state.amoled) {
        StatusCard(
            isLoggedIn = state.loggedIn,
            isBlocked = state.isBlocked,
            nextRetryAtMillis = state.nextRetryAtMillis,
            loginPhase = state.loginPhase,
            loginUsername = state.loginUsername,
            loginError = state.loginError,
            webLoginFailed = state.webLoginFailed,
            onClick = {},
        )
    }
}