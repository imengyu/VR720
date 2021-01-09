package com.imengyu.vr720.model.holder;

import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.imengyu.vr720.R;
import com.imengyu.vr720.widget.MainThumbnailImageView;

/**
 * 列表条目绑定
 */
public class MainListViewHolder extends RecyclerView.ViewHolder {

    public MainListViewHolder(View v) {
        super(v);
        imageView = v.findViewById(R.id.img_item);
        textView = v.findViewById(R.id.text_item);
        checkMark = v.findViewById(R.id.check_item);
        videoMark = v.findViewById(R.id.video_mark);
        item =  v.findViewById(R.id.item);
        view = v;
    }

    public MainThumbnailImageView imageView;
    public TextView textView;
    public CheckBox checkMark;
    public ImageView videoMark;
    public View item;
    public View view;
}
