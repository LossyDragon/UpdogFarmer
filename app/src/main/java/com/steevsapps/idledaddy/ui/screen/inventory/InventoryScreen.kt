package com.steevsapps.idledaddy.ui.screen.inventory

import android.annotation.SuppressLint
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import com.steevsapps.idledaddy.R
import com.steevsapps.idledaddy.ui.component.IdleTopAppBar
import com.steevsapps.idledaddy.ui.theme.IdleTheme
import org.koin.androidx.compose.koinViewModel

@Composable
fun InventoryScreen(onBack: () -> Unit) {
    val viewModel = koinViewModel<InventoryViewModel>()

    InventoryScreenContent(
        url = viewModel.url,
        cookies = viewModel.cookies,
        onBack = onBack,
    )
}

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreenContent(
    url: String,
    cookies: Map<String, String>,
    onBack: () -> Unit,
) {
    var webView by remember { mutableStateOf<WebView?>(null) }
    var canGoBack by remember { mutableStateOf(false) }

    // Let the WebView consume back presses while it has history
    BackHandler(enabled = canGoBack) { webView?.goBack() }

        Scaffold(
            topBar = {
                IdleTopAppBar(
                    title = stringResource(R.string.inventory),
                    onNavClick = onBack
                )
            }
        ) { paddingValues ->
            val localView = LocalView.current
            val placeholderColor = MaterialTheme.colorScheme.surfaceContainerHigh.toArgb()
            AndroidView(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
                factory = { context ->
                    if (localView.isInEditMode) {
                        View(context).apply { setBackgroundColor(placeholderColor) }
                    } else {
                        WebView(context).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            webViewClient = object : WebViewClient() {
                                override fun doUpdateVisitedHistory(
                                    view: WebView,
                                    url: String?,
                                    isReload: Boolean,
                                ) {
                                    canGoBack = view.canGoBack()
                                }
                            }
                            CookieManager.getInstance().apply {
                                setAcceptCookie(true)
                                cookies.forEach { (name, value) ->
                                    setCookie(url, "$name=$value; Path=/; Secure")
                                }
                            }
                            loadUrl(url)
                        }.also { webView = it }
                    }
                },
                onRelease = {
                    webView = null
                    (it as? WebView)?.destroy()
                },
            )
        }
    }

/**
 * Preview
 */

@Preview
@Composable
private fun Preview() {
    IdleTheme {
        InventoryScreenContent(
            url = "",
            cookies = emptyMap(),
            onBack = {},
        )
    }
}