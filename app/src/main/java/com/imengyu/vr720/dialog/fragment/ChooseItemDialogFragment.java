package com.imengyu.vr720.dialog.fragment;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.imengyu.vr720.R;
import com.imengyu.vr720.adapter.SimpleListAdapter;
import com.imengyu.vr720.utils.AlertDialogTool;
import com.imengyu.vr720.utils.PixelTool;
import com.imengyu.vr720.utils.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ChooseItemDialogFragment extends DialogFragment {

    public ChooseItemDialogFragment() {
        setStyle(DialogFragment.STYLE_NORMAL, R.style.CustomDialog);
    }
    public ChooseItemDialogFragment(String title, String[] items) {
        this.items = items;
        this.title = title;
        setStyle(DialogFragment.STYLE_NORMAL, R.style.CustomDialog);
    }

    private String title;
    private String[] items;
    private OnChooseItemListener onChooseItemListener;
    private boolean cancelButtonVisible = true;

    public interface OnChooseItemListener {
        void onChooseItem(boolean choosed, int index, String item);
    }

    public ChooseItemDialogFragment setCancelButtonVisible(boolean cancelButtonVisible) {
        this.cancelButtonVisible = cancelButtonVisible;
        return this;
    }
    public ChooseItemDialogFragment setOnChooseItemListener(OnChooseItemListener onChooseItemListener) {
        this.onChooseItemListener = onChooseItemListener;
        return this;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putBoolean("stateSaved", true);
        outState.putBoolean("cancelButtonVisible", cancelButtonVisible);
        outState.putString("title", title);
        outState.putStringArray("items", items);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        if(savedInstanceState != null && savedInstanceState.getBoolean("stateSaved", false)) {
            title = savedInstanceState.getString("title");
            cancelButtonVisible = savedInstanceState.getBoolean("cancelButtonVisible");
            items = savedInstanceState.getStringArray("items");
        }
        super.onViewStateRestored(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.dialog_choose_item, container, false);
        context = requireContext();
        text_title = v.findViewById(R.id.text_title);
        btn_cancel = v.findViewById(R.id.btn_cancel);

        final List<String> listItems = new ArrayList<>();
        final SimpleListAdapter adapter = new SimpleListAdapter(context, R.layout.item_simple, listItems);

        list_simple = v.findViewById(R.id.list_simple);
        list_simple.setAdapter(adapter);
        list_simple.setDivider(null);
        list_simple.setDividerHeight(0);
        //添加条目
        listItems.addAll(Arrays.asList(items));
        adapter.notifyDataSetChanged();

        if(StringUtils.isNullOrEmpty(title)) {
            text_title.setVisibility(View.GONE);
        }
        else {
            text_title.setText(title);
            text_title.setVisibility(View.VISIBLE);
        }
        //按钮事件
        btn_cancel.setOnClickListener(view -> {
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
    private Button btn_cancel;
    private TextView text_title;

    @Override
    public void onResume() {
        Window window = requireDialog().getWindow();
        window.setWindowAnimations(R.style.DialogBottomPopup);

        WindowManager.LayoutParams params = window.getAttributes();
        params.width = WindowManager.LayoutParams.MATCH_PARENT;
        params.height = WindowManager.LayoutParams.MATCH_PARENT;
        window.setAttributes(params);

        onConfigurationChanged(getResources().getConfiguration());

        btn_cancel.setVisibility(cancelButtonVisible ? View.VISIBLE : View.GONE);
        if(!StringUtils.isNullOrEmpty(title)) {
            text_title.setText(title);
            text_title.setVisibility(View.VISIBLE);
        } else text_title.setVisibility(View.GONE);
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
