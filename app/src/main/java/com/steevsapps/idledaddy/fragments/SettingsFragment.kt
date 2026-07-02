package com.steevsapps.idledaddy.fragments

import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.steevsapps.idledaddy.R
import com.steevsapps.idledaddy.preferences.BlacklistDialog
import com.steevsapps.idledaddy.preferences.BlacklistPreference
import com.steevsapps.idledaddy.preferences.NumPickerDialog
import com.steevsapps.idledaddy.preferences.NumPickerPreference

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        // Load preferences from XML resource
        addPreferencesFromResource(R.xml.preferences)
        findPreference<Preference>("gdpr_consent")?.setOnPreferenceClickListener { true }
    }

    override fun onDisplayPreferenceDialog(preference: Preference) {
        val fragment = when (preference) {
            is BlacklistPreference -> BlacklistDialog.newInstance(preference)
            is NumPickerPreference -> NumPickerDialog.newInstance(preference)
            else -> {
                super.onDisplayPreferenceDialog(preference)
                return
            }
        }
        @Suppress("DEPRECATION")
        fragment.setTargetFragment(this, 0)
        fragment.show(parentFragmentManager, DIALOG_TAG)
    }

    companion object {
        private const val DIALOG_TAG = "android.support.v7.preference.PreferenceFragment.DIALOG"

        fun newInstance(): SettingsFragment = SettingsFragment()
    }
}