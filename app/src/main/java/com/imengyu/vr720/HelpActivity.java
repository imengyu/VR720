package com.imengyu.vr720;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.imengyu.vr720.utils.StatusBarUtils;
import com.imengyu.vr720.widget.MyTitleBar;

import java.lang.ref.WeakReference;

public class HelpActivity extends AppCompatActivity {

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);

        StatusBarUtils.setLightMode(this);

        MyTitleBar title_bar = findViewById(R.id.toolbar);
        title_bar.setTitle(getTitle());
        title_bar.setLeftIconOnClickListener((View v) -> finish());

        WebView myWebView = findViewById(R.id.webview_help);

        WebSettings webSettings = myWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);

        myWebView.addJavascriptInterface(new JSInterface(this), "jsi");
        myWebView.loadUrl("file:///android_asset/help.html");
    }

    public static class JSInterface {

        private final WeakReference<HelpActivity> mTarget;

        public JSInterface(HelpActivity activity) {
            mTarget = new WeakReference<>(activity);
        }

        @JavascriptInterface
        public void openPage(String url) {
            Intent intent = new Intent(mTarget.get(), HtmlActivity.class);
            intent.putExtra("url", url);
            mTarget.get().startActivity(intent);
        }
        @JavascriptInterface
        public boolean getIsGithubBuild() {
            return false;
        }
    }
}