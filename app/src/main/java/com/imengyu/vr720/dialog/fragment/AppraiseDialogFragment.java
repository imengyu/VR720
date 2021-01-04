package com.imengyu.vr720.dialog.fragment;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.preference.PreferenceManager;

import com.imengyu.vr720.BuildConfig;
import com.imengyu.vr720.R;
import com.imengyu.vr720.utils.AlertDialogTool;
import com.imengyu.vr720.utils.AppUtils;

import java.util.Date;
import java.util.Objects;

public class AppraiseDialogFragment extends DialogFragment {

    public AppraiseDialogFragment() {
        super();
        setStyle(DialogFragment.STYLE_NORMAL, R.style.CustomDialog);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_appraise, container, false);

        Button button_good = view.findViewById(R.id.button_good);
        Button button_bad = view.findViewById(R.id.button_bad);
        Button button_later = view.findViewById(R.id.button_later);

        button_good.setOnClickListener((v) -> goAppStore());
        button_bad.setOnClickListener((v) -> goAppStore());
        button_later.setOnClickListener((v) -> {
            dismiss();

            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
            sharedPreferences.edit().putLong("last_show_appraise_dialog", new Date().getTime()).apply();
        });

        return view;
    }

    @Override
    public void onResume() {
        Window window = requireDialog().getWindow();
        WindowManager.LayoutParams params = window.getAttributes();
        params.width = WindowManager.LayoutParams.MATCH_PARENT;
        params.height = WindowManager.LayoutParams.MATCH_PARENT;
        window.setAttributes(params);

        super.onResume();
    }

    private void goAppStore() {
        AppUtils.goToAppStore(requireContext(), BuildConfig.APPLICATION_ID);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
        sharedPreferences.edit().putBoolean("appraise_dialog_opened", true).apply();
        dismiss();
    }



}
