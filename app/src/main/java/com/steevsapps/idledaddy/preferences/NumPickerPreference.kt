package com.steevsapps.idledaddy.preferences

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import androidx.preference.DialogPreference
import com.steevsapps.idledaddy.R

class NumPickerPreference(
    context: Context,
    attrs: AttributeSet
) : DialogPreference(context, attrs) {
    var value: Int = 0
        private set

    init {
        dialogLayoutResource = R.layout.numpicker_dialog
        positiveButtonText = null
        setNegativeButtonText(android.R.string.cancel)
        dialogIcon = null
    }

    fun persistValue(value: Int) {
        this.value = value
        persistInt(value)
    }

    override fun onGetDefaultValue(a: TypedArray, index: Int): Any {
        return a.getInteger(index, DEFAULT_VALUE)
    }

    override fun onSetInitialValue(defaultValue: Any?) {
        if (defaultValue == null) {
            // Restore persisted value
            this.value = getPersistedInt(DEFAULT_VALUE)
        } else {
            // Set default value
            this.value = defaultValue as Int
            persistInt(this.value)
        }
    }

    companion object {
        private const val DEFAULT_VALUE = 3
    }
}
