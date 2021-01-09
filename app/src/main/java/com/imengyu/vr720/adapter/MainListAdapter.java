package com.imengyu.vr720.adapter;

import android.content.Context;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.imengyu.vr720.R;
import com.imengyu.vr720.list.MainList;
import com.imengyu.vr720.model.OnListCheckableChangedListener;
import com.imengyu.vr720.model.holder.MainListViewHolder;
import com.imengyu.vr720.model.list.MainListItem;
import com.imengyu.vr720.utils.PixelTool;
import com.imengyu.vr720.utils.ScreenUtils;

import java.util.List;

/**
 * 主列表适配器
 */
public class MainListAdapter extends RecyclerView.Adapter<MainListViewHolder> implements CheckableListAdapter<MainListItem> {

    private final MainList mainList;
    private final int layoutId;
    private final Context context;
    private final List<MainListItem> list;
    private final RequestManager requestManager;
    private final int dp5;
    private final int dp10;
    private final int dp160;
    private final int dp30;
    private final int dp24;
    private final int dp100;
    private final int gridItemSize;
    private final Size screenSize;

    public MainListAdapter(MainList mainList, Context context, int layoutId, List<MainListItem> list) {
        this.mainList = mainList;
        this.list = list;
        this.layoutId = layoutId;
        this.context = context;
        this.requestManager =  Glide.with(context);
        this.dp5 = PixelTool.dp2px(context, 5);
        this.dp10 = PixelTool.dp2px(context, 10);
        this.dp160 = PixelTool.dp2px(context, 160);
        this.dp30 = PixelTool.dp2px(context, 45);
        this.dp100 = PixelTool.dp2px(context, 100);
        this.dp24 = PixelTool.dp2px(context, 24);

        screenSize = ScreenUtils.getScreenSize(context);
        gridItemSize = (screenSize.getWidth() / 3 - 3);
    }

    private int lineItemCount = 1;

    public void setLineItemCount(int lineItemCount) {
        this.lineItemCount = lineItemCount;
    }

    private final CompoundButton.OnCheckedChangeListener groupHeaderCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            final MainListItem item = (MainListItem)buttonView.getTag();
            if(item != null) {
                item.setChecked(isChecked);
                if(mainListCheckGroupListener != null)
                    mainListCheckGroupListener.onMainListCheckGroup(item.getFileName(), isChecked);
            }
        }
    };

    @NonNull
    @Override
    public MainListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
        return new MainListViewHolder(v);
    }
    @Override
    public void onBindViewHolder(@NonNull MainListViewHolder viewHolder, int position) {
        final MainListItem item = list.get(position);

        viewHolder.imageView.setOnLongClickListener(mainList.getMainListBaseOnLongClickListener());
        viewHolder.imageView.setOnClickListener(mainList.getMainListBaseOnClickListener());
        viewHolder.item.setVisibility(item.isSearchHidden() ? View.GONE : View.VISIBLE);

        int height = 0;
        int width = 0;

        if (item.getForceItemType() == MainListItem.ITEM_TYPE_NORMAL) {

            viewHolder.imageView.setTag(position);
            viewHolder.imageView.setVisibility(View.VISIBLE);
            viewHolder.imageView.setImageText(item.getFileName());
            viewHolder.imageView.setLeftTextReserveSpace(item.isVideo());
            viewHolder.imageView.setEnableRenderExtras(!isGrid);

            if(mainList.getMainSortType() == MainList.MAIN_SORT_DATE)
                viewHolder.imageView.setImageSize(item.getFileModifyDate());
            else
                viewHolder.imageView.setImageSize(item.getFileSize());

            if (item.isThumbnailLoading())
                viewHolder.imageView.setImageResource(R.drawable.ic_tumb);
            else if (item.isThumbnailFail() || item.getThumbnail() == null)
                viewHolder.imageView.setImageResource(R.drawable.ic_noprob);
            else if(item.getThumbnail() != viewHolder.imageView.getDrawable())
                viewHolder.imageView.setImageDrawable(item.getThumbnail());

            if (!item.isThumbnailLoadingStarted()) {
                item.setThumbnailLoadingStarted(true);
                mainList.loadThumbnail(item);
            }

            viewHolder.textView.setVisibility(View.GONE);

            viewHolder.videoMark.setVisibility(item.isVideo() ? View.VISIBLE : View.GONE);
            viewHolder.checkMark.setVisibility(isCheckable() ? View.VISIBLE : View.GONE);
            viewHolder.checkMark.setOnCheckedChangeListener(null);
            viewHolder.checkMark.setTag(null);
            viewHolder.checkMark.setClickable(false);
            viewHolder.checkMark.setChecked(item.isChecked());

            height = isGrid ? gridItemSize : dp160;
            width = isGrid ? gridItemSize : ViewGroup.LayoutParams.MATCH_PARENT;
        }
        else if (item.getForceItemType() == MainListItem.ITEM_TYPE_TEXT) {
            viewHolder.imageView.setVisibility(View.GONE);
            viewHolder.textView.setVisibility(View.VISIBLE);
            viewHolder.checkMark.setVisibility(View.GONE);
            viewHolder.videoMark.setVisibility(View.GONE);
            viewHolder.textView.setText(item.getFileName());
            viewHolder.checkMark.setOnCheckedChangeListener(null);
            viewHolder.checkMark.setClickable(false);
            viewHolder.checkMark.setTag(null);

            height = dp30;
            width = screenSize.getWidth();
        }
        else if (item.getForceItemType() == MainListItem.ITEM_TYPE_GROUP_HEADER) {
            viewHolder.imageView.setVisibility(View.GONE);
            viewHolder.textView.setVisibility(View.VISIBLE);
            viewHolder.checkMark.setChecked(item.isChecked());
            viewHolder.checkMark.setVisibility(isCheckable() ? View.VISIBLE : View.GONE);
            viewHolder.checkMark.setOnCheckedChangeListener(groupHeaderCheckedChangeListener);
            viewHolder.checkMark.setTag(item);
            viewHolder.checkMark.setClickable(true);
            viewHolder.videoMark.setVisibility(View.GONE);
            viewHolder.textView.setText(item.getFileName());

            height = dp30;
            width = screenSize.getWidth();
        }

        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams )viewHolder.imageView.getLayoutParams();
        RelativeLayout.LayoutParams layoutParamsCheck = (RelativeLayout.LayoutParams )viewHolder.checkMark.getLayoutParams();
        RelativeLayout.LayoutParams layoutParamsVideoMark = (RelativeLayout.LayoutParams )viewHolder.videoMark.getLayoutParams();
        if(isGrid) {
            layoutParams.setMargins(0,0,0, 0);
            layoutParamsCheck.rightMargin = dp5;
            layoutParamsCheck.bottomMargin = dp5;
            layoutParamsVideoMark.leftMargin = dp10;
            layoutParamsCheck.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        }
        else {
            layoutParamsCheck.rightMargin = dp30;
            layoutParamsVideoMark.leftMargin = dp24;
            layoutParamsCheck.bottomMargin = 0;
            layoutParamsCheck.removeRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            layoutParams.setMargins(dp10, dp10, dp10, 0);
        }
        viewHolder.videoMark.setLayoutParams(layoutParamsVideoMark);
        viewHolder.checkMark.setLayoutParams(layoutParamsCheck);
        viewHolder.imageView.setLayoutParams(layoutParams);

        viewHolder.view.setMinimumHeight(height);
        viewHolder.item.setMinimumHeight(height);

        RecyclerView.LayoutParams param = (RecyclerView.LayoutParams)viewHolder.view.getLayoutParams();
        if (!item.isSearchHidden()) {
            param.height = height;
            param.width = width;
        } else {
            param.height = 0;
            param.width = 0;
        }
        if(!isGrid) {
            if (getItemCount() > 3 && position == getItemCount() - 1)
                param.setMargins(0, 0, 0, dp100);
            else
                param.setMargins(0, 0, 0, 0);
        } else {

            int lastLineCount = getItemCount() % lineItemCount;
            if(lastLineCount == 0) lastLineCount = lineItemCount;
            for(int i = 0; i < lastLineCount; i++) {
                if(getItem(getItemCount() - 1 - i).getForceItemType() != MainListItem.ITEM_TYPE_NORMAL) {
                    lastLineCount = i + 1;
                    break;
                }
            }

            if (item.getForceItemType() == MainListItem.ITEM_TYPE_NORMAL
                    && getItemCount() > lineItemCount && position >= getItemCount() - lastLineCount)
                param.setMargins(0, 0, 0, dp100);
            else
                param.setMargins(0, 0, 0, 0);
        }

        viewHolder.view.setLayoutParams(param);
    }
    @Override
    public int getItemCount() {
        return list.size();
    }
    @Override
    public MainListItem getItem(int index) {
        return list.get(index);
    }

    private boolean isGrid = false;

    public void setGrid(boolean grid) {
        isGrid = grid;
    }

    private boolean mCheckable;
    private OnListCheckableChangedListener mainListCheckableChangedListener;
    private OnMainListCheckGroupListener mainListCheckGroupListener;

    public interface OnMainListCheckGroupListener {
        void onMainListCheckGroup(String group, boolean check);
    }

    @Override
    public void setCheckable(boolean mCheckable) {
        this.mCheckable = mCheckable;
        if (this.mainListCheckableChangedListener != null)
            this.mainListCheckableChangedListener.onListCheckableChangedListener(mCheckable);
    }

    @Override
    public boolean isCheckable() { return mCheckable; }

    @Override
    public void setMainListCheckableChangedListener(OnListCheckableChangedListener mainListCheckableChangedListener) {
        this.mainListCheckableChangedListener = mainListCheckableChangedListener;
    }

    public void setMainListCheckGroupListener(OnMainListCheckGroupListener mainListCheckGroupListener) {
        this.mainListCheckGroupListener = mainListCheckGroupListener;
    }
}
