package com.imengyu.vr720.widget;

import android.content.Context;
import android.util.AttributeSet;

public class RollTextView  extends androidx.appcompat.widget.AppCompatTextView {
    public RollTextView (Context context) {
        super(context);
    }
    public RollTextView (Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    public RollTextView (Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public boolean isFocused() {
        return true;
    }
}
