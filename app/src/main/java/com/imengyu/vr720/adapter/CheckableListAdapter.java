package com.imengyu.vr720.adapter;

import android.content.Context;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;

import com.imengyu.vr720.model.OnListCheckableChangedListener;

import java.util.List;

public class CheckableListAdapter<T> extends ArrayAdapter<T>  {

    private boolean mCheckable;
    private OnListCheckableChangedListener mainListCheckableChangedListener;

    public CheckableListAdapter(@NonNull Context context, int resource, @NonNull List<T> objects) {
        super(context, resource, objects);
    }

    public void setCheckable(boolean mCheckable) {
        this.mCheckable = mCheckable;
        if (this.mainListCheckableChangedListener != null)
            this.mainListCheckableChangedListener.onListCheckableChangedListener(mCheckable);
    }

    public boolean isCheckable() { return mCheckable; }

    public void setMainListCheckableChangedListener(OnListCheckableChangedListener mainListCheckableChangedListener) {
        this.mainListCheckableChangedListener = mainListCheckableChangedListener;
    }
}
