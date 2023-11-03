package com.steevsapps.idledaddy.preferences

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.TextView.OnEditorActionListener
import androidx.preference.Preference
import androidx.preference.PreferenceDialogFragmentCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.steevsapps.idledaddy.R
import com.steevsapps.idledaddy.adapters.BlacklistAdapter

class BlacklistDialog :
    PreferenceDialogFragmentCompat(),
    View.OnClickListener,
    OnEditorActionListener {

    private var currentValue: String? = null

    private lateinit var adapter: BlacklistAdapter
    private lateinit var addButton: ImageView
    private lateinit var input: EditText
    private lateinit var layoutManager: RecyclerView.LayoutManager
    private lateinit var preference: BlacklistPreference
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        preference = getPreference() as BlacklistPreference

        currentValue = if (savedInstanceState == null) {
            preference.value
        } else {
            savedInstanceState.getString(VALUE)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(VALUE, adapter.value)
    }

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)

        adapter = BlacklistAdapter(currentValue)

        addButton = view.findViewById(R.id.add)
        addButton.setOnClickListener(this)

        input = view.findViewById(R.id.input)
        input.setOnEditorActionListener(this)

        recyclerView = view.findViewById(R.id.recycler_view)
        recyclerView.setAdapter(adapter)
        recyclerView.setHasFixedSize(true)

        layoutManager = LinearLayoutManager(recyclerView.context)

        recyclerView.setLayoutManager(layoutManager)
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult) {
            preference.persistStringValue(adapter.value)
        }
    }

    override fun onClick(view: View) {
        if (view.id == R.id.add) {
            addItem()
        }
    }

    private fun addItem() {
        val text = input.getText().toString().trim { it <= ' ' }
        if (text.matches("\\d+".toRegex())) {
            adapter.addItem(text)
            input.setText("")
            recyclerView.scrollToPosition(0)
        }
    }

    override fun onEditorAction(textView: TextView, actionId: Int, event: KeyEvent): Boolean {
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            // Submit
            addItem()
            return true
        }

        return false
    }

    companion object {
        private const val VALUE = "VALUE" // Key to hold current value

        @JvmStatic
        fun newInstance(preference: Preference): BlacklistDialog = BlacklistDialog().apply {
            Bundle(1).apply {
                putString("key", preference.key)
            }.also(this::setArguments)
        }
    }
}
