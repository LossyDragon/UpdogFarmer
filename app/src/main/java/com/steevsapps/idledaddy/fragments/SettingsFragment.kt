package com.steevsapps.idledaddy.fragments

import android.content.SharedPreferences
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.steevsapps.idledaddy.R
import com.steevsapps.idledaddy.preferences.BlacklistDialog
import com.steevsapps.idledaddy.preferences.BlacklistPreference
import com.steevsapps.idledaddy.preferences.NumPickerDialog
import com.steevsapps.idledaddy.preferences.NumPickerPreference

class SettingsFragment : PreferenceFragmentCompat() {

    private val prefs: SharedPreferences? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        // Load preferences from XML resource
        addPreferencesFromResource(R.xml.preferences)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setupGdpr()
    }

    private fun setupGdpr() {
        // findPreference("gdpr_consent").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
        //     @Override
        //     public boolean onPreferenceClick(Preference preference) {
        //         ((ConsentListener) getActivity()).onConsentRevoked();
        //         return true;
        //     }
        // });
    }

    override fun onDisplayPreferenceDialog(preference: Preference) {
        when (preference) {
            is BlacklistPreference -> {
                // Show blacklist dialog
                val fragment = BlacklistDialog.newInstance(preference)
                fragment.setTargetFragment(this, 0)
                fragment.show(
                    requireFragmentManager(),
                    "android.support.v7.preference.PreferenceFragment.DIALOG"
                )
            }
            is NumPickerPreference -> {
                val fragment = NumPickerDialog.newInstance(preference)
                fragment.setTargetFragment(this, 0)
                fragment.show(
                    requireFragmentManager(),
                    "android.support.v7.preference.PreferenceFragment.DIALOG"
                )
            }
            else -> super.onDisplayPreferenceDialog(preference)
        }
    }

    companion object {
        private val TAG = SettingsFragment::class.java.getSimpleName()

        fun newInstance(): SettingsFragment = SettingsFragment()
    }
}
