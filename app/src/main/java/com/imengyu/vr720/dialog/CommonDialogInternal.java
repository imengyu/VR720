package com.imengyu.vr720.dialog;

import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.imengyu.vr720.R;

public class CommonDialogInternal extends Dialog {

    public CommonDialogInternal(@NonNull Context context) {
        super(context, R.style.CustomDialog);
        init();
    }

    private void init() {

    }

    public String titleText;
    public String messageText;
    public String negativeText;
    public String positiveText;
    public String neutralText;
    public String editTextValue;
    public String editTextHint;
    public String checkBoxText;
    public int imageValue;
    public Drawable imageValueDrawable;
    public boolean positiveEnable = true;
    public boolean neutralEnable = true;
    public boolean negativeEnable = true;
    public boolean checkBoxEnable = true;
    public boolean editTextEnable = true;
    public boolean checkBoxValue = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_common);

        initView();
        initEvents();
    }

    public TextView title;
    public TextView message;
    public EditText edit;
    public ImageView image;
    public Button negative;
    public Button positive;
    public Button neutral;
    public CheckBox checkBox;
    public View layout_dialog;

    public interface OnButtonClickListener {
        void onButtonClickListener(int b);
    }
    public interface OnCheckBoxCheckChangedListener {
        void onCheckBoxCheckChangedListener(boolean checked);
    }
    public interface OnEditTextTextChangedListener {
        void onEditTextTextChangedListener(Editable editable);
    }

    private OnEditTextTextChangedListener onEditTextTextChangedListener = null;
    private OnCheckBoxCheckChangedListener onCheckBoxCheckChangedListener = null;
    private OnButtonClickListener onButtonClickListener = null;

    public void setOnEditTextTextChangedListener(OnEditTextTextChangedListener onEditTextTextChangedListener) {
        this.onEditTextTextChangedListener = onEditTextTextChangedListener;
    }
    public void setOnCheckBoxCheckChangedListener(OnCheckBoxCheckChangedListener onCheckBoxCheckChangedListener) {
        this.onCheckBoxCheckChangedListener = onCheckBoxCheckChangedListener;
    }
    public void setOnButtonClickListener(OnButtonClickListener onButtonClickListener) {
        this.onButtonClickListener = onButtonClickListener;
    }

    private void initView() {
        title = findViewById(R.id.title);
        message = findViewById(R.id.message);
        edit = findViewById(R.id.edit);
        image = findViewById(R.id.image);
        negative = findViewById(R.id.negative);
        positive = findViewById(R.id.positive);
        neutral = findViewById(R.id.neutral);
        checkBox = findViewById(R.id.checkBox);
        layout_dialog = findViewById(R.id.layout_dialog);
    }
    private void initEvents() {
        positive.setOnClickListener((v) -> {
            if(onButtonClickListener != null)
                onButtonClickListener.onButtonClickListener(CommonDialog.BUTTON_POSITIVE);
        });
        negative.setOnClickListener((v) -> {
            if(onButtonClickListener != null)
                onButtonClickListener.onButtonClickListener(CommonDialog.BUTTON_NEGATIVE);
        });
        neutral.setOnClickListener((v) -> {
            if(onButtonClickListener != null)
                onButtonClickListener.onButtonClickListener(CommonDialog.BUTTON_NEUTRAL);
        });
        edit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }
            @Override
            public void afterTextChanged(Editable editable) {
                if(onEditTextTextChangedListener != null)
                    onEditTextTextChangedListener.onEditTextTextChangedListener(editable);
            }
        });
        checkBox.setOnClickListener((v) -> {
            if(onButtonClickListener != null)
                onButtonClickListener.onButtonClickListener(CommonDialog.BUTTON_CHECK_BOX);
        });
        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if(onCheckBoxCheckChangedListener!=null)
                onCheckBoxCheckChangedListener.onCheckBoxCheckChangedListener(isChecked);
        });
    }

    public void refreshButtons() {
        positive.setEnabled(positiveEnable);
        negative.setEnabled(negativeEnable);
        neutral.setEnabled(neutralEnable);
        //按钮的文字
        if (!TextUtils.isEmpty(positiveText)) {
            positive.setText(positiveText);
        } else positive.setText(getContext().getString(R.string.action_ok));
        if (!TextUtils.isEmpty(negativeText)) {
            negative.setVisibility(View.VISIBLE);
            negative.setText(negativeText);
        } else negative.setVisibility(View.GONE);
        if (!TextUtils.isEmpty(neutralText)) {
            neutral.setVisibility(View.VISIBLE);
            neutral.setText(neutralText);
        } else neutral.setVisibility(View.GONE);
    }
    public void refreshTexts() {
        if (!TextUtils.isEmpty(titleText)) {
            title.setText(titleText);
            title.setVisibility(View.VISIBLE);
        } else title.setVisibility(View.GONE);
        if (!TextUtils.isEmpty(messageText)) {
            message.setText(messageText);
            message.setVisibility(View.VISIBLE);
        }else message.setVisibility(View.GONE);
    }
    public void refreshImages() {
        if (imageValue != -1){
            image.setImageResource(imageValue);
            image.setVisibility(View.VISIBLE);
        }
        else if (imageValueDrawable != null) {
            image.setImageDrawable(imageValueDrawable);
            image.setVisibility(View.VISIBLE);
        } else image.setVisibility(View.GONE);
    }
    public void refreshCheck() {
        checkBox.setEnabled(checkBoxEnable);
        checkBox.setChecked(checkBoxValue);
        if (!TextUtils.isEmpty(checkBoxText)) {
            checkBox.setText(checkBoxText);
            checkBox.setVisibility(View.VISIBLE);
        } else checkBox.setVisibility(View.GONE);
    }
    public void refreshEdit() {
        edit.setEnabled(editTextEnable);
        if (!TextUtils.isEmpty(editTextHint)) {
            edit.setVisibility(View.VISIBLE);
            edit.setHint(editTextHint);
            edit.setText(editTextValue);
        } else if (!TextUtils.isEmpty(editTextValue)) {
            edit.setVisibility(View.VISIBLE);
            edit.setText(editTextValue);
        } else edit.setVisibility(View.GONE);
    }
    public void refreshViews() {
        refreshEdit();
        refreshCheck();
        refreshImages();
        refreshTexts();
        refreshButtons();
    }
}