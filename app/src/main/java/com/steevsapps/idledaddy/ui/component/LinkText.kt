package com.steevsapps.idledaddy.ui.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.tooling.preview.Preview
import com.steevsapps.idledaddy.R

internal val appLinkStyles: TextLinkStyles
    @Composable get() = TextLinkStyles(
        style = SpanStyle(
            color = MaterialTheme.colorScheme.primary,
            textDecoration = TextDecoration.None,
        ),
    )

@Composable
fun LinkText(text: String, url: String) {
    Text(
        text = buildAnnotatedString {
            withLink(LinkAnnotation.Url(url = url, styles = appLinkStyles)) {
                append(text)
            }
        },
        style = MaterialTheme.typography.bodyLarge,
    )
}

/**
 * Preview
 */

@Preview
@Composable
private fun Preview() {
    LinkText(text = stringResource(R.string.app_name), "")
}