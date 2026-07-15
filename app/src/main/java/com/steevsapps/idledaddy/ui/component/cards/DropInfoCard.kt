package com.steevsapps.idledaddy.ui.component.cards

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.steevsapps.idledaddy.R
import com.steevsapps.idledaddy.ui.theme.IdleTheme
import com.steevsapps.idledaddy.ui.theme.LocalAmoled

@Composable
fun DropInfoCard(
    modifier: Modifier = Modifier,
    gameCount: Int,
    cardCount: Int,
) {
    val amoled = LocalAmoled.current
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (amoled) Color(0xFF101010) else MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
        ) {
            Text(
                text = pluralStringResource(R.plurals.games_left, gameCount, gameCount),
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Text(
                text = pluralStringResource(
                    R.plurals.card_drops_remaining,
                    cardCount,
                    cardCount,
                ),
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * Preview
 */

private class DropInfoPreview : PreviewParameterProvider<Triple<Boolean, Int, Int>> {
    override val values = sequenceOf(Triple(false, 1, 1), Triple(true, 24, 57))

    override fun getDisplayName(index: Int) =
        if (values.elementAt(index).first) "AMOLED" else "Default"
}

@Preview(showBackground = false)
@Composable
private fun Preview(
    @PreviewParameter(DropInfoPreview::class) values: Triple<Boolean, Int, Int>,
) {
    IdleTheme(amoled = values.first) {
        DropInfoCard(
            gameCount = values.second,
            cardCount = values.third,
        )
    }
}