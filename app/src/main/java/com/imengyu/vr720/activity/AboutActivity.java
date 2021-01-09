package com.imengyu.vr720.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.imengyu.vr720.BuildConfig;
import com.imengyu.vr720.R;
import com.imengyu.vr720.config.Constants;
import com.imengyu.vr720.core.natives.NativeVR720;
import com.imengyu.vr720.utils.AppPages;
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
        ((TextView)findViewById(R.id.text_version_name)).setText(BuildConfig.VERSION_NAME);

        findViewById(R.id.button_back).setOnClickListener(v -> finish());
        findViewById(R.id.button_send_feed_back).setOnClickListener(v -> {
            AppPages.showFeedBack(this);
        });
        findViewById(R.id.button_open_source_license).setOnClickListener(v -> {
            Intent intent = new Intent(this, HtmlActivity.class);
            intent.putExtra("url", Constants.LICENSE_PAGE_URL);
            startActivity(intent);
        });
    }
}
