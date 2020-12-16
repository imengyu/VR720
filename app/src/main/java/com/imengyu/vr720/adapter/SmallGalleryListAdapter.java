package com.imengyu.vr720.adapter;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.imengyu.vr720.R;
import com.imengyu.vr720.VR720Application;
import com.imengyu.vr720.list.GalleryList;
import com.imengyu.vr720.model.holder.GalleryListViewHolder;
import com.imengyu.vr720.model.list.GalleryListItem;
import com.imengyu.vr720.service.ListDataService;
import com.imengyu.vr720.service.ListImageCacheService;

import java.util.List;

/**
 * 主列表适配器
 */
public class SmallGalleryListAdapter extends ArrayAdapter<GalleryListItem> {

    private final Context context;
    private final int layoutId;
    private final Activity activity;
    private final ListImageCacheService listImageCacheService;

    public SmallGalleryListAdapter(Activity activity, int layoutId, List<GalleryListItem> list) {
        super(activity, layoutId, list);
        this.context = activity;
        this.layoutId = layoutId;
        this.activity = activity;
        this.listImageCacheService = ((VR720Application)activity.getApplication()).getListImageCacheService();
    }

    private void loadItemThumbnail(GalleryListItem item) {
        //在背景线程进行缩略图加载
        new Thread(() -> {
            Drawable drawable = listImageCacheService.loadImageThumbnailCache(item.getThumbnailPath());
            if(drawable != null) {
                item.setThumbnail(drawable);
                item.setThumbnailLoading(false);
                activity.runOnUiThread(this::notifyDataSetChanged);
            } else {
                item.setThumbnailLoading(false);
                item.setThumbnailFail(true);
                activity.runOnUiThread(this::notifyDataSetChanged);
            }
        }).start();
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        final GalleryListItem item = getItem(position);

        GalleryListViewHolder viewHolder;

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(layoutId, parent, false);
            viewHolder = new GalleryListViewHolder();
            viewHolder.text_title = convertView.findViewById(R.id.text_title);
            viewHolder.text_subtitle = convertView.findViewById(R.id.text_subtitle);
            viewHolder.image = convertView.findViewById(R.id.image);
            viewHolder.image_thumbnail = convertView.findViewById(R.id.image_thumbnail);
            viewHolder.check = convertView.findViewById(R.id.check);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (GalleryListViewHolder) convertView.getTag();
        }
        if(item != null) {

            viewHolder.image.setTag(position);
            viewHolder.image.setVisibility(View.VISIBLE);
            viewHolder.text_subtitle.setText(item.getSubTitle(context));
            viewHolder.text_title.setText(item.getName());
            viewHolder.text_subtitle.setVisibility(View.VISIBLE);

            Drawable thumbnail = null;

            if (item.isThumbnailLoading()) {
                viewHolder.image_thumbnail.setVisibility(View.INVISIBLE);
                viewHolder.image.setImageResource(R.drawable.ic_image_loading);
                viewHolder.image.setBackgroundColor(ContextCompat.getColor(context, R.color.colorImageDefault));
            } else if (item.isThumbnailFail()) {
                viewHolder.image_thumbnail.setVisibility(View.INVISIBLE);
                viewHolder.image.setImageResource(R.drawable.ic_image_failed);
                viewHolder.image.setBackgroundColor(ContextCompat.getColor(context, R.color.colorImageFailed));
            }
            else {
                if (!item.isThumbnailLoadingStarted()) {
                    item.setThumbnailLoadingStarted(true);
                    loadItemThumbnail(item);
                } else thumbnail = item.getThumbnail();
                viewHolder.image.setBackgroundColor(ContextCompat.getColor(context, R.color.colorImageDefault));
            }

            if (item.id == ListDataService.GALLERY_LIST_ID_ADD) {
                viewHolder.image.setBackgroundColor(ContextCompat.getColor(context, R.color.colorAccent));
                viewHolder.image.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_button_add));
                viewHolder.text_subtitle.setVisibility(View.GONE);
            } else if (item.id == ListDataService.GALLERY_LIST_ID_I_LIKE) {
                viewHolder.image.setBackgroundColor(ContextCompat.getColor(context, R.color.colorImageLike));
                viewHolder.image.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_image_ilike));
            } else if (item.id == ListDataService.GALLERY_LIST_ID_VIDEOS) {
                viewHolder.image.setBackgroundColor(ContextCompat.getColor(context, R.color.colorVideo));
                viewHolder.image.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_video));
            }

            if (thumbnail == null) {
                viewHolder.image.setVisibility(View.VISIBLE);
                viewHolder.image_thumbnail.setVisibility(View.INVISIBLE);

                if (item.id > 0) {
                    thumbnail = ContextCompat.getDrawable(context, R.drawable.ic_image);
                    viewHolder.image.setImageDrawable(thumbnail);
                }

            }else {
                viewHolder.image.setBackgroundColor(Color.TRANSPARENT);
                viewHolder.image.setVisibility(item.id > 0 ? View.INVISIBLE : View.VISIBLE);
                viewHolder.image_thumbnail.setVisibility(View.VISIBLE);
                viewHolder.image_thumbnail.setImageDrawable(thumbnail);
            }
        }
        return convertView;
    }
}
