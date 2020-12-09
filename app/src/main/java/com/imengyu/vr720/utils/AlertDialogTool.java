package com.imengyu.vr720.utils;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.imengyu.vr720.R;

/**
 * 弹出框工具类
 */
public class AlertDialogTool {

    public static AlertDialog buildCustomBottomPopupDialog(Context context, View v) {
        return buildCustomStylePopupDialogGravity(context, v, Gravity.BOTTOM, R.style.DialogBottomPopup, true);
    }
    public static AlertDialog buildCustomStylePopupDialogGravity(Context context, View v, int gravity, int anim) {
        return buildCustomStylePopupDialogGravity(context, v, gravity, anim, true);
    }
    public static AlertDialog buildCustomStylePopupDialogGravity(Context context, View v, int gravity, int anim, boolean cancelable) {
        AlertDialog dialog = new AlertDialog.Builder(context, R.style.WhiteRoundDialog)
                .setView(v)
                .setCancelable(cancelable)
                .create();

        Window window = dialog.getWindow();
        window.setGravity(gravity);
        window.getDecorView().setPadding(0, 0, 0, 0);

        WindowManager.LayoutParams lp = window.getAttributes();
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;

        window.setAttributes(lp);
        window.setWindowAnimations(anim);

        return dialog;
    }
    public static AlertDialog.Builder buildBottomPopupDialogBuilder(Context context) {
        return new AlertDialog.Builder(context, R.style.WhiteRoundDialog);
    }
    public static AlertDialog buildBottomPopupDialog(AlertDialog.Builder builder) {
        AlertDialog dialog = builder.create();

        Window window = dialog.getWindow();
        window.setGravity(Gravity.BOTTOM);
        window.getDecorView().setPadding(0, 0, 0, 0);

        WindowManager.LayoutParams lp = window.getAttributes();
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;

        window.setAttributes(lp);
        window.setWindowAnimations(R.style.DialogBottomPopup);

        return dialog;
    }
    public static AlertDialog buildLoadingDialog(Context context, String text, boolean cancelable) {

        LayoutInflater inflater = LayoutInflater.from(context);
        View v = inflater.inflate(R.layout.dialog_loading, null);

        ((TextView)v.findViewById(R.id.text_title)).setText(text);

        AlertDialog dialog = new AlertDialog.Builder(context, R.style.WhiteRoundDialog)
                .setView(v)
                .setCancelable(cancelable)
                .create();

        Window window = dialog.getWindow();
        window.setGravity(Gravity.CENTER);

        WindowManager.LayoutParams lp = window.getAttributes();
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;

        window.setAttributes(lp);
        window.setWindowAnimations(R.style.DialogFadePopup);

        return dialog;
    }

    public static AlertDialog setDialogButtonBetterStyle(Context context, AlertDialog dialog) {

        Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        LinearLayout.LayoutParams cancelBtnPara = null;
        if(button != null) {
            cancelBtnPara = (LinearLayout.LayoutParams) button.getLayoutParams();
            cancelBtnPara.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            cancelBtnPara.width = ViewGroup.LayoutParams.MATCH_PARENT;
            cancelBtnPara.setMargins(0, 0, 0, 10);
            button.setLayoutParams(cancelBtnPara);
            button.setBackground(ContextCompat.getDrawable(context, R.drawable.btn_round_primary));
            button.setTextColor(Color.WHITE);
        }
        button = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        if(button != null) {

            cancelBtnPara = (LinearLayout.LayoutParams) button.getLayoutParams();
            cancelBtnPara.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            cancelBtnPara.width = ViewGroup.LayoutParams.MATCH_PARENT;
            cancelBtnPara.setMargins(0, 0, 0, 10);
            button.setLayoutParams(cancelBtnPara);
            button.setBackground(ContextCompat.getDrawable(context, R.drawable.btn_round));
        }
        button = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
        if(button != null) {

            cancelBtnPara = (LinearLayout.LayoutParams) button.getLayoutParams();
            cancelBtnPara.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            cancelBtnPara.width = ViewGroup.LayoutParams.MATCH_PARENT;
            cancelBtnPara.setMargins(0, 0, 0, 10);
            button.setLayoutParams(cancelBtnPara);
            button.setBackground(ContextCompat.getDrawable(context, R.drawable.btn_round));
        }
        return dialog;
    }

    /**
     * 设置对话框边距
     */
    public static Dialog setDialogGravity(Dialog dialog, int gravity) {
        Window window = dialog.getWindow();
        assert window != null;
        window.setGravity(gravity);
        window.getDecorView().setPadding(0, 0, 0, 0);
        return dialog;
    }
    /**
     * 设置对话框边距
     */
    public static Dialog setDialogPadding(Dialog dialog, int left, int top, int right, int bottom) {
        Window window = dialog.getWindow();
        if(window != null) {
            View view = window.getDecorView();
            view.setPadding(left, top, right, bottom);
        }
        return dialog;
    }
    public static Dialog setDialogSize(Dialog dialog,int width, int height) {
        Window window = dialog.getWindow();
        if(window != null) {
            WindowManager.LayoutParams layoutParams = window.getAttributes();
            layoutParams.height = height;
            layoutParams.width = width;
            window.setAttributes(layoutParams);
        }
        return dialog;
    }
    public static Dialog setDialogMargin(Dialog dialog, float vertical, float horizontal) {
        return setDialogMargin(dialog, vertical, horizontal,
                WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
    }
    public static Dialog setDialogMargin(Dialog dialog, float vertical, float horizontal, int width, int height) {
        Window window = dialog.getWindow();
        if(window != null) {
            WindowManager.LayoutParams layoutParams = window.getAttributes();
            layoutParams.height = height;
            layoutParams.width = width;
            layoutParams.horizontalMargin = horizontal;
            layoutParams.verticalMargin = vertical;
            window.setAttributes(layoutParams);
        }
        return dialog;
    }
    public static Dialog setDialogWindowAnimations(Dialog dialog, int anim) {
        Window window = dialog.getWindow();
        if(window != null)
            window.setWindowAnimations(anim);
        return dialog;
    }
}
