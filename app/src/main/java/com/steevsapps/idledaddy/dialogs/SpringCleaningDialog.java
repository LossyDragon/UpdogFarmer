package com.steevsapps.idledaddy.dialogs;

import android.app.Dialog;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.appcompat.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.steevsapps.idledaddy.BaseActivity;
import com.steevsapps.idledaddy.R;
import com.steevsapps.idledaddy.steam.SteamWebHandler;

public class SpringCleaningDialog extends DialogFragment implements View.OnClickListener {
    public final static String TAG = SpringCleaningDialog.class.getSimpleName();

    private Button dailyTasksBtn;
    private TextView statusTv;
    private SpringCleaningViewModel viewModel;

    public static SpringCleaningDialog newInstance() {
        return new SpringCleaningDialog();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final View view = LayoutInflater.from(getActivity()).inflate(R.layout.spring_cleaning_dialog, null);
        dailyTasksBtn = view.findViewById(R.id.btn_daily_tasks);
        statusTv = view.findViewById(R.id.status);
        dailyTasksBtn.setOnClickListener(this);

        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.spring_cleaning_title)
                .setView(view)
                .create();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setupViewModel();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_daily_tasks) {
            viewModel.completeTasks();
        }
    }

    private void setupViewModel() {
        viewModel = ViewModelProviders.of(this).get(SpringCleaningViewModel.class);
        viewModel.init(SteamWebHandler.getInstance(), ((BaseActivity) getActivity()).getService());
        viewModel.getStatus().observe(this, new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                statusTv.setVisibility(View.VISIBLE);
                statusTv.setText(s);
                dailyTasksBtn.setEnabled(viewModel.isFinished());
            }
        });
    }
}
