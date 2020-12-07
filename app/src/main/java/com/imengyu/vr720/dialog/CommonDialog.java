package com.imengyu.vr720.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.imengyu.vr720.R;
import com.imengyu.vr720.utils.AlertDialogTool;

public class CommonDialog extends Dialog {
  /**
   * 显示的图片
   */
  private ImageView imageIv ;

  /**
   * 显示的标题
   */
  private TextView titleTv ;

  /**
   * 显示的消息
   */
  private TextView messageTv ;

  /**
   * 确认和取消按钮
   */
  private Button negativeBn ,positiveBn;

  private CheckBox checkBox;

  /**
   * 按钮之间的分割线
   */
  public CommonDialog(Context context) {
    super(context, R.style.WhiteRoundDialog);
  }

  /**
   * 都是内容数据
   */
  private String message;
  private String title;
  private String positive, negative ;
  private String checkText;
  private int imageResId = -1 ;

  /**
   * 底部是否只有一个按钮
   */
  private boolean isSingle = false;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.dialog_common);
    //按空白处不能取消动画
    setCanceledOnTouchOutside(false);
    //初始化界面控件
    initView();
    //初始化界面数据
    refreshView();
    //初始化界面控件的事件
    initEvent();

    AlertDialogTool.setDialogWindowAnimations(this, R.style.DialogBottomPopup);
    AlertDialogTool.setDialogGravity(this, Gravity.BOTTOM);
    AlertDialogTool.setDialogSize(this, WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
  }

  /**
   * 初始化界面的确定和取消监听器
   */
  private void initEvent() {
    //设置确定按钮被点击后，向外界提供监听
    positiveBn.setOnClickListener(v -> {
      if ( onClickBottomListener!= null) {
        onClickBottomListener.onPositiveClick(CommonDialog.this);
      }
    });
    //设置取消按钮被点击后，向外界提供监听
    negativeBn.setOnClickListener(v -> {
      if ( onClickBottomListener!= null) {
        onClickBottomListener.onNegativeClick(CommonDialog.this);
      }
    });
  }

  /**
   * 初始化界面控件的显示数据
   */
  private void refreshView() {
    //如果用户自定了title和message
    if (!TextUtils.isEmpty(title)) {
      titleTv.setText(title);
      titleTv.setVisibility(View.VISIBLE);
    }else {
      titleTv.setVisibility(View.GONE);
    }
    if (!TextUtils.isEmpty(message)) {
      messageTv.setText(message);
    }
    //如果设置按钮的文字
    if (!TextUtils.isEmpty(positive)) {
      positiveBn.setText(positive);
    }else {
      positiveBn.setText(getContext().getString(R.string.action_ok));
    }
    if (!TextUtils.isEmpty(negative)) {
      negativeBn.setText(negative);
    }else {
      negativeBn.setText("取消");
    }
    if (!TextUtils.isEmpty(checkText)) {
      checkBox.setText(checkText);
      checkBox.setVisibility(View.VISIBLE);
    }else {
      checkBox.setVisibility(View.GONE);
    }

    if (imageResId!=-1){
      imageIv.setImageResource(imageResId);
      imageIv.setVisibility(View.VISIBLE);
    }else {
      imageIv.setVisibility(View.GONE);
    }
    /**
     * 只显示一个按钮的时候隐藏取消按钮，回掉只执行确定的事件
     */
    if (isSingle){
      negativeBn.setVisibility(View.GONE);
    }else {
      negativeBn.setVisibility(View.VISIBLE);
    }
  }

  @Override
  public void show() {
    super.show();
    refreshView();
  }

  /**
   * 初始化界面控件
   */
  private void initView() {
    negativeBn = findViewById(R.id.negative);
    positiveBn = findViewById(R.id.positive);
    titleTv = findViewById(R.id.title);
    messageTv = findViewById(R.id.message);
    imageIv = findViewById(R.id.image);
    checkBox = findViewById(R.id.checkBox);
  }

  /**
   * 设置确定取消按钮的回调
   */
  public OnClickBottomListener onClickBottomListener;
  public CommonDialog setOnClickBottomListener(OnClickBottomListener onClickBottomListener) {
    this.onClickBottomListener = onClickBottomListener;
    return this;
  }
  public interface OnClickBottomListener{
    /**
     * 点击确定按钮事件
     */
    void onPositiveClick(CommonDialog dialog);
    /**
     * 点击取消按钮事件
     */
    void onNegativeClick(CommonDialog dialog);
  }

  public String getMessage() {
    return message;
  }

  public CommonDialog setMessage(CharSequence message) {
    this.message = message.toString();
    return this ;
  }
  public CommonDialog setMessage(String message) {
    this.message = message;
    return this ;
  }

  public String getTitle() {
    return title;
  }

  public CommonDialog setTitle(String title) {
    this.title = title;
    return this;
  }
  public CommonDialog setTitle2(CharSequence title) {
    this.title = title.toString();
    return this;
  }

  public String getPositive() {
    return positive;
  }

  public CommonDialog setPositive(String positive) {
    this.positive = positive;
    return this ;
  }
  public CommonDialog setPositive(CharSequence positive) {
    this.positive = positive.toString();
    return this ;
  }

  public String getNegative() {
    return negative;
  }

  public CommonDialog setNegative(String negative) {
    this.negative = negative;
    return this ;
  }
  public CommonDialog setNegative(CharSequence negative) {
    this.negative = negative.toString();
    return this ;
  }

  public int getImageResId() {
    return imageResId;
  }
  public boolean isSingle() {
    return isSingle;
  }

  public CommonDialog setDialogCancelable(boolean cancelable) {
    super.setCancelable(cancelable);
    return this;
  }


  public CommonDialog setSingle(boolean single) {
    isSingle = single;
    return this ;
  }
  public CommonDialog setImageResId(int imageResId) {
    this.imageResId = imageResId;
    return this ;
  }

  public CommonDialog setCheckText(CharSequence message) {
    this.checkText = message.toString();
    return this ;
  }
  public CommonDialog setCheckText(String message) {
    this.checkText = message;
    return this ;
  }

  public boolean isCheckBoxChecked() {
    return checkBox.isChecked();
  }
}
