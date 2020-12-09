package com.imengyu.vr720.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.imengyu.vr720.R;
import com.imengyu.vr720.widget.RotateLoading;

public class LoadingDialog extends Dialog {

  public LoadingDialog(@NonNull Context context) {
    super(context, R.style.CustomDialog);
    init();
  }

  private TextView text;
  private RotateLoading rotateLoading;

  private void init() {

  }
  private void initView() {
    text = findViewById(R.id.text);
    rotateLoading = findViewById(R.id.rotateloading);
  }
  private void refreshView() {
    if (!TextUtils.isEmpty(title)) {
      text.setText(title);
      text.setVisibility(View.VISIBLE);
    }else {
      text.setVisibility(View.GONE);
    }
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.dialog_loading);
    setCanceledOnTouchOutside(false);
    initView();
  }

  @Override
  public void show() {
    super.show();
    refreshView();
    rotateLoading.start();
  }

  @Override
  public void hide() {
    super.hide();
    rotateLoading.stop();
  }

  @Override
  protected void onStop() {
    super.onStop();
    rotateLoading.stop();
  }

  private String title;

  public String getTitle() {
    return title;
  }
  public LoadingDialog setTitle(String title) {
    this.title = title;
    return this;
  }

}
