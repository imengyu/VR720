package com.imengyu.vr720;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.imengyu.vr720.core.NativeVR720;
import com.imengyu.vr720.dialog.CommonDialogs;
import com.imengyu.vr720.utils.StatusBarUtils;
import com.imengyu.vr720.widget.MyTitleBar;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        MyTitleBar titleBar = findViewById(R.id.titlebar);
        titleBar.setLeftIconOnClickListener(v -> finish());

        StatusBarUtils.setLightMode(this);


        ((TextView)findViewById(R.id.text_core_ver)).setText(
                String.format("%s%s", getString(R.string.text_core_ver), NativeVR720.getNativeVersion())
        );

        findViewById(R.id.button_back).setOnClickListener(v -> finish());
        findViewById(R.id.button_send_feed_back).setOnClickListener(v -> {
            CommonDialogs.showFeedBack(this);
        });
        findViewById(R.id.button_help).setOnClickListener(v -> {
            CommonDialogs.showHelp(this);
        });
    }
}
