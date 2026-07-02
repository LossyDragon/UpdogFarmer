package com.steevsapps.idledaddy.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import com.steevsapps.idledaddy.R
import com.steevsapps.idledaddy.listeners.DialogListener

class RedeemDialog : DialogFragment() {
    private var callback: DialogListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callback = context as? DialogListener
            ?: throw ClassCastException("$context must implement DialogListener.")
    }

    override fun onDetach() {
        super.onDetach()
        callback = null
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = layoutInflater.inflate(R.layout.redeem_dialog, null)
        val input = view.findViewById<EditText>(R.id.input)
        return AlertDialog.Builder(activity)
            .setTitle(R.string.redeem)
            .setMessage(R.string.redeem_msg)
            .setView(view)
            .setPositiveButton(R.string.ok) { _, _ ->
                callback?.onYesPicked(input.text.toString())
            }
            .create()
    }

    companion object {
        fun newInstance(): RedeemDialog = RedeemDialog()
    }
}
