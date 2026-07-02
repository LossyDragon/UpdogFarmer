package com.steevsapps.idledaddy.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.webkit.WebView
import androidx.fragment.app.DialogFragment
import com.steevsapps.idledaddy.R
import java.io.IOException
import java.util.Locale

class AboutDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val webView = layoutInflater.inflate(R.layout.about_dialog, null) as WebView
        val lang = Locale.getDefault().language
        var uri = "file:///android_asset/about.html"
        try {
            // Load language-specific version of the about page if available.
            val localizedFile = "about-$lang.html"
            if (resources.assets.list("")?.contains(localizedFile) == true) {
                uri = "file:///android_asset/$localizedFile"
            }
        } catch (e: IOException) {
            e.printStackTrace()
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
