package com.imengyu.vr720;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.imengyu.vr720.utils.StatusBarUtils;
import com.imengyu.vr720.widget.MyTitleBar;

import java.lang.ref.WeakReference;

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

        myWebView.addJavascriptInterface(new JSInterface(this), "jsi");
        myWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onReceivedTitle(WebView view, String title) {
                title_bar.setTitle(title);
                super.onReceivedTitle(view, title);
            }
        });
        myWebView.loadUrl(url);
    }

    public static class JSInterface {

        private final WeakReference<HtmlActivity> mTarget;

        public JSInterface(HtmlActivity activity) {
            mTarget = new WeakReference<>(activity);
        }

        @JavascriptInterface
        public void openPage(String url) {
            Intent intent = new Intent(mTarget.get(), HtmlActivity.class);
            intent.putExtra("url", url);
            mTarget.get().startActivity(intent);
        }
        @JavascriptInterface
        public String getLanguage() {
            return PreferenceManager.getDefaultSharedPreferences(mTarget.get()).getString("language", "zh");
        }
        @JavascriptInterface
        public void closePage() {
            mTarget.get().finish();
        }
        @JavascriptInterface
        public boolean getIsGithubBuild() {
            return false;
        }
        @JavascriptInterface
        public int getBuildVersion() {
            return BuildConfig.VERSION_CODE;
        }
    }
}