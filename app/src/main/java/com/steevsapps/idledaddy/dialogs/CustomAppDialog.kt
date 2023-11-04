package com.steevsapps.idledaddy.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import androidx.fragment.app.DialogFragment
import com.steevsapps.idledaddy.R
import com.steevsapps.idledaddy.listeners.GamePickedListener
import com.steevsapps.idledaddy.steam.model.Game

class CustomAppDialog : DialogFragment() {
    private var customApp: EditText? = null
    private var callback: GamePickedListener? = null
    override fun onAttach(context: Context) {
        super.onAttach(context)
        callback = try {
            context as GamePickedListener
        } catch (e: ClassCastException) {
            throw ClassCastException(context.toString() + "must implement GamePickedListener.")
        }
    }

    override fun onDetach() {
        super.onDetach()
        callback = null
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(activity).inflate(R.layout.custom_app_dialog, null)

        customApp = view.findViewById(R.id.custom_app)

        val spinner = view.findViewById<Spinner>(R.id.spinner)

        // Create an ArrayAdapter using the string array and a default spinner layout
        val adapter = ArrayAdapter.createFromResource(
            requireActivity(),
            R.array.custom_app_type_options,
            android.R.layout.simple_spinner_item
        )

        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        // Apply the adapter to the spinner
        spinner.setAdapter(adapter)

        return AlertDialog.Builder(activity)
            .setTitle(R.string.idle_custom_app)
            .setView(view)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                when (spinner.selectedItemPosition) {
                    TYPE_APPID -> idleHiddenApp()
                    TYPE_CUSTOM -> idleNonSteamApp()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()
    }

    private fun idleHiddenApp() {
        val text = customApp!!.getText().toString().trim()

        if (text.isEmpty()) {
            return
        }

        try {
            val appId = text.toInt()
            val game = Game(appId, getString(R.string.playing_unknown_app, appId), 0f, 0)
            callback?.onGamePicked(game)
        } catch (e: NumberFormatException) {
            e.printStackTrace()
        }
    }

    private fun idleNonSteamApp() {
        val text = customApp!!.getText().toString().trim()

        if (text.isEmpty()) {
            return
        }

        val game = Game(0, text, 0f, 0)
        callback?.onGamePicked(game)
    }

    companion object {
        val TAG: String = CustomAppDialog::class.java.getSimpleName()

        private const val TYPE_APPID = 0
        private const val TYPE_CUSTOM = 1

        fun newInstance(): CustomAppDialog = CustomAppDialog()
    }
}
