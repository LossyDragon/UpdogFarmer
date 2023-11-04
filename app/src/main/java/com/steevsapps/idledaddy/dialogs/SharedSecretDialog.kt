package com.steevsapps.idledaddy.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.steevsapps.idledaddy.R
import com.steevsapps.idledaddy.preferences.PrefsManager.sharedSecret
import com.steevsapps.idledaddy.preferences.PrefsManager.writeSharedSecret

class SharedSecretDialog : DialogFragment(), View.OnClickListener {

    private lateinit var enterManuallyBtn: Button
    private lateinit var sharedSecretBtn: Button
    private lateinit var statusTv: TextView
    private var viewModel: SharedSecretViewModel? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(activity).inflate(R.layout.shared_secret_dialog, null)

        statusTv = view.findViewById(R.id.status)

        sharedSecretBtn = view.findViewById(R.id.btn_shared_secret)
        sharedSecretBtn.setOnClickListener(this)

        enterManuallyBtn = view.findViewById(R.id.btn_enter_manually)
        enterManuallyBtn.setOnClickListener(this)

        return AlertDialog.Builder(activity)
            .setTitle(R.string.import_shared_secret)
            .setView(view)
            .create()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setupViewModel()
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(this)[SharedSecretViewModel::class.java].apply {
            init(requireArguments().getLong(STEAM_ID))
            status.observe(this@SharedSecretDialog) { s ->
                statusTv.visibility = View.VISIBLE
                statusTv.text = s
            }
        }
    }

    override fun onClick(v: View) {
        val id = v.id
        if (id == R.id.btn_shared_secret) {
            viewModel!!.sharedSecret
        } else if (id == R.id.btn_enter_manually) {
            showManualDialog()
        }
    }

    private fun showManualDialog() {
        val view = LayoutInflater.from(activity).inflate(R.layout.enter_shared_secret_dialog, null)

        val editText = view.findViewById<EditText>(R.id.shared_secret_input)
        editText.setText(sharedSecret)

        AlertDialog.Builder(activity)
            .setTitle(R.string.enter_shared_secret)
            .setView(view)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val text = editText.getText().toString().trim()
                writeSharedSecret(text)
                viewModel!!.setValue(getString(R.string.your_shared_secret, text))
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    companion object {
        val TAG: String = SharedSecretDialog::class.java.getSimpleName()

        private const val STEAM_ID = "STEAM_ID"

        fun newInstance(steamId: Long): SharedSecretDialog = SharedSecretDialog().apply {
            val args = Bundle().apply {
                putLong(STEAM_ID, steamId)
            }
            setArguments(args)
        }
    }
}
