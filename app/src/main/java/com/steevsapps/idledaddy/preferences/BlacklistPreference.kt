package com.steevsapps.idledaddy.preferences

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import androidx.preference.DialogPreference
import com.steevsapps.idledaddy.R

class BlacklistPreference(
    context: Context,
    attrs: AttributeSet
) : DialogPreference(context, attrs) {
    var value: String = ""
        private set

    init {
        dialogLayoutResource = R.layout.blacklist_dialog
        setPositiveButtonText(android.R.string.ok)
        setNegativeButtonText(android.R.string.cancel)
        dialogIcon = null
    }

    fun persistStringValue(value: String) {
        this.value = value
        persistString(this.value)
    }

    override fun onGetDefaultValue(a: TypedArray, index: Int): Any? = a.getString(index)

    override fun onSetInitialValue(defaultValue: Any?) {
        if (defaultValue == null) {
            // Restore persisted value
            this.value = getPersistedString("")
        } else {
            // Set default value
            this.value = defaultValue.toString()
            persistString(this.value)
        }
    }
}
