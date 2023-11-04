package com.steevsapps.idledaddy.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.steevsapps.idledaddy.R
import com.steevsapps.idledaddy.steam.SteamWebHandler

class AutoDiscoverDialog : DialogFragment(), View.OnClickListener {

    private var viewModel: AutoDiscoverViewModel? = null

    private lateinit var statusTv: TextView
    private lateinit var autoDiscoverBtn: Button

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater
            .from(activity)
            .inflate(R.layout.auto_discover_dialog, null)

        statusTv = view.findViewById(R.id.status)

        autoDiscoverBtn = view.findViewById(R.id.btn_auto_discover)
        autoDiscoverBtn.setOnClickListener(this)

        return AlertDialog.Builder(activity)
            .setTitle(R.string.auto_discovery_title)
            .setView(view)
            .create()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setupViewModel()
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(this)[AutoDiscoverViewModel::class.java].apply {
            init(SteamWebHandler.instance)
            status.observe(this@AutoDiscoverDialog) { s ->
                statusTv.visibility = View.VISIBLE
                statusTv.text = s
                autoDiscoverBtn.setEnabled(viewModel!!.isFinished)
            }
        }
    }

    override fun onClick(v: View) {
        if (v.id == R.id.btn_auto_discover) {
            viewModel!!.autodiscover()
        }
    }

    companion object {
        val TAG: String = AutoDiscoverDialog::class.java.getSimpleName()

        fun newInstance(): AutoDiscoverDialog = AutoDiscoverDialog()
    }
}
