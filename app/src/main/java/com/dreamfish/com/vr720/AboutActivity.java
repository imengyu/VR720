package com.dreamfish.com.vr720;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.dreamfish.com.vr720.core.NativeVR720;
import com.dreamfish.com.vr720.utils.StatusBarUtils;
import com.dreamfish.com.vr720.widget.MyTitleBar;

public class AboutActivity extends AppCompatActivity {

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        MyTitleBar titleBar = findViewById(R.id.myTitleBar);
        titleBar.setLeftIconOnClickListener(v -> finish());

        StatusBarUtils.setLightMode(this);

        findViewById(R.id.button_back).setOnClickListener(v -> finish());
        ((TextView)findViewById(R.id.text_core_ver)).setText(getString(R.string.text_core_ver)
                + NativeVR720.getNativeVersion());
    }
}
