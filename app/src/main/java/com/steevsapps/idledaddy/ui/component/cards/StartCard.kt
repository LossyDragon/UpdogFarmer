package com.steevsapps.idledaddy.ui.component.cards

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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

@Composable
fun StartCard(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val amoled = LocalAmoled.current
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (amoled) Color(0xFF101010) else MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 16.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.cat_idle),
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = Icons.Default.AttachMoney,
                contentDescription = null,
            )
        }
        Text(
            text = stringResource(R.string.desc_start),
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        HorizontalDivider()
        TextButton(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            shape = MaterialTheme.shapes.small,
        ) {
            Text(text = stringResource(R.string.start_idling))
        }
    }
}

/**
 * Preview
 */

private class StartPreview : PreviewParameterProvider<Pair<Boolean, Boolean>> {
    override val values = sequenceOf(
        true to false,
        false to false,
        true to true,
    )

    override fun getDisplayName(index: Int): String {
        val (enabled, amoled) = values.elementAt(index)
        return when {
            amoled -> "AMOLED"
            enabled -> "Enabled"
            else -> "Disabled"
        }
    }
}

@Preview
@Composable
private fun Preview(
    @PreviewParameter(StartPreview::class) values: Pair<Boolean, Boolean>,
) {
    val (enabled, amoled) = values
    IdleTheme(amoled = amoled) {
        StartCard(enabled = enabled, onClick = {})
    }
}