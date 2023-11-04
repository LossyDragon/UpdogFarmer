package com.steevsapps.idledaddy.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.steevsapps.idledaddy.BaseActivity
import com.steevsapps.idledaddy.R
import com.steevsapps.idledaddy.steam.SteamWebHandler

class SpringCleaningDialog : DialogFragment(), View.OnClickListener {
    private lateinit var dailyTasksBtn: Button
    private lateinit var statusTv: TextView
    private var viewModel: SpringCleaningViewModel? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(activity).inflate(R.layout.spring_cleaning_dialog, null)

        dailyTasksBtn = view.findViewById(R.id.btn_daily_tasks)
        statusTv = view.findViewById(R.id.status)

        dailyTasksBtn.setOnClickListener(this)

        return AlertDialog.Builder(requireActivity())
            .setTitle(R.string.spring_cleaning_title)
            .setView(view)
            .create()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setupViewModel()
    }

    override fun onClick(v: View) {
        if (v.id == R.id.btn_daily_tasks) {
            viewModel!!.completeTasks()
        }
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(this)[SpringCleaningViewModel::class.java].apply {
            init(SteamWebHandler.instance, (activity as BaseActivity?)!!.service!!)
            status.observe(this@SpringCleaningDialog) { s ->
                statusTv.visibility = View.VISIBLE
                statusTv.text = s
                dailyTasksBtn.setEnabled(viewModel!!.isFinished)
            }
        }
    }

    companion object {
        val TAG: String = SpringCleaningDialog::class.java.getSimpleName()

        fun newInstance(): SpringCleaningDialog = SpringCleaningDialog()
    }
}
