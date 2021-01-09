package com.imengyu.vr720.dialog;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.imengyu.vr720.dialog.fragment.CommonDialogFragment;

import java.lang.reflect.Field;

/**
 * 通用对话框封装
 */
public class CommonDialog {

    public static final int BUTTON_UN_KNOW = 0;
    /**
     * 点击了积极按钮
     */
    public static final int BUTTON_POSITIVE = 1;
    /**
     * 点击了消极按钮
     */
    public static final int BUTTON_NEGATIVE = 2;
    /**
     * 点击了中立按钮
     */
    public static final int BUTTON_NEUTRAL = 3;
    /**
     * 点击了 CheckBox
     */
    public static final int BUTTON_CHECK_BOX = 4;

    private OnCommonDialogResult onCommonDialogResult = null;
    private OnCommonDialogEditTextChangedListener onCommonDialogEditTextChangedListener = null;
    private OnCommonDialogCheckBoxCheckedChangedListener onCommonDialogCheckBoxCheckedChangedListener = null;

    public CommonDialog setOnCheckBoxCheckedChangedListener(OnCommonDialogCheckBoxCheckedChangedListener onCommonDialogCheckBoxCheckedChangedListener) {
        this.onCommonDialogCheckBoxCheckedChangedListener = onCommonDialogCheckBoxCheckedChangedListener;
        return this;
    }
    public CommonDialog setOnResult(OnCommonDialogResult onCommonDialogResult) {
        this.onCommonDialogResult = onCommonDialogResult;
        return this;
    }
    public CommonDialog setOnEditTextChangedListener(OnCommonDialogEditTextChangedListener onCommonDialogEditTextChangedListener) {
        this.onCommonDialogEditTextChangedListener = onCommonDialogEditTextChangedListener;
        return this;
    }

    /**
     * 创建通用对话框
     * @param activity 活动
     */
    public CommonDialog(Activity activity) {
        this.activity = activity;

        commonDialogFragment = new CommonDialogFragment();
        commonDialogInternal = commonDialogFragment.createCommonDialogInternal(activity);
        commonDialogInternal.setOnButtonClickListener((b) -> {
            if(onCommonDialogResult != null) {
                if (onCommonDialogResult.onCommonDialogResult(b, this))
                    commonDialogFragment.dismiss();
            } else
                commonDialogFragment.dismiss();
        });
        commonDialogInternal.setOnCheckBoxCheckChangedListener((c) -> {
            if(onCommonDialogCheckBoxCheckedChangedListener != null)
                onCommonDialogCheckBoxCheckedChangedListener.onCheckBoxCheckedChangedListener(c, this);
        });
        commonDialogInternal.setOnEditTextTextChangedListener((e) -> {
            if(onCommonDialogEditTextChangedListener != null)
                onCommonDialogEditTextChangedListener.onEditTextChanged(e, this);
        });
    }

    private final Activity activity;
    private final CommonDialogFragment commonDialogFragment;
    private final CommonDialogInternal commonDialogInternal;

    public CommonDialog setCancelable(boolean cancelable) {
        commonDialogInternal.setCancelable(cancelable);
        return this ;
    }
    public CommonDialog setCanceledOnTouchOutside(boolean cancelable) {
        commonDialogInternal.setCanceledOnTouchOutside(cancelable);
        return this ;
    }
    public CommonDialog setTitle(String title) {
        commonDialogInternal.setTitle(title);
        commonDialogInternal.titleText = title;
        if(commonDialogInternal.isShowing())
            commonDialogInternal.refreshTexts();
        return this ;
    }
    public CommonDialog setTitle(int resId) {
        commonDialogInternal.setTitle(resId);
        commonDialogInternal.titleText = activity.getString(resId);
        if(commonDialogInternal.isShowing())
            commonDialogInternal.refreshTexts();
        return this ;
    }
    public CommonDialog setMessage(String message) {
        commonDialogInternal.messageText = message;
        if(commonDialogInternal.isShowing())
            commonDialogInternal.refreshTexts();
        return this;
    }
    public CommonDialog setMessage(int resId) {
        commonDialogInternal.messageText = activity.getString(resId);
        if(commonDialogInternal.isShowing())
            commonDialogInternal.refreshTexts();
        return this;
    }
    public CommonDialog setCheckBoxText(String message) {
        commonDialogInternal.checkBoxText = message;
        if(commonDialogInternal.isShowing())
            commonDialogInternal.refreshCheck();
        return this;
    }
    public CommonDialog setCheckBoxText(int resId) {
        commonDialogInternal.checkBoxText = activity.getString(resId);
        if(commonDialogInternal.isShowing())
            commonDialogInternal.refreshCheck();
        return this;
    }
    public CommonDialog setCheckBoxText(boolean checked) {
        commonDialogInternal.checkBoxValue = checked;
        if(commonDialogInternal.isShowing())
            commonDialogInternal.refreshCheck();
        return this;
    }
    public CommonDialog setEditTextValue(String message) {
        commonDialogInternal.editTextValue = message;
        if(commonDialogInternal.isShowing())
            commonDialogInternal.refreshEdit();
        return this;
    }
    public CommonDialog setEditTextHint(String message) {
        commonDialogInternal.editTextHint = message;
        if(commonDialogInternal.isShowing())
            commonDialogInternal.refreshEdit();
        return this;
    }
    public CommonDialog setEditTextHint(int resId) {
        commonDialogInternal.editTextHint = activity.getString(resId);
        if(commonDialogInternal.isShowing())
            commonDialogInternal.refreshEdit();
        return this;
    }
    public CommonDialog setImageResource(int resId) {
        commonDialogInternal.imageValue = resId;
        if(commonDialogInternal.isShowing())
            commonDialogInternal.refreshImages();
        return this;
    }
    public CommonDialog setImageDrawable(Drawable drawable) {
        commonDialogInternal.imageValueDrawable = drawable;
        if(commonDialogInternal.isShowing())
            commonDialogInternal.refreshImages();
        return this;
    }
    public CommonDialog setNegative(String text) {
        commonDialogInternal.negativeText = text;
        if(commonDialogInternal.isShowing())
            commonDialogInternal.refreshButtons();
        return this;
    }
    public CommonDialog setPositive(String text) {
        commonDialogInternal.positiveText = text;
        if(commonDialogInternal.isShowing())
            commonDialogInternal.refreshButtons();
        return this;
    }
    public CommonDialog setNeutral(String text) {
        commonDialogInternal.neutralText = text;
        if(commonDialogInternal.isShowing())
            commonDialogInternal.refreshButtons();
        return this;
    }
    public CommonDialog setNegative(int resId) {
        commonDialogInternal.negativeText = activity.getString(resId);
        if(commonDialogInternal.isShowing())
            commonDialogInternal.refreshButtons();
        return this;
    }
    public CommonDialog setPositive(int resId) {
        commonDialogInternal.positiveText = activity.getString(resId);
        if(commonDialogInternal.isShowing())
            commonDialogInternal.refreshButtons();
        return this;
    }
    public CommonDialog setNeutral(int resId) {
        commonDialogInternal.neutralText = activity.getString(resId);
        if(commonDialogInternal.isShowing())
            commonDialogInternal.refreshButtons();
        return this;
    }
    public CommonDialog setNegativeEnable(boolean enable) {
        commonDialogInternal.negativeEnable = enable;
        if(commonDialogInternal.isShowing())
            commonDialogInternal.refreshButtons();
        return this;
    }
    public CommonDialog setPositiveEnable(boolean enable) {
        commonDialogInternal.positiveEnable = enable;
        if(commonDialogInternal.isShowing())
            commonDialogInternal.refreshButtons();
        return this;
    }
    public CommonDialog setNeutralEnable(boolean enable) {
        commonDialogInternal.neutralEnable = enable;
        if(commonDialogInternal.isShowing())
            commonDialogInternal.refreshButtons();
        return this;
    }
    public CommonDialog setCheckBoxEnable(boolean enable) {
        commonDialogInternal.checkBoxEnable = enable;
        if(commonDialogInternal.isShowing())
            commonDialogInternal.refreshCheck();
        return this;
    }
    public CommonDialog setEditTextEnable(boolean enable) {
        commonDialogInternal.editTextEnable = enable;
        if(commonDialogInternal.isShowing())
            commonDialogInternal.refreshEdit();
        return this;
    }

    public boolean isCheckBoxChecked() {
        return commonDialogInternal.checkBox.isChecked();
    }
    public Editable getEditTextValue() {
        return commonDialogInternal.edit.getText();
    }
    public EditText getEditText() {
        return commonDialogInternal.edit;
    }

    /**
     * 显示对话框
     */
    public void show() {
        if(activity instanceof AppCompatActivity)
            commonDialogFragment.show(((AppCompatActivity)activity).getSupportFragmentManager(),
                    "CommonDialog_" + this.hashCode());
    }

    /**
     * 显示对话框(允许状态丢失)
     */
    public void showAllowingStateLoss() {
        try {
            Field dismissed = DialogFragment.class.getDeclaredField("mDismissed");
            dismissed.setAccessible(true);
            dismissed.set(commonDialogFragment, false);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
        try {
            Field shown = DialogFragment.class.getDeclaredField("mShownByMe");
            shown.setAccessible(true);
            shown.set(commonDialogFragment, true);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
        FragmentManager manager = ((AppCompatActivity)activity).getSupportFragmentManager();
        FragmentTransaction ft = manager.beginTransaction();
        ft.add(commonDialogFragment, "CommonDialog_" + this.hashCode());
        ft.commitAllowingStateLoss();
    }
    /**
     * 关闭对话框
     */
    public void dismiss() {
        commonDialogFragment.dismiss();
    }
    /**
     * 隐藏对话框
     */
    public void hide() {
        commonDialogInternal.hide();
    }
}
