package com.imengyu.vr720.dialog.fragment;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.imengyu.vr720.R;
import com.imengyu.vr720.adapter.GalleryListAdapter;
import com.imengyu.vr720.adapter.SimpleListAdapter;
import com.imengyu.vr720.config.MainMessages;
import com.imengyu.vr720.dialog.CommonDialog;
import com.imengyu.vr720.model.GalleryItem;
import com.imengyu.vr720.model.list.GalleryListItem;
import com.imengyu.vr720.service.ListDataService;
import com.imengyu.vr720.utils.AlertDialogTool;
import com.imengyu.vr720.utils.PixelTool;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class ChooseItemDialogFragment extends DialogFragment {

    public ChooseItemDialogFragment(String title, String[] items) {
        super();
        this.items = items;
        this.title = title;
        setStyle(DialogFragment.STYLE_NORMAL, R.style.CustomDialog);
    }

    private final String title;
    private final String[] items;
    private OnChooseItemListener onChooseItemListener;

    public interface OnChooseItemListener {
        void onChooseItem(boolean choosed, int index, String item);
    }

    public void setOnChooseItemListener(OnChooseItemListener onChooseItemListener) {
        this.onChooseItemListener = onChooseItemListener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.dialog_choose_item, container, false);
        context = requireContext();

        final List<String> listItems = new ArrayList<>();
        final SimpleListAdapter adapter = new SimpleListAdapter(context, R.layout.item_simple, listItems);

        list_simple = v.findViewById(R.id.list_simple);
        ((TextView)v.findViewById(R.id.text_title)).setText(title);
        list_simple.setAdapter(adapter);
        list_simple.setDivider(null);
        list_simple.setDividerHeight(0);
        //添加条目
        listItems.addAll(Arrays.asList(items));
        adapter.notifyDataSetChanged();

        //按钮事件
        v.findViewById(R.id.btn_cancel).setOnClickListener(view -> {
            if(onChooseItemListener != null)
                onChooseItemListener.onChooseItem(false, -1, null);
            dismiss();
        });
        list_simple.setOnItemClickListener((adapterView, view, i, l) -> {
            if (onChooseItemListener != null)
                onChooseItemListener.onChooseItem(true, i, listItems.get(i));
            dismiss();
        });

        return v;
    }

    private Context context;
    private ListView list_simple;

    @Override
    public void onResume() {
        Window window = requireDialog().getWindow();
        window.setWindowAnimations(R.style.DialogBottomPopup);

        WindowManager.LayoutParams params = window.getAttributes();
        params.width = WindowManager.LayoutParams.MATCH_PARENT;
        params.height = WindowManager.LayoutParams.MATCH_PARENT;
        window.setAttributes(params);

        onConfigurationChanged(getResources().getConfiguration());
        super.onResume();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            ViewGroup.LayoutParams layoutParams = list_simple.getLayoutParams();
            layoutParams.height = PixelTool.dp2px(context, items.length >= 4 ? 200 : items.length * 50);
            list_simple.setLayoutParams(layoutParams);
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            ViewGroup.LayoutParams layoutParams = list_simple.getLayoutParams();
            layoutParams.height = PixelTool.dp2px(context, items.length >= 8 ? 400 : items.length * 50);
            list_simple.setLayoutParams(layoutParams);
        }

        AlertDialogTool.bottomDialogSizeAutoSet(requireDialog(),
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        super.onConfigurationChanged(newConfig);
    }


}
