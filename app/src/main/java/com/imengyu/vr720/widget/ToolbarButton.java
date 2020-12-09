package com.imengyu.vr720.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.appcompat.widget.AppCompatButton;
import androidx.core.widget.TextViewCompat;

import com.imengyu.vr720.R;

public class ToolbarButton extends AppCompatButton {

    public ToolbarButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    public ToolbarButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public ToolbarButton(Context context) {
        super(context);
        init(context, null);
    }

    private int normalColor = Color.WHITE;
    private int hoverColor = Color.RED;

    private ColorStateList normalColorStateList = null;
    private ColorStateList hoverColorStateList = null;

    private boolean activeable = true;
    private boolean checked = false;

    private void init(Context context, AttributeSet attrs) {

        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ToolbarButton);
            activeable = a.getBoolean(R.styleable.ToolbarButton_activeable, activeable);
            hoverColor = a.getColor(R.styleable.ToolbarButton_hoverColor, hoverColor);
            normalColor = a.getColor(R.styleable.ToolbarButton_normalColor, normalColor);
            a.recycle();
        }

        normalColorStateList = ColorStateList.valueOf(normalColor);
        hoverColorStateList = ColorStateList.valueOf(hoverColor);

        setBackground(null);
    }

    public boolean isChecked() {
        return checked;
    }
    public void setChecked(boolean checked) {
        this.checked = checked;
        setHightLight(checked);
    }

    private void setHightLight(boolean hightlight){
        if(activeable) {
            if (hightlight) {
                setTextColor(hoverColor);
                TextViewCompat.setCompoundDrawableTintList(this, hoverColorStateList);
                setForegroundTintList(hoverColorStateList);
            } else {
                setTextColor(normalColor);
                TextViewCompat.setCompoundDrawableTintList(this, normalColorStateList);
                setForegroundTintList(normalColorStateList);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(event.getAction() == MotionEvent.ACTION_DOWN){
            setHightLight(true);
        }else if(event.getAction() == MotionEvent.ACTION_UP){
            setHightLight(checked);
        }
        return super.onTouchEvent(event);
    }
}
