package com.steevsapps.idledaddy.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import com.steevsapps.idledaddy.R
import com.steevsapps.idledaddy.listeners.DialogListener

class RedeemDialog : DialogFragment() {

    private var callback: DialogListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callback = try {
            context as DialogListener
        } catch (e: ClassCastException) {
            throw ClassCastException("$context must implement DialogListener.")
        }
    }

    override fun onDetach() {
        super.onDetach()
        callback = null
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(activity).inflate(R.layout.redeem_dialog, null)
        val input = view.findViewById<EditText>(R.id.input)

        val builder = AlertDialog.Builder(activity).apply {
            setTitle(R.string.redeem)
            setMessage(R.string.redeem_msg)
            setView(view)
            setPositiveButton(R.string.ok) { _, _ ->
                if (callback != null) {
                    callback!!.onYesPicked(input.getText().toString())
                }
            }
        }

        return builder.create()
    }

    companion object {
        fun newInstance(): RedeemDialog = RedeemDialog()
    }
}
