package com.imengyu.vr720.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.imengyu.vr720.R;
import com.imengyu.vr720.list.MainList;
import com.imengyu.vr720.model.OnListCheckableChangedListener;
import com.imengyu.vr720.model.holder.MainListViewHolder;
import com.imengyu.vr720.model.list.MainListItem;
import com.imengyu.vr720.utils.PixelTool;

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

    public MainListAdapter(MainList mainList, Context context, int layoutId, List<MainListItem> list) {
        this.mainList = mainList;
        this.list = list;
        this.layoutId = layoutId;
        this.context = context;
        this.requestManager =  Glide.with(context);
    }

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

        if (item.getForceItemType() == MainListItem.ITEM_TYPE_NORMAL) {

            viewHolder.imageView.setTag(position);
            viewHolder.imageView.setVisibility(View.VISIBLE);
            viewHolder.imageView.setImageText(item.getFileName());
            viewHolder.imageView.setLeftTextReserveSpace(item.isVideo());

            if(mainList.getMainSortType() == MainList.MAIN_SORT_DATE)
                viewHolder.imageView.setImageSize(item.getFileModifyDate());
            else
                viewHolder.imageView.setImageSize(item.getFileSize());

            if (item.isThumbnailLoading())
                viewHolder.imageView.setImageResource(R.drawable.ic_tumb);
            else if (item.isThumbnailFail() || item.getThumbnail() == null)
                viewHolder.imageView.setImageResource(R.drawable.ic_noprob);
            else if(item.getThumbnail() != viewHolder.imageView.getDrawable())
                requestManager
                        .load(item.getThumbnail())
                        .placeholder(R.drawable.ic_tumb)
                        .error(R.drawable.ic_noprob)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .into(viewHolder.imageView);

            if (!item.isThumbnailLoadingStarted()) {
                item.setThumbnailLoadingStarted(true);
                mainList.loadThumbnail(item);
            }

            viewHolder.textView.setVisibility(View.GONE);

            viewHolder.videoMark.setVisibility(item.isVideo() ? View.VISIBLE : View.GONE);
            viewHolder.checkMark.setChecked(item.isChecked());
            viewHolder.checkMark.setVisibility(isCheckable() ? View.VISIBLE : View.GONE);

            height = PixelTool.dp2px(context, 160);
        }
        else if (item.getForceItemType() == MainListItem.ITEM_TYPE_TEXT) {
            viewHolder.imageView.setVisibility(View.GONE);
            viewHolder.textView.setVisibility(View.VISIBLE);
            viewHolder.checkMark.setVisibility(View.GONE);
            viewHolder.videoMark.setVisibility(View.GONE);
            viewHolder.textView.setText(item.getFileName());

            height = PixelTool.dp2px(context, 30);
        }

        RecyclerView.LayoutParams param = (RecyclerView.LayoutParams)viewHolder.item.getLayoutParams();
        if (!item.isSearchHidden()) {
            param.height = height;
            param.width = ViewGroup.LayoutParams.MATCH_PARENT;
        }else{
            param.height = 0;
            param.width = 0;
        }
        if (getItemCount() > 3 && position == getItemCount() - 1)
            param.setMargins(0, 0, 0, PixelTool.dip2px(context, 100));
        else
            param.setMargins(0, 0, 0,0);

        viewHolder.item.setLayoutParams(param);


    }
    @Override
    public int getItemCount() {
        return list.size();
    }
    @Override
    public MainListItem getItem(int index) {
        return list.get(index);
    }

    private boolean mCheckable;
    private OnListCheckableChangedListener mainListCheckableChangedListener;

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
}
