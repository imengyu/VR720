package com.imengyu.vr720.adapter;

import android.content.Context;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.imengyu.vr720.R;
import com.imengyu.vr720.list.GalleryGridList;
import com.imengyu.vr720.list.GalleryList;
import com.imengyu.vr720.model.holder.GalleryListViewHolder;
import com.imengyu.vr720.model.list.MainListItem;
import com.imengyu.vr720.service.ListDataService;
import com.imengyu.vr720.utils.PixelTool;

import java.util.List;

/**
 * 主列表适配器
 */
public class GalleryGridAdapter extends CheckableListAdapter<MainListItem> {
    
    private final Context context;
    private final GalleryGridList galleryGridList;
    private final int layoutId;
    private int itemSize;

    public GalleryGridAdapter(GalleryGridList galleryGridList, Context context, int layoutId, List<MainListItem> list) {
        super(context, layoutId, list);
        this.context = context;
        this.layoutId = layoutId;
        this.galleryGridList = galleryGridList;

        Point screenSize = new Point();
        context.getSystemService(WindowManager.class).getDefaultDisplay().getSize(screenSize);
        itemSize = screenSize.x / 3 - 3;
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

            viewHolder.view_item.setOnLongClickListener(galleryGridList.getMainListBaseOnLongClickListener());
            viewHolder.view_item.setOnClickListener(galleryGridList.getMainListBaseOnClickListener());

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (GalleryListViewHolder) convertView.getTag();
        }
        if(item != null) {

            if(item.getForceItemType() == MainListItem.ITEM_TYPE_TEXT) {

                ViewGroup.LayoutParams layoutParams = viewHolder.view_item.getLayoutParams();
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
                viewHolder.check.setChecked(item.isChecked());
                viewHolder.check.setVisibility(isCheckable() ? View.VISIBLE : View.GONE);

            }
            else {

                ViewGroup.LayoutParams layoutParams = viewHolder.view_item.getLayoutParams();
                layoutParams.width = itemSize;
                layoutParams.height = itemSize;
                viewHolder.view_item.setLayoutParams(layoutParams);
                layoutParams = viewHolder.view_item_outer.getLayoutParams();
                layoutParams.width = itemSize;
                layoutParams.height = itemSize;
                viewHolder.view_item_outer.setLayoutParams(layoutParams);

                if (item.isThumbnailFail())
                    viewHolder.image.setImageResource(R.drawable.ic_noprob);
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
                viewHolder.check.setChecked(item.isChecked());
                viewHolder.check.setVisibility(isCheckable() ? View.VISIBLE : View.GONE);
            }
        }
        return convertView;
    }
}
