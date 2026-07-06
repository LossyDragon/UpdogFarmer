package com.steevsapps.idledaddy.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.steevsapps.idledaddy.R
import com.steevsapps.idledaddy.steam.model.Game

class CustomAppDialog : DialogFragment() {
    private lateinit var customApp: EditText
    private val viewModel: CustomAppViewModel by viewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = layoutInflater.inflate(R.layout.custom_app_dialog, null)
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
        spinner.adapter = adapter
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateInputType(position)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        updateInputType(spinner.selectedItemPosition)

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

    private fun updateInputType(position: Int) {
        customApp.inputType = if (position == TYPE_APPID) {
            InputType.TYPE_CLASS_NUMBER
        } else {
            InputType.TYPE_CLASS_TEXT
        }
    }

    private fun idleHiddenApp() {
        val text = customApp.text.toString().trim()
        if (text.isEmpty()) return

        try {
            val appId = text.toInt()
            val game = Game(appId, getString(R.string.playing_unknown_app, appId), 0f, 0)
            idleGame(game)
        } catch (e: NumberFormatException) {
            e.printStackTrace()
        }
    }

    private fun idleNonSteamApp() {
        val text = customApp.text.toString().trim()
        if (text.isEmpty()) return
        val game = Game(0, text, 0f, 0)
        idleGame(game)
    }

    private fun idleGame(game: Game) {
        viewModel.idleGame(game)
    }

    companion object {
        val TAG: String = CustomAppDialog::class.java.simpleName

        private const val TYPE_APPID = 0
        private const val TYPE_CUSTOM = 1

        fun newInstance(): CustomAppDialog = CustomAppDialog()
    }
}
