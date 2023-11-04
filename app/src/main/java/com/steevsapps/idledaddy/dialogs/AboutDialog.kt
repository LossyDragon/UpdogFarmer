package com.steevsapps.idledaddy.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.webkit.WebView
import androidx.fragment.app.DialogFragment
import com.steevsapps.idledaddy.R
import java.io.IOException
import java.util.Locale

class AboutDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val webView = LayoutInflater.from(activity).inflate(R.layout.about_dialog, null) as WebView
        val lang = Locale.getDefault().language
        var uri = "file:///android_asset/about.html"

        try {
            // Load language-specific version of the about page if available.
            val assets = resources.assets.list("")?.toList() ?: listOf()
            if (assets.contains(String.format("about-%s.html", lang))) {
                uri = String.format("file:///android_asset/about-%s.html", lang)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
            // Getting Chromium crashes on certain KitKat devices. Might be caused by hardware acceleration
            webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        }

        webView.loadUrl(uri)
        webView.setBackgroundColor(Color.TRANSPARENT)

        return AlertDialog.Builder(activity)
            .setTitle(R.string.about)
            .setView(webView)
            .setPositiveButton(R.string.ok, null)
            .create()
    }

    companion object {
        val TAG: String = AboutDialog::class.java.getSimpleName()

        fun newInstance(): AboutDialog = AboutDialog()
    }
}
