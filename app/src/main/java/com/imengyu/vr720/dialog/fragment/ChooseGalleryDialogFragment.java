package com.imengyu.vr720.dialog.fragment;

import android.app.Activity;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.imengyu.vr720.R;
import com.imengyu.vr720.VR720Application;
import com.imengyu.vr720.adapter.GalleryListAdapter;
import com.imengyu.vr720.config.MainMessages;
import com.imengyu.vr720.dialog.CommonDialog;
import com.imengyu.vr720.model.GalleryItem;
import com.imengyu.vr720.model.list.GalleryListItem;
import com.imengyu.vr720.service.ListDataService;
import com.imengyu.vr720.utils.AlertDialogTool;
import com.imengyu.vr720.utils.PixelTool;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ChooseGalleryDialogFragment extends DialogFragment {

    public ChooseGalleryDialogFragment() {
        setStyle(DialogFragment.STYLE_NORMAL, R.style.CustomDialog);
    }

    private ListDataService listDataService;
    private Handler handler;
    private OnChooseGalleryListener onChooseGalleryListener;

    public interface OnChooseGalleryListener {
        void onChooseGallery(int galleryId);
    }

    public void setOnChooseGalleryListener(OnChooseGalleryListener onChooseGalleryListener) {
        this.onChooseGalleryListener = onChooseGalleryListener;
    }
    public void setHandler(Handler handler) {
        this.handler = handler;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.dialog_choose_gallery, container, false);
        context = requireContext();
        Activity activity = requireActivity();
        this.listDataService = ((VR720Application)activity.getApplication()).getListDataService();

        final List<GalleryListItem> listItems = new ArrayList<>();
        final GalleryListAdapter smallGalleryListAdapter = new GalleryListAdapter(requireActivity(),
                null, true, context,
                R.layout.item_gallery_small, listItems);

        list_gallery = v.findViewById(R.id.list_gallery);
        list_gallery.setAdapter(smallGalleryListAdapter);
        list_gallery.setDivider(null);
        list_gallery.setDividerHeight(0);

        //添加条目
        GalleryListItem addItem = new GalleryListItem();
        addItem.setId(ListDataService.GALLERY_LIST_ID_ADD);
        listItems.add(addItem);
        for (GalleryItem item : listDataService.getGalleryList()) {
            if(item.id == ListDataService.GALLERY_LIST_ID_VIDEOS) continue;//跳过视频收藏夹
            GalleryListItem nItem = new GalleryListItem(item);
            nItem.refresh(listDataService);
            listItems.add(nItem);
        }
        smallGalleryListAdapter.notifyDataSetChanged();

        //按钮事件
        v.findViewById(R.id.btn_cancel).setOnClickListener(view -> dismiss());
        list_gallery.setOnItemClickListener((adapterView, view, i, l) -> {
            GalleryListItem item = listItems.get(i);
            if(item.getId() == ListDataService.GALLERY_LIST_ID_ADD) {
                //新建相册
                new CommonDialog(requireActivity())
                        .setEditTextHint(R.string.text_enter_gallery_name)
                        .setTitle(R.string.action_new_gallery)
                        .setPositiveEnable(false)
                        .setCancelable(true)
                        .setPositive(R.string.action_ok)
                        .setNegative(R.string.action_cancel)
                        .setOnEditTextChangedListener((newText, commonDialog) ->
                                commonDialog.setPositiveEnable(newText.length() > 0))
                        .setOnResult((b, innerDialog) -> {
                            if(b == CommonDialog.BUTTON_POSITIVE) {
                                //添加相册
                                GalleryItem listItem = new GalleryItem();
                                listItem.id = listDataService.getGalleryListMinId();
                                listItem.name = innerDialog.getEditText().getText().toString();
                                listItem.createTime = String.valueOf(new Date().getTime());
                                listDataService.addGalleryItem(listItem);

                                if(handler != null) {
                                    //发送信息到相册界面完成添加 galleryList.addItem(listItem, true);
                                    Message message = new Message();
                                    message.what = MainMessages.MSG_GALLERY_LIST_ADD_ITEM;
                                    message.obj = listItem.id;
                                    handler.sendMessage(message);
                                }

                                if(onChooseGalleryListener != null)
                                    onChooseGalleryListener.onChooseGallery(listItem.id);
                                dismiss();
                                return true;
                            } else return b == CommonDialog.BUTTON_NEGATIVE;
                        })
                        .show();
            }
            else if(onChooseGalleryListener != null) {
                onChooseGalleryListener.onChooseGallery(item.getId());
                dismiss();
            }
        });

        return v;
    }

    private Context context;
    private ListView list_gallery;

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
            ViewGroup.LayoutParams layoutParams = list_gallery.getLayoutParams();
            layoutParams.height = PixelTool.dp2px(context, 180);
            list_gallery.setLayoutParams(layoutParams);
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            ViewGroup.LayoutParams layoutParams = list_gallery.getLayoutParams();
            layoutParams.height = PixelTool.dp2px(context, 380);
            list_gallery.setLayoutParams(layoutParams);
        }

        AlertDialogTool.bottomDialogSizeAutoSet(requireDialog(),
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        super.onConfigurationChanged(newConfig);
    }


}
