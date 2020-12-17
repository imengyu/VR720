package com.imengyu.vr720.utils;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Point;
import android.util.Size;
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

import java.util.ArrayList;

/**
 * 弹出框工具类
 */
public class AlertDialogTool {

    public static AlertDialog buildCustomBottomPopupDialog(Context context, View v) {
        return buildCustomStylePopupDialogGravity(context, v, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, R.style.DialogBottomPopup, true);
    }
    public static AlertDialog buildCustomStylePopupDialogGravity(Context context, View v, int gravity, int anim) {
        return buildCustomStylePopupDialogGravity(context, v, gravity, anim, true);
    }
    public static AlertDialog buildCustomStylePopupDialogGravity(Context context, View v, int gravity, int anim, boolean cancelable) {
        AlertDialog dialog = new AlertDialog.Builder(context, R.style.WhiteRoundDialog)
                .setView(v)
                .setCancelable(cancelable)
                .create();

        setDialogGravity(dialog, gravity);
        setDialogPadding(dialog, 0, 0, 0, 0);
        setDialogSize(dialog, WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);

        return dialog;
    }
    public static AlertDialog.Builder buildBottomPopupDialogBuilder(Context context) {
        return new AlertDialog.Builder(context, R.style.WhiteRoundDialog);
    }
    public static AlertDialog buildBottomPopupDialog(AlertDialog.Builder builder) {
        AlertDialog dialog = builder.create();

        setDialogGravity(dialog, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        setDialogPadding(dialog, 0, 0, 0, 0);
        setDialogSize(dialog, WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);

        dialog.getWindow().setWindowAnimations(R.style.DialogBottomPopup);

        return dialog;
    }

    public interface OnDialogConfigurationChangedListener {
        void onDialogConfigurationChanged(Dialog dialog);
    }

    private static final ArrayList<DialogSizeConfigurationData> receiveConfigurationChangedDialogs = new ArrayList<>();
    private static class DialogSizeConfigurationData {
        public Dialog dialog;
        public int width;
        public int height;
        public OnDialogConfigurationChangedListener onDialogConfigurationChangedListener = null;
    }

    private static boolean isInReceiveConfigurationChangedDialogs(Dialog d) {
        for(DialogSizeConfigurationData data : receiveConfigurationChangedDialogs)
            if(data.dialog == d)
                return true;
        return false;
    }
    private static void deleteReceiveConfigurationChangedDialogs(Dialog d) {
        DialogSizeConfigurationData needRemove = null;
        for(DialogSizeConfigurationData data : receiveConfigurationChangedDialogs)
            if(data.dialog == d) {
                needRemove = data;
                break;
            }
        if(needRemove != null)
            receiveConfigurationChangedDialogs.remove(needRemove);
    }
    private static void addReceiveConfigurationChangedDialogs(Dialog d, int width, int height) {
        DialogSizeConfigurationData data = new DialogSizeConfigurationData();
        data.dialog = d;
        data.width = width;
        data.height = height;
        receiveConfigurationChangedDialogs.add(data);
    }

    /**
     * 设置对话框边距
     */
    public static void setDialogGravity(Dialog dialog, int gravity) {
        Window window = dialog.getWindow();
        assert window != null;
        window.setGravity(gravity);
        window.getDecorView().setPadding(0, 0, 0, 0);
    }
    /**
     * 设置对话框边距
     */
    public static void setDialogPadding(Dialog dialog, int left, int top, int right, int bottom) {
        Window window = dialog.getWindow();
        if(window != null) {
            View view = window.getDecorView();
            view.setPadding(left, top, right, bottom);
        }
    }
    public static void setDialogSize(Dialog dialog, int width, int height) {
        Window window = dialog.getWindow();
        Context context = dialog.getContext();
        if(window != null) {
            WindowManager.LayoutParams layoutParams = window.getAttributes();
            layoutParams.width = width;

            if(!isInReceiveConfigurationChangedDialogs(dialog)) {
                addReceiveConfigurationChangedDialogs(dialog, width, height);
                dialog.setOnDismissListener(dialog1 -> deleteReceiveConfigurationChangedDialogs((Dialog) dialog1));
            }

            //设置对话框在宽屏模式下的最宽宽度
            if(width == ViewGroup.LayoutParams.MATCH_PARENT) {
                Point point = new Point();
                context.getDisplay().getRealSize(point);
                Size screenSize = new Size(point.x, point.y);

                Configuration mConfiguration = context.getResources().getConfiguration(); //获取设置的配置信息
                if (mConfiguration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    layoutParams.width = screenSize.getWidth() > PixelTool.dp2px(context, 500) ?
                            PixelTool.dp2px(context, 500) : width;
                } else if (mConfiguration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                    layoutParams.width = width;
                }
            }

            layoutParams.height = height;
            window.setAttributes(layoutParams);
        }
    }
    public static void setDialogMargin(Dialog dialog, float vertical, float horizontal) {
        setDialogMargin(dialog, vertical, horizontal,
                WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
    }
    public static void setDialogMargin(Dialog dialog, float vertical, float horizontal, int width, int height) {
        Window window = dialog.getWindow();
        if(window != null) {
            WindowManager.LayoutParams layoutParams = window.getAttributes();
            layoutParams.height = height;
            layoutParams.width = width;
            layoutParams.horizontalMargin = horizontal;
            layoutParams.verticalMargin = vertical;
            window.setAttributes(layoutParams);
        }
    }
    public static void setDialogWindowAnimations(Dialog dialog, int anim) {
        Window window = dialog.getWindow();
        if(window != null)
            window.setWindowAnimations(anim);
    }

    public static void setOnDialogConfigurationChangedListener(Dialog dialog, OnDialogConfigurationChangedListener onDialogConfigurationChangedListener) {
        for(DialogSizeConfigurationData data : receiveConfigurationChangedDialogs)
            if(data.dialog == dialog) {
                data.onDialogConfigurationChangedListener = onDialogConfigurationChangedListener;
                break;
            }
    }
    public static void notifyConfigurationChangedForDialog(Activity activity) {
        for(DialogSizeConfigurationData data : receiveConfigurationChangedDialogs) {
            setDialogSize(data.dialog, data.width, data.height);
            if(data.onDialogConfigurationChangedListener != null)
                data.onDialogConfigurationChangedListener.onDialogConfigurationChanged(data.dialog);
        }
    }
}
