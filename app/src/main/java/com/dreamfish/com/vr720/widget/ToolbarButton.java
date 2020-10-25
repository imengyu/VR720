package com.dreamfish.com.vr720.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.appcompat.widget.AppCompatButton;

import com.dreamfish.com.vr720.R;

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
    private Drawable normalIcon = null;
    private Drawable hoverIcon = null;

    private boolean activeable = true;
    private boolean checked = false;

    private void init(Context context, AttributeSet attrs) {

        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ToolbarButton);
            activeable = a.getBoolean(R.styleable.ToolbarButton_activeable, activeable);
            hoverColor = a.getColor(R.styleable.ToolbarButton_hoverColor, hoverColor);
            normalColor = a.getColor(R.styleable.ToolbarButton_normalColor, normalColor);
            normalIcon = a.getDrawable(R.styleable.ToolbarButton_normalIcon);
            if(normalIcon != null)
                normalIcon.setBounds(0, 0, normalIcon.getMinimumWidth(),
                    normalIcon.getMinimumHeight());
            hoverIcon = a.getDrawable(R.styleable.ToolbarButton_hoverIcon);
            if(hoverIcon != null)
                hoverIcon.setBounds(0, 0, hoverIcon.getMinimumWidth(),
                    hoverIcon.getMinimumHeight());
        }

        setBackground(null);
    }

    public boolean isChecked() {
        return checked;
    }
    public void setChecked(boolean checked) {
        this.checked = checked;
        setHightlight(checked);
    }

    private void setHightlight(boolean hightlight){
        if(activeable) {
            if (hightlight) {
                setTextColor(hoverColor);
                setCompoundDrawables(null, hoverIcon, null, null);
            } else {
                setCompoundDrawables(null, normalIcon, null, null);
                setTextColor(normalColor);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(event.getAction() == MotionEvent.ACTION_DOWN){
            setHightlight(true);
        }else if(event.getAction() == MotionEvent.ACTION_UP){
            setHightlight(checked);
        }
        return super.onTouchEvent(event);
    }
}
