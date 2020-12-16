package com.imengyu.vr720.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.imengyu.vr720.R;

import java.util.List;

public class SimpleListAdapter extends ArrayAdapter<String> {

  private final int layoutId;

  public SimpleListAdapter(Context context, int layoutId, List<String> list) {
    super(context, layoutId, list);
    this.layoutId = layoutId;
  }

  private static class SimpleListViewHolder {
    public TextView textView;
  }

  @NonNull
  @Override
  public View getView(int position, View convertView, @NonNull ViewGroup parent) {
    final String item = getItem(position);

    SimpleListViewHolder viewHolder;

    if (convertView == null) {
      convertView = LayoutInflater.from(getContext()).inflate(layoutId, parent, false);
      viewHolder = new SimpleListViewHolder();
      viewHolder.textView = convertView.findViewById(R.id.text);

      convertView.setTag(viewHolder);
    } else {
      viewHolder = (SimpleListViewHolder) convertView.getTag();
    }

    if(item != null) {
      viewHolder.textView.setText(item);
    }

    return convertView;
  }
}
