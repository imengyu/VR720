package com.imengyu.vr720.adapter;

import com.imengyu.vr720.model.OnListCheckableChangedListener;

public interface CheckableListAdapter<T>   {
    void setCheckable(boolean mCheckable);
    boolean isCheckable();
    void setMainListCheckableChangedListener(OnListCheckableChangedListener mainListCheckableChangedListener);
    T getItem(int index);
    void notifyDataSetChanged();
}
