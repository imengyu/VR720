package com.imengyu.vr720;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.imengyu.vr720.utils.StatusBarUtils;
import com.imengyu.vr720.widget.MyTitleBar;

public class HelpActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);

        StatusBarUtils.setLightMode(this);

        MyTitleBar title_bar = findViewById(R.id.toolbar);
        title_bar.setTitle(getTitle());
        title_bar.setLeftIconOnClickListener((View v) -> finish());

        WebView myWebView = findViewById(R.id.webview_help);
        myWebView.loadUrl("file:///android_asset/help.html");

        WebSettings webSettings = myWebView.getSettings();
        webSettings.setJavaScriptEnabled(false);
    }
}