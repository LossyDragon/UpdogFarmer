package com.steevsapps.idledaddy.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.steevsapps.idledaddy.R
import com.steevsapps.idledaddy.preferences.PrefsManager.blacklist
import com.steevsapps.idledaddy.preferences.PrefsManager.writeBlacklist
import com.steevsapps.idledaddy.steam.model.Game

/**
 * Options dialog shown when you long press a game
 */
class GameOptionsDialog : DialogFragment() {

    private var title: String? = null
    private var appId: String? = null

    private var blacklisted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val args = arguments
        title = args!!.getString(TITLE)
        appId = args.getString(APPID)
        blacklisted = blacklist.contains(appId)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val options = resources.getStringArray(R.array.game_long_press_options)

        if (blacklisted) {
            // Already blacklisted
            options[0] = getString(R.string.remove_from_blacklist)
        }

        return AlertDialog.Builder(activity)
            .setTitle(title)
            .setItems(options) { _, position ->
                if (position == 0) {
                    addRemoveBlacklist()
                }
            }
            .create()
    }

    private fun addRemoveBlacklist() {
        val blacklist: MutableList<String> = blacklist.toMutableList()

        if (blacklisted) {
            blacklist.remove(appId)
        } else {
            blacklist.add(0, appId.orEmpty())
        }

        writeBlacklist(blacklist)

        Toast.makeText(
            activity,
            if (blacklisted) R.string.removed_from_blacklist else R.string.added_to_blacklist,
            Toast.LENGTH_LONG
        ).show()
    }

    companion object {
        val TAG: String = GameOptionsDialog::class.java.getSimpleName()

        private const val TITLE = "TITLE"
        private const val APPID = "APPID"

        fun newInstance(game: Game): GameOptionsDialog = GameOptionsDialog().apply {
            val args = Bundle().apply {
                putString(TITLE, game.name)
                putString(APPID, game.appId.toString())
            }
            setArguments(args)
        }
    }
}
