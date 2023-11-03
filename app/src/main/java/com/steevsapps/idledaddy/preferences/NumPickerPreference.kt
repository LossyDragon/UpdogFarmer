package com.steevsapps.idledaddy.preferences

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import androidx.preference.DialogPreference
import com.steevsapps.idledaddy.R

class NumPickerPreference(
    context: Context,
    attrs: AttributeSet?
) : DialogPreference(context, attrs) {

    var value = 0
        private set

    init {
        dialogLayoutResource = R.layout.numpicker_dialog
        positiveButtonText = null
        dialogIcon = null

        setNegativeButtonText(android.R.string.cancel)
    }

    fun persistValue(value: Int) {
        this.value = value
        persistInt(value)
    }

    override fun onGetDefaultValue(a: TypedArray, index: Int): Any =
        a.getInteger(index, DEFAULT_VALUE)

    override fun onSetInitialValue(restorePersistedValue: Boolean, defaultValue: Any?) {
        if (restorePersistedValue) {
            // Restore persisted value.
            // NOTE: local variable defaultValue is always null here
            value = getPersistedInt(DEFAULT_VALUE)
        } else {
            // Set default value
            value = defaultValue as Int
            persistInt(value)
        }
    }

    companion object {
        private const val DEFAULT_VALUE = 3
    }
}
