package com.imengyu.vr720;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;

import androidx.appcompat.app.AppCompatActivity;

import com.hjq.toast.ToastUtils;
import com.imengyu.vr720.utils.StatusBarUtils;
import com.imengyu.vr720.widget.MyTitleBar;

public class HtmlActivity extends AppCompatActivity {

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_html);

        String url = getIntent().getStringExtra("url");

        StatusBarUtils.setLightMode(this);

        MyTitleBar title_bar = findViewById(R.id.toolbar);
        title_bar.setTitle(getTitle());
        title_bar.setLeftIconOnClickListener((View v) -> finish());

        WebView myWebView = findViewById(R.id.webview);

        WebSettings webSettings = myWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);

        myWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onReceivedTitle(WebView view, String title) {
                title_bar.setTitle(title);
                super.onReceivedTitle(view, title);
            }
        });
        myWebView.loadUrl(url);


    }

}