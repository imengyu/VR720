package com.imengyu.vr720.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import com.imengyu.vr720.R;

/**
 * 自定义标题栏
 */
public class MyTitleBar extends ConstraintLayout {

  public static final int TITLE_MODE_ONE = 10;
  public static final int TITLE_MODE_ONE_AND_TEXT = 11;
  public static final int TITLE_MODE_TWO = 12;

  private LinearLayout viewCustom;
  private Button ivBack;
  private Button ivMore;
  private TextView tvTitle;
  private TextView tvMore;
  private int titleMode;
  private boolean titleBarDark;

  public Button getLeftButton() { return ivBack; }
  public Button getRightButton() { return ivMore; }

  public MyTitleBar(Context context, AttributeSet attrs) {
    super(context, attrs);

    initView(context,attrs);
  }

  //初始化视图
  private void initView(final Context context, AttributeSet attributeSet) {
    View inflate = LayoutInflater.from(context).inflate(R.layout.layout_titlebar, this);
    ivBack = inflate.findViewById(R.id.btn_back);
    tvTitle = inflate.findViewById(R.id.text_title);
    tvMore = inflate.findViewById(R.id.text_more);
    ivMore = inflate.findViewById(R.id.btn_more);
    viewCustom = inflate.findViewById(R.id.view_custom);

    init(context,attributeSet);
  }

  //初始化资源文件
  public void init(Context context, AttributeSet attributeSet){
    TypedArray typedArray = context.obtainStyledAttributes(attributeSet, R.styleable.MyTitleBar);
    String title = typedArray.getString(R.styleable.MyTitleBar_title);//标题
    int leftIcon = typedArray.getResourceId(R.styleable.MyTitleBar_left_icon, R.drawable.ic_back);//左边图片
    int rightIcon = typedArray.getResourceId(R.styleable.MyTitleBar_right_icon, R.drawable.ic_more);//右边图片
    String rightText = typedArray.getString(R.styleable.MyTitleBar_right_text);//右边文字
    int titleBarType = typedArray.getInt(R.styleable.MyTitleBar_title_bar_type, 10);//标题栏类型,默认为10
    boolean titleBarDark = typedArray.getBoolean(R.styleable.MyTitleBar_title_bar_dark, false);//标题栏类型,默认为10
    typedArray.recycle();

    //赋值进去我们的标题栏
    tvTitle.setText(title);
    setLeftButtonIconResource(leftIcon);
    tvMore.setText(rightText);
    setRightButtonIconResource(rightIcon);
    setTitleBarDark(titleBarDark);
    setTitleMode(titleBarType);
  }

  public void addCustomView(View view) {
    viewCustom.addView(view);
  }
  public void removeCustomView(View view) {
    viewCustom.removeView(view);
  }
  public void setCustomViewsVisible(int visible) {
    viewCustom.setVisibility(visible);
  }

  public void setRightButtonIcon(Drawable icon) {
    ivMore.setForeground(icon);
  }
  public void setLeftButtonIcon(Drawable icon) {
    ivBack.setForeground(icon);
  }
  public void setRightButtonIconResource(int leftIcon) {
    ivMore.setForeground(getResources().getDrawable(leftIcon, null));
  }
  public void setLeftButtonIconResource(int rightIcon) {
    ivBack.setForeground(getResources().getDrawable(rightIcon,null));
  }


  /**
   * 获取标题栏是否是暗黑模式
   * @return 标题栏是否是暗黑模式
   */
  public boolean isTitleBarDark() {
    return titleBarDark;
  }
  /**
   * 设置标题栏是否是暗黑模式
   * @param titleBarDark 是否是暗黑模式
   */
  public void setTitleBarDark(boolean titleBarDark) {
    this.titleBarDark = titleBarDark;
    if(titleBarDark) {
      tvTitle.setTextColor(Color.WHITE);
      tvMore.setTextColor(Color.WHITE);
    } else {
      tvTitle.setTextColor(Color.BLACK);
      tvMore.setTextColor(Color.BLACK);
    }
  }
  /**
   * 获取标题栏模式
   * @return TITLE_MODE_*
   */
  public int getTitleMode() {
    return titleMode;
  }
  /**
   * 设置标题栏模式
   * @param titleMode TITLE_MODE_*
   */
  public void setTitleMode(int titleMode) {
    this.titleMode = titleMode;
    if (titleMode == TITLE_MODE_ONE) {//不传入,默认为10,显示更多 文字,隐藏更多图标按钮
      ivMore.setVisibility(View.GONE);
      tvMore.setVisibility(View.GONE);
    } else if(titleMode == TITLE_MODE_ONE_AND_TEXT) {//传入11,显示更多图标按钮,隐藏更多 文字
      tvMore.setVisibility(View.VISIBLE);
      ivMore.setVisibility(View.GONE);
    } else if(titleMode == TITLE_MODE_TWO) {
      tvMore.setVisibility(View.GONE);
      ivMore.setVisibility(View.VISIBLE);
    }
  }
  /**
   * 左边图片点击事件
   * @param l 事件回调
   */
  public void setLeftIconOnClickListener(OnClickListener l){
    ivBack.setOnClickListener(l);
  }
  /**
   * 右边图片点击事件
   * @param l 事件回调
   */
  public void setRightIconOnClickListener(OnClickListener l){
    ivMore.setOnClickListener(l);
  }
  /**
   * 右边文字点击事件
   * @param l 事件回调
   */
  public void setRightTextOnClickListener(OnClickListener l){
    tvMore.setOnClickListener(l);
  }
  /**
   * 设置标题栏文字
   * @param s 文字
   */
  public void setTitle(String s) {
    this.tvTitle.setText(s);
  }
  /**
   * 设置标题栏文字
   * @param s 文字
   */
  public void setTitle(CharSequence s) {
    this.tvTitle.setText(s);
  }
  /**
   * 设置标题栏文字
   * @param s 文字
   */
  public void setRightText(String s) {
    this.tvMore.setText(s);
  }
  /**
   * 设置标题栏文字
   * @param s 文字
   */
  public void setRightText(CharSequence s) {
    this.tvMore.setText(s);
  }
}