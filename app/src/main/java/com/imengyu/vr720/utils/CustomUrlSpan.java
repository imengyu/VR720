package com.imengyu.vr720.utils;

import android.content.Context;
import android.content.Intent;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.view.View;
import android.widget.TextView;

import com.imengyu.vr720.HtmlActivity;

public class CustomUrlSpan extends ClickableSpan {

    private final Context context;
    private final String url;

    public CustomUrlSpan(Context context,String url){
        this.context = context;
        this.url = url;
    }

    @Override
    public void onClick(View widget) {
        Intent intent = new Intent(context, HtmlActivity.class);
        intent.putExtra("url", url);
        context.startActivity(intent);
    }

    public static void solveTextViewCustomUrlSpan(Context context, TextView textView) {
        textView.setMovementMethod(LinkMovementMethod.getInstance());
        solveTextViewCustomUrlSpan(context, textView, textView.getText());
    }
    public static void solveTextViewCustomUrlSpan(Context context, TextView textView, CharSequence text) {
        textView.setMovementMethod(LinkMovementMethod.getInstance());
        if (text instanceof Spannable) {
            int end = text.length();
            Spannable spannable = (Spannable) textView.getText();
            URLSpan[] urlSpans = spannable.getSpans(0, end, URLSpan.class);
            if (urlSpans.length == 0) {
                return;
            }
            SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(text);
            // 循环遍历并拦截 所有https://开头的链接
            for (URLSpan uri : urlSpans) {
                String url = uri.getURL();
                if (url.indexOf("https://") == 0) {
                    CustomUrlSpan customUrlSpan = new CustomUrlSpan(context, url);
                    spannableStringBuilder.setSpan(customUrlSpan, spannable.getSpanStart(uri),
                            spannable.getSpanEnd(uri), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
                }
            }
            textView.setText(spannableStringBuilder);
        }
    }
}