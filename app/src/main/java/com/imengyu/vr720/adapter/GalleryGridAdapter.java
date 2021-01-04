package com.imengyu.vr720.adapter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;

import com.imengyu.vr720.R;
import com.imengyu.vr720.list.GalleryGridList;
import com.imengyu.vr720.model.OnListCheckableChangedListener;
import com.imengyu.vr720.model.holder.GalleryListViewHolder;
import com.imengyu.vr720.model.list.MainListItem;
import com.imengyu.vr720.utils.ScreenUtils;

import java.util.List;

/**
 * 主列表适配器
 */
public class GalleryGridAdapter extends ArrayAdapter<MainListItem> implements CheckableListAdapter<MainListItem> {

    private final GalleryGridList galleryGridList;
    private final int layoutId;
    private final int itemSize;

    public GalleryGridAdapter(GalleryGridList galleryGridList, Context context, int layoutId, List<MainListItem> list) {
        super(context, layoutId, list);
        this.layoutId = layoutId;
        this.galleryGridList = galleryGridList;

        Size screenSize = ScreenUtils.getScreenSize(context);
        itemSize = screenSize.getWidth() / 3 - 3;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        final MainListItem item = getItem(position);

        GalleryListViewHolder viewHolder;

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(layoutId, parent, false);
            viewHolder = new GalleryListViewHolder();
            viewHolder.view_item_outer = convertView.findViewById(R.id.view_item_outer);
            viewHolder.view_item = convertView.findViewById(R.id.view_item);
            viewHolder.image = convertView.findViewById(R.id.image);
            viewHolder.check = convertView.findViewById(R.id.check);
            viewHolder.text_title = convertView.findViewById(R.id.text);
            viewHolder.video_mark = convertView.findViewById(R.id.video_mark);

            viewHolder.view_item.setOnLongClickListener(galleryGridList.getMainListBaseOnLongClickListener());
            viewHolder.view_item.setOnClickListener(galleryGridList.getMainListBaseOnClickListener());

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (GalleryListViewHolder) convertView.getTag();
        }
        if(item != null) {

            ViewGroup.LayoutParams layoutParams = viewHolder.view_item.getLayoutParams();
            if(item.getForceItemType() == MainListItem.ITEM_TYPE_TEXT) {

                layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
                layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                viewHolder.view_item.setLayoutParams(layoutParams);
                layoutParams = viewHolder.view_item_outer.getLayoutParams();
                layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
                layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                viewHolder.view_item_outer.setLayoutParams(layoutParams);

                viewHolder.text_title.setVisibility(View.VISIBLE);
                viewHolder.text_title.setText(item.getFileName());
                viewHolder.image.setVisibility(View.GONE);

            }
            else {

                layoutParams.width = itemSize;
                layoutParams.height = itemSize;
                viewHolder.view_item.setLayoutParams(layoutParams);
                layoutParams = viewHolder.view_item_outer.getLayoutParams();
                layoutParams.width = itemSize;
                layoutParams.height = itemSize;
                viewHolder.view_item_outer.setLayoutParams(layoutParams);

                if (item.isThumbnailFail())
                    viewHolder.image.setImageResource(R.drawable.ic_noprob_small);
                else if (item.isThumbnailLoading())
                    viewHolder.image.setImageResource(R.drawable.ic_tumb);
                else {

                    //加载缩略图
                    if (!item.isThumbnailLoadingStarted()) {
                        item.setThumbnailLoadingStarted(true);
                        galleryGridList.loadItemThumbnail(item);
                    }

                    //设置缩略图
                    Drawable drawable = item.getThumbnail();
                    if(drawable == null)
                        viewHolder.image.setImageDrawable(ResourcesCompat.getDrawable(galleryGridList.getResources(), R.drawable.ic_image, null));
                    else
                        viewHolder.image.setImageDrawable(drawable);
                }

                viewHolder.text_title.setVisibility(View.GONE);
                viewHolder.view_item.setTag(position);
                viewHolder.image.setTag(position);
                viewHolder.image.setVisibility(View.VISIBLE);
            }

            viewHolder.check.setChecked(item.isChecked());
            viewHolder.check.setVisibility(isCheckable() ? View.VISIBLE : View.GONE);
            viewHolder.video_mark.setVisibility(item.isVideo() ? View.VISIBLE : View.GONE);
        }
        return convertView;
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
