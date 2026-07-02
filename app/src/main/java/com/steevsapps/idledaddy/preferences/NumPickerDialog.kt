package com.steevsapps.idledaddy.preferences

import android.os.Bundle
import android.view.View
import android.widget.NumberPicker
import android.widget.NumberPicker.OnValueChangeListener
import androidx.preference.Preference
import androidx.preference.PreferenceDialogFragmentCompat
import com.steevsapps.idledaddy.R

class NumPickerDialog : PreferenceDialogFragmentCompat(), OnValueChangeListener {
    private var numPicker: NumberPicker? = null
    private var preference: NumPickerPreference? = null
    private var currentValue = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preference = getPreference() as NumPickerPreference
        currentValue = savedInstanceState?.getInt(VALUE) ?: preference!!.value
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(VALUE, currentValue)
    }

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)
        numPicker = view.findViewById(R.id.numpicker)
        numPicker!!.setMinValue(0)
        numPicker!!.setMaxValue(5)
        numPicker!!.value = currentValue
        numPicker!!.setOnValueChangedListener(this)
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        preference!!.persistValue(currentValue)
    }

    override fun onValueChange(numberPicker: NumberPicker?, oldValue: Int, newValue: Int) {
        currentValue = newValue
    }

    companion object {
        private const val VALUE = "VALUE"

        fun newInstance(preference: Preference): NumPickerDialog {
            val fragment = NumPickerDialog()
            Bundle().apply {
                putString("key", preference.key)
            }.also(fragment::setArguments)
            return fragment
        }
    }
}
