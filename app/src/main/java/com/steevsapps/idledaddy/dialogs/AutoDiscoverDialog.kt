package com.steevsapps.idledaddy.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.steevsapps.idledaddy.R
import com.steevsapps.idledaddy.steam.SteamWebHandler

class AutoDiscoverDialog : DialogFragment() {
    private lateinit var viewModel: AutoDiscoverViewModel
    private lateinit var statusTv: TextView
    private lateinit var autoDiscoverBtn: Button

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = layoutInflater.inflate(R.layout.auto_discover_dialog, null)
        statusTv = view.findViewById(R.id.status)
        autoDiscoverBtn = view.findViewById(R.id.btn_auto_discover)
        autoDiscoverBtn.setOnClickListener {
            viewModel.autodiscover()
        }

        viewModel = ViewModelProvider(this)[AutoDiscoverViewModel::class.java]
        viewModel.init(SteamWebHandler.instance)
        viewModel.statusMessage.observe(this) { s ->
            statusTv.visibility = View.VISIBLE
            statusTv.text = s
            autoDiscoverBtn.isEnabled = viewModel.isFinished
        }

        return AlertDialog.Builder(activity)
            .setTitle(R.string.auto_discovery_title)
            .setView(view)
            .create()
    }

    companion object {
        val TAG: String = AutoDiscoverDialog::class.java.simpleName
        fun newInstance(): AutoDiscoverDialog = AutoDiscoverDialog()
    }
}