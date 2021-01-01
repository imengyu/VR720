package com.imengyu.vr720.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.imengyu.vr720.R;
import com.imengyu.vr720.list.MainList;
import com.imengyu.vr720.model.holder.MainListViewHolder;
import com.imengyu.vr720.model.list.MainListItem;
import com.imengyu.vr720.utils.DateUtils;

import java.util.Date;
import java.util.List;

/**
 * 主列表适配器
 */
public class MainListAdapter extends CheckableListAdapter<MainListItem> {

    private final MainList mainList;
    private final int layoutId;

    public MainListAdapter(MainList mainList, Context context, int layoutId, List<MainListItem> list) {
        super(context, layoutId, list);
        this.mainList = mainList;
        this.layoutId = layoutId;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        final MainListItem item = getItem(position);

        MainListViewHolder viewHolder;

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(layoutId, parent, false);
            viewHolder = new MainListViewHolder();
            viewHolder.textView = convertView.findViewById(R.id.text_item);
            viewHolder.imageView = convertView.findViewById(R.id.img_item);
            viewHolder.videoMark = convertView.findViewById(R.id.video_mark);
            viewHolder.checkMark = convertView.findViewById(R.id.check_item);

            viewHolder.imageView.setOnLongClickListener(mainList.getMainListBaseOnLongClickListener());
            viewHolder.imageView.setOnClickListener(mainList.getMainListBaseOnClickListener());

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (MainListViewHolder) convertView.getTag();
        }
        if(item != null) {
            if (item.getForceItemType() == MainListItem.ITEM_TYPE_NORMAL) {

                viewHolder.imageView.setTag(position);
                viewHolder.imageView.setVisibility(View.VISIBLE);
                viewHolder.imageView.setImageText(item.getFileName());
                viewHolder.imageView.setLeftTextReserveSpace(item.isVideo());

                if(mainList.getMainSortType() == MainList.MAIN_SORT_DATE)
                    viewHolder.imageView.setImageSize(DateUtils.format(new Date(item.getFileModifyDate()), DateUtils.FORMAT_SHORT));
                else
                    viewHolder.imageView.setImageSize(item.getFileSize());

                if (item.isThumbnailFail())
                    viewHolder.imageView.setImageResource(R.drawable.ic_noprob);
                else if (item.isThumbnailLoading())
                    viewHolder.imageView.setImageResource(R.drawable.ic_tumb);
                else if(item.getThumbnail() != viewHolder.imageView.getDrawable())
                    Glide.with(getContext())
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
            }
            else if (item.getForceItemType() == MainListItem.ITEM_TYPE_TEXT) {
                viewHolder.imageView.setVisibility(View.GONE);
                viewHolder.textView.setVisibility(View.VISIBLE);
                viewHolder.checkMark.setVisibility(View.GONE);
                viewHolder.videoMark.setVisibility(View.GONE);
                viewHolder.textView.setText(item.getFileName());
            }
        }
        return convertView;
    }
}
