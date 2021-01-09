package com.imengyu.vr720.dialog.fragment;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import androidx.fragment.app.DialogFragment;

import com.huantansheng.easyphotos.models.album.AlbumModel;
import com.huantansheng.easyphotos.models.album.entity.AlbumItem;
import com.imengyu.vr720.R;
import com.imengyu.vr720.adapter.GalleryListAdapter;
import com.imengyu.vr720.model.list.GalleryListItem;
import com.imengyu.vr720.utils.AlertDialogTool;
import com.imengyu.vr720.utils.PixelTool;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class ChooseSystemGalleryDialogFragment extends DialogFragment {

    public ChooseSystemGalleryDialogFragment() {
        setStyle(DialogFragment.STYLE_NORMAL, R.style.CustomDialog);
    }

    private OnChooseGalleryListener onChooseGalleryListener;

    public interface OnChooseGalleryListener {
        void onChooseGallery(AlbumItem item);
    }

    public void setOnChooseGalleryListener(OnChooseGalleryListener onChooseGalleryListener) {
        this.onChooseGalleryListener = onChooseGalleryListener;
    }

    private AlbumModel albumModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.dialog_choose_gallery, container, false);
        context = requireContext();
        handler = new UpdateHandler(this);

        smallGalleryListAdapter = new GalleryListAdapter(requireActivity(),
                null, true, context,
                R.layout.item_gallery_small, listItems);

        list_gallery = v.findViewById(R.id.list_gallery);
        list_gallery.setAdapter(smallGalleryListAdapter);
        list_gallery.setDivider(null);
        list_gallery.setDividerHeight(0);

        //按钮事件
        ((TextView)v.findViewById(R.id.text_title)).setText(getString(R.string.text_choose_system_gallery_to_import));
        v.findViewById(R.id.btn_cancel).setOnClickListener(view -> dismiss());
        list_gallery.setOnItemClickListener((adapterView, view, i, l) -> {
            GalleryListItem item = listItems.get(i);
            if(item != null && onChooseGalleryListener != null) {
                onChooseGalleryListener.onChooseGallery((AlbumItem) item.getData());
                dismiss();
            }
        });

        albumModel = AlbumModel.getInstance();
        albumModel.query(context, this::loadGalleryItems);

        return v;
    }

    private UpdateHandler handler = null;
    private GalleryListAdapter smallGalleryListAdapter = null;
    private final List<GalleryListItem> listItems = new ArrayList<>();
    private Context context;
    private ListView list_gallery;

    private void loadGalleryItems() {

        List<AlbumItem> albums = albumModel.getAlbumItems();

        listItems.clear();
        for (AlbumItem item : albums) {
            GalleryListItem nItem = new GalleryListItem();
            nItem.setThumbnailPath(item.coverImagePath);
            nItem.setImageCount(item.photos.size());
            nItem.setWithSubTitleText(false);
            nItem.setName(item.name);
            nItem.setId(1);
            nItem.setData(item);
            listItems.add(nItem);
        }

        handler.sendEmptyMessage(MSG_UPDATE);
    }

    private static final int MSG_UPDATE = 10;
    private static class UpdateHandler extends Handler {

        private final WeakReference<ChooseSystemGalleryDialogFragment> mTarget;

        public UpdateHandler(ChooseSystemGalleryDialogFragment dialogFragment) {
            super(Looper.myLooper());
            mTarget = new WeakReference<>(dialogFragment);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            if(msg.what == MSG_UPDATE) {
                mTarget.get().smallGalleryListAdapter.notifyDataSetChanged();
            }
            super.handleMessage(msg);
        }
    }

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
