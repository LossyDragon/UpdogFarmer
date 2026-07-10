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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
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
import kotlinx.coroutines.delay

@Composable
fun StatusCard(
    isLoggedIn: Boolean,
    isParentalControlled: Boolean,
    isBlocked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    nextRetryAtMillis: Long = 0L,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        // Only offer "tap to login" when genuinely logged out, not mid-retry reconnect.
        enabled = !isLoggedIn && !isBlocked,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
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
                            delay(1000L)
                        }
                    }
                    val remaining = ((nextRetryAtMillis - now) / 1000L).coerceAtLeast(0L)
                    Text(
                        text = stringResource(R.string.status_retry_countdown, formatCountdown(remaining)),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (isParentalControlled) {
                    Text(
                        text = stringResource(R.string.status_parental, true),
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
    val isParentalControlled: Boolean,
    val isBlocked: Boolean = false,
    val nextRetryAtMillis: Long = 0L,
)

private class StatusPreview : PreviewParameterProvider<StatusPreviewState> {
    override val values = sequenceOf(
        StatusPreviewState(loggedIn = false, isParentalControlled = false),
        StatusPreviewState(loggedIn = true, isParentalControlled = false),
        StatusPreviewState(loggedIn = true, isParentalControlled = true),
        StatusPreviewState(loggedIn = true, isParentalControlled = false, isBlocked = true),
        StatusPreviewState(
            loggedIn = true,
            isParentalControlled = false,
            isBlocked = true,
            nextRetryAtMillis = System.currentTimeMillis() + 125_000L,
        ),
    )
}

@Preview
@Composable
private fun Preview(
    @PreviewParameter(StatusPreview::class) state: StatusPreviewState,
) {
    IdleTheme {
        StatusCard(
            isLoggedIn = state.loggedIn,
            isParentalControlled = state.isParentalControlled,
            isBlocked = state.isBlocked,
            nextRetryAtMillis = state.nextRetryAtMillis,
            onClick = {},
        )
    }
}