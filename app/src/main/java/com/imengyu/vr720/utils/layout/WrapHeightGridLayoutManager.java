package com.imengyu.vr720.utils.layout;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class WrapHeightGridLayoutManager extends GridLayoutManager {

    private final int mChildPerLines;
    private final int[] mMeasuredDimension = new int[2];

    public WrapHeightGridLayoutManager(Context context, int spanCount) {
        super(context, spanCount);
        this.mChildPerLines = spanCount;
    }

    @Override
    public boolean isAutoMeasureEnabled() {
        return false;
    }

    @Override
    public void onMeasure(@NonNull RecyclerView.Recycler recycler, @NonNull RecyclerView.State state, int widthSpec, int heightSpec) {

        final int heightMode = View.MeasureSpec.getMode(heightSpec);
        final int widthSize = View.MeasureSpec.getSize(widthSpec);
        final int heightSize = View.MeasureSpec.getSize(heightSpec);
        int height = 0;
        for (int i = 0; i < getItemCount(); ) {
            if(i >= 0 && i < state.getItemCount()) {
                measureScrapChild(recycler, i,
                        View.MeasureSpec.makeMeasureSpec(i, View.MeasureSpec.UNSPECIFIED),
                        View.MeasureSpec.makeMeasureSpec(i, View.MeasureSpec.UNSPECIFIED),
                        mMeasuredDimension);
            }
            height = height + mMeasuredDimension[1];
            i = i + mChildPerLines;
        }

        // If child view is more than screen size, there is no need to make it wrap content. We can use original onMeasure() so we can scroll view.
        if (height >heightSize) {
            switch (heightMode) {
                case View.MeasureSpec.EXACTLY:
                    height = heightSize;
                case View.MeasureSpec.AT_MOST:
                case View.MeasureSpec.UNSPECIFIED:
            }
            setMeasuredDimension(widthSize, height);
        } else {
            super.onMeasure(recycler, state, widthSpec, heightSpec);
        }
    }

    private void measureScrapChild(RecyclerView.Recycler recycler, int position, int widthSpec,
                                   int heightSpec, int[] measuredDimension) {

        View view;
        try {
            view = recycler.getViewForPosition(position);
        }catch (Exception e) {
            return;
        }

        // For adding Item Decor Insets to view
        super.measureChildWithMargins(view, 0, 0);
        RecyclerView.LayoutParams p = (RecyclerView.LayoutParams) view.getLayoutParams();
        int childWidthSpec = ViewGroup.getChildMeasureSpec(widthSpec,
                getPaddingLeft() + getPaddingRight() + getDecoratedLeft(view) + getDecoratedRight(view), p.width);
        int childHeightSpec = ViewGroup.getChildMeasureSpec(heightSpec,
                getPaddingTop() + getPaddingBottom() + getPaddingBottom() + getDecoratedBottom(view), p.height);
        view.measure(childWidthSpec, childHeightSpec);

        // Get decorated measurements
        measuredDimension[0] = getDecoratedMeasuredWidth(view) + p.leftMargin + p.rightMargin;
        measuredDimension[1] = getDecoratedMeasuredHeight(view) + p.bottomMargin + p.topMargin;
        recycler.recycleView(view);
    }
}
