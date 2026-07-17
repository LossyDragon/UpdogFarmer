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
import com.steevsapps.idledaddy.ui.theme.IdleTheme
import com.steevsapps.idledaddy.ui.theme.LocalAmoled
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

@Composable
fun StatusCard(
    isLoggedIn: Boolean,
    isBlocked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    nextRetryAtMillis: Long = 0L,
) {
    val amoled = LocalAmoled.current
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
                    text = stringResource(
                        when {
                            // Occupied (incl. the brief reconnect gap while retrying) wins
                            // over the logged-out state so we don't flash "tap to login".
                            isBlocked -> R.string.logged_in_elsewhere
                            isLoggedIn -> R.string.logged_in
                            else -> R.string.tap_to_login
                        }
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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

            val image = if (isLoggedIn && !isBlocked) Icons.Default.CheckCircle
            else Icons.Default.Error
            val color = if (isLoggedIn && !isBlocked) Color(0xFF90BA3C)
            else Color(0xFFFFBB33)
            Icon(
                modifier = Modifier.size(48.dp),
                imageVector = image,
                tint = color,
                contentDescription = stringResource(R.string.status),
            )
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
)

private class StatusPreview : PreviewParameterProvider<StatusPreviewState> {
    private val states = listOf(
        "Logged out" to StatusPreviewState(loggedIn = false),
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
            onClick = {},
        )
    }
}