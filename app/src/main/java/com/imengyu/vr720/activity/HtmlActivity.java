package com.imengyu.vr720.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.imengyu.vr720.BuildConfig;
import com.imengyu.vr720.R;
import com.imengyu.vr720.annotation.UnUsed;
import com.imengyu.vr720.config.Constants;
import com.imengyu.vr720.dialog.CommonDialog;
import com.imengyu.vr720.utils.StatusBarUtils;
import com.imengyu.vr720.utils.StringUtils;
import com.imengyu.vr720.widget.MyTitleBar;

import java.lang.ref.WeakReference;

public class HtmlActivity extends AppCompatActivity {

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_html);

        StatusBarUtils.setLightMode(this);

        MyTitleBar title_bar = findViewById(R.id.toolbar);
        title_bar.setTitle(getTitle());
        title_bar.setLeftIconOnClickListener((View v) -> finish());

        progress = findViewById(R.id.progress);
        progress.setIndeterminate(true);
        WebView myWebView = findViewById(R.id.webview);

        WebSettings webSettings = myWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setSupportZoom(true);

        myWebView.addJavascriptInterface(new JSInterface(this), "jsi");
        myWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                view.loadUrl(request.getUrl().toString());
                return true;
            }
        });
        myWebView.setWebChromeClient(new WebChromeClient() {

            @Override
            public void onReceivedTitle(WebView view, String title) {
                if("s:help".equals(title))
                    title_bar.setTitle(getString(R.string.title_activity_help));
                else title_bar.setTitle(title);
                super.onReceivedTitle(view, title);
            }
            @Override
            public boolean onJsConfirm(WebView view, String url, String message, JsResult result) {
                new CommonDialog(HtmlActivity.this)
                        .setTitle(R.string.text_tip)
                        .setMessage(message)
                        .setNegative(R.string.action_cancel)
                        .setPositive(R.string.action_ok)
                        .setOnResult((b, dialog) -> {
                            if(b == CommonDialog.BUTTON_POSITIVE)  { result.confirm(); return true; }
                            else if(b == CommonDialog.BUTTON_NEGATIVE) { result.cancel(); return true; }
                            return false;
                        })
                        .show();
                return true;
            }
            @Override
            public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
                new CommonDialog(HtmlActivity.this)
                        .setTitle(getString(R.string.text_tip))
                        .setMessage(message)
                        .setPositive(R.string.action_ok)
                        .setOnResult((b, dialog) -> {
                            if(b == CommonDialog.BUTTON_POSITIVE) { result.confirm(); return true; }
                            return b == CommonDialog.BUTTON_NEGATIVE;
                        })
                        .show();
                return true;
            }
            @Override
            public boolean onJsPrompt(WebView view, String url, String message, String defaultValue, JsPromptResult result) {
                new CommonDialog(HtmlActivity.this)
                        .setTitle(getString(R.string.text_tip))
                        .setMessage(message)
                        .setEditTextHint(" ")
                        .setEditTextValue(defaultValue)
                        .setNegative(R.string.action_cancel)
                        .setPositive(R.string.action_ok)
                        .setOnResult((b, dialog) -> {
                            if(b == CommonDialog.BUTTON_POSITIVE) { result.confirm(); return true; }
                            else if(b == CommonDialog.BUTTON_NEGATIVE) { result.cancel(); return true; }
                            return false;
                        })
                        .show();
                return true;
            }
            @Override
            public void onCloseWindow(WebView window) { finish(); }
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progress.setIndeterminate(false);
                progress.setProgress(newProgress);
                if(newProgress == 100)
                    progress.setVisibility(View.GONE);
                else
                    progress.setVisibility(View.VISIBLE);
                super.onProgressChanged(view, newProgress);
            }
        });

        final String targetUrl = getIntent().getStringExtra("url");

        new Thread(() -> {
            try {
                Thread.sleep(400);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            runOnUiThread(() -> {
                if(StringUtils.isNullOrEmpty(targetUrl)) myWebView.loadUrl(Constants.ERROR_PAGE_URL);
                else myWebView.loadUrl(targetUrl);
            });
        }).start();
    }

    private ProgressBar progress;

    public static class JSInterface {

        private final WeakReference<HtmlActivity> mTarget;

        public JSInterface(HtmlActivity activity) {
            mTarget = new WeakReference<>(activity);
        }

        @UnUsed
        @JavascriptInterface
        public void openPage(String url) {
            Intent intent = new Intent(mTarget.get(), HtmlActivity.class);
            intent.putExtra("url", url);
            mTarget.get().startActivity(intent);
        }
        @UnUsed
        @JavascriptInterface
        public String getLanguage() {
            return PreferenceManager.getDefaultSharedPreferences(mTarget.get()).getString("language", "zh");
        }
        @UnUsed
        @JavascriptInterface
        public void closePage() {
            mTarget.get().finish();
        }
        @UnUsed
        @JavascriptInterface
        public boolean getIsGithubBuild() {
            return false;
        }
        @UnUsed
        @JavascriptInterface
        public int getBuildVersion() {
            return BuildConfig.VERSION_CODE;
        }
    }
}