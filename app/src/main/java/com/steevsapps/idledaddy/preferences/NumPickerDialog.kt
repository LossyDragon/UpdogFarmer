package com.steevsapps.idledaddy.preferences

import android.os.Bundle
import android.view.View
import android.widget.NumberPicker
import android.widget.NumberPicker.OnValueChangeListener
import androidx.preference.Preference
import androidx.preference.PreferenceDialogFragmentCompat
import com.steevsapps.idledaddy.R

class NumPickerDialog : PreferenceDialogFragmentCompat(), OnValueChangeListener {

    private lateinit var numPicker: NumberPicker
    private lateinit var preference: NumPickerPreference

    private var currentValue = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preference = getPreference() as NumPickerPreference
        currentValue = savedInstanceState?.getInt(VALUE) ?: preference.value
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(VALUE, currentValue)
    }

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)
        numPicker = view.findViewById(R.id.numpicker)
        numPicker.setMaxValue(5)
        numPicker.setMinValue(0)
        numPicker.setOnValueChangedListener(this)
        numPicker.value = currentValue
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        preference.persistValue(currentValue)
    }

    override fun onValueChange(numberPicker: NumberPicker, oldValue: Int, newValue: Int) {
        currentValue = newValue
    }

    companion object {
        private const val VALUE = "VALUE"

        @JvmStatic
        fun newInstance(preference: Preference): NumPickerDialog = NumPickerDialog().apply {
            Bundle().apply {
                putString("key", preference.key)
            }.also(this::setArguments)
        }
    }
}
