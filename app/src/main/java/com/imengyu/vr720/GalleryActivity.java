package com.imengyu.vr720;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;

import com.donkingliang.imageselector.utils.ImageSelector;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.hjq.toast.ToastUtils;
import com.imengyu.vr720.config.Codes;
import com.imengyu.vr720.config.MainMessages;
import com.imengyu.vr720.dialog.AppDialogs;
import com.imengyu.vr720.dialog.CommonDialog;
import com.imengyu.vr720.fragment.IMainFragment;
import com.imengyu.vr720.list.GalleryGridList;
import com.imengyu.vr720.list.GalleryList;
import com.imengyu.vr720.model.GalleryItem;
import com.imengyu.vr720.model.ImageItem;
import com.imengyu.vr720.model.list.GalleryListItem;
import com.imengyu.vr720.model.list.MainListItem;
import com.imengyu.vr720.service.ListDataService;
import com.imengyu.vr720.utils.FileUtils;
import com.imengyu.vr720.utils.StatusBarUtils;
import com.imengyu.vr720.widget.MyTitleBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.os.Message;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.GridView;
import android.widget.PopupMenu;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GalleryActivity extends AppCompatActivity {

    private Resources resources;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);

        Intent intent = getIntent();
        currentGalleryId = intent.getIntExtra("galleryId", 0);

        listDataService = ((VR720Application)getApplication()).getListDataService();
        galleryGridList = new GalleryGridList(this, ((VR720Application)getApplication()).getListImageCacheService());
        resources = getResources();

        initView();
        initMenu();
        initGallery();
    }

    private int currentGalleryId = 0;
    private boolean currentGalleryChanged = false;
    private GalleryItem currentGallery = null;
    private String currentGalleryName = "";

    private GalleryGridList galleryGridList = null;
    private ListDataService listDataService = null;

    private MyTitleBar titleBar;

    //====================================================
    //菜单
    //====================================================

    private PopupMenu mainMenu;

    private void initMenu() {
        mainMenu = new PopupMenu(this, titleBar.getRightButton(), Gravity.TOP);
        mainMenu.getMenuInflater().inflate(R.menu.menu_gallery, mainMenu.getMenu());
        mainMenu.setOnMenuItemClickListener(this::onOptionsItemSelected);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId() == R.id.action_import_pano) {
            ImageSelector.builder()
                    .useCamera(false)
                    .setSingle(false)
                    .setMaxSelectCount(0)
                    .canPreview(false)
                    .start(this, Codes.REQUEST_CODE_OPENIMAGE);
        } else if(item.getItemId() == R.id.action_sort_name) {
            galleryGridList.sort(GalleryGridList.SORT_NAME);
        } else if(item.getItemId() == R.id.action_sort_date) {
            galleryGridList.sort(GalleryGridList.SORT_DATE);
        } else if(item.getItemId() == R.id.action_sort_size) {
            galleryGridList.sort(GalleryGridList.SORT_SIZE);
        } else if(item.getItemId() == R.id.action_rename) {
            onRenameGalleryClick();
        }
        return super.onOptionsItemSelected(item);
    }

    //====================================================
    //初始化视图
    //====================================================

    private void initView() {

        StatusBarUtils.setLightMode(this);

        titleBar = findViewById(R.id.titlebar);
        titleBar.setLeftIconOnClickListener(v -> onBackPressed());
        titleBar.setRightIconOnClickListener((v) -> onMoreClicked());

        GridView gridView = findViewById(R.id.grid_gallery);
        gridView.setAdapter(galleryGridList.getListAdapter());
        View layout_empty = findViewById(R.id.layout_empty);

        gridView.setEmptyView(layout_empty);

        View layout_selection = findViewById(R.id.layout_selection);
        layout_selection.setVisibility(View.GONE);
        Button button_mainsel_openwith = layout_selection.findViewById(R.id.button_mainsel_openwith);
        Button button_mainsel_share = layout_selection.findViewById(R.id.button_mainsel_share);
        Button button_mainsel_delete = layout_selection.findViewById(R.id.button_mainsel_delete);
        Button button_mainsel_add_to = layout_selection.findViewById(R.id.button_mainsel_add_to);

        button_mainsel_openwith.setOnClickListener(v -> onOpenImageWithClick());
        button_mainsel_delete.setOnClickListener(v -> onDeleteImageClick());
        button_mainsel_share.setOnClickListener(v -> onShareImageClick());
        button_mainsel_add_to.setOnClickListener(v -> onAddImageToClick());

        galleryGridList.init(handler, gridView);
        galleryGridList.setListCheckableChangedListener(checkable -> {
            if (checkable) {
                AnimationSet animationSet = (AnimationSet) AnimationUtils.loadAnimation(GalleryActivity.this, R.anim.bottom_up);
                layout_selection.startAnimation(animationSet);
                layout_selection.setVisibility(View.VISIBLE);

                onTitleSelectionChanged(true, 0, false);

            } else {
                AnimationSet animationSet = (AnimationSet) AnimationUtils.loadAnimation(GalleryActivity.this, R.anim.bottom_down);
                layout_selection.startAnimation(animationSet);
                layout_selection.setVisibility(View.GONE);

                onTitleSelectionChanged(false, 0, false);
            }
        });
        galleryGridList.setListCheckItemCountChangedListener((count) -> {

            button_mainsel_openwith.setEnabled(count == 1);
            button_mainsel_share.setEnabled(count == 1);
            button_mainsel_delete.setEnabled(count > 0);
            button_mainsel_add_to.setEnabled(count > 0);

            onTitleSelectionChanged(
                        galleryGridList.isListCheckMode(), count, count == galleryGridList.getCheckableItemsCount());
        });
        galleryGridList.setListOnItemClickListener((parent, v, position, id) -> {
            MainListItem item = galleryGridList.getListAdapter().getItem(position);
            if(item != null) {
                onOpenImageClick(item.getFilePath());
            }
        });
    }
    private void initGallery() {
        if(currentGalleryId == 0) {
            ToastUtils.show("Bad galleryId");
            finish();
            return;
        }
        currentGallery = listDataService.getGalleryItem(currentGalleryId);
        currentGalleryName = currentGallery.name;
        List<ImageItem> list = listDataService.collectGalleryItems(currentGalleryId);

        for (ImageItem item : list)
            galleryGridList.addItem(new MainListItem(item), false);
        galleryGridList.notifyChange();

        titleBar.setTitle(currentGalleryName);
    }

    //====================================================
    //列表事件
    //====================================================

    private void onOpenImageClick(String path) {
        Intent intent = new Intent(this, PanoActivity.class);
        intent.putExtra("filePath", path);
        intent.putCharSequenceArrayListExtra("fileList", galleryGridList.getListPathItems());
        startActivityForResult(intent, Codes.REQUEST_CODE_PANO);
    }
    private void onShareImageClick() {
        final List<MainListItem> sel = galleryGridList.getSelectedItems();
        if(sel.size() > 0)
            FileUtils.shareFile(this, sel.get(0).getFilePath());
    }
    private void onDeleteImageClick() {
        final List<MainListItem> sel = galleryGridList.getSelectedItems();
        if(sel.size() > 0){
            new CommonDialog(this)
                    .setTitle(resources.getString(R.string.text_sure_delete_pano))
                    .setMessage(String.format(resources.getString(R.string.text_you_can_remove_choose_images_from_gallery), sel.size()))
                    .setNegative(resources.getText(R.string.action_sure_delete))
                    .setPositive(resources.getText(R.string.action_cancel))
                    .setCheckText(resources.getText(R.string.text_also_delete_from_main_list))
                    .setOnClickBottomListener(new CommonDialog.OnClickBottomListener() {
                        @Override
                        public void onNegativeClick(CommonDialog dialog) {
                            //delete files
                            if(dialog.isCheckBoxChecked()) {
                                //delete in listDataService
                                for(MainListItem item : sel)
                                    listDataService.removeImageItem(item.getImageItem());
                            }
                            for(MainListItem item : sel)
                                item.getImageItem().belongGalleries.remove(currentGalleryId);
                            //del
                            galleryGridList.deleteItems(sel);
                            galleryGridList.setListCheckMode(false);
                            //标记
                            currentGalleryChanged = true;
                            dialog.dismiss();
                        }
                        @Override
                        public void onPositiveClick(CommonDialog dialog) { dialog.dismiss(); }
                    })
                    .show();
        }
    }
    private void onOpenImageWithClick() {
        final List<MainListItem> sel = galleryGridList.getSelectedItems();
        if(sel.size() == 1)
            FileUtils.openFile(this, sel.get(0).getFilePath());
    }
    private void onAddImageToClick() {
        final List<MainListItem> sel = galleryGridList.getSelectedItems();
        if(sel.size() > 0) {
            AppDialogs.showChooseGalleryDialog(handler, this, listDataService, galleryId -> {

                AppDialogs.showChooseItemDialog(GalleryActivity.this, getString(R.string.text_choose_move_method),
                        new String[] {
                                getString(R.string.text_method_move),
                                getString(R.string.text_method_copy)
                        } , (choose, i, c) -> {

                    if(!choose)
                        return;

                    boolean isMove = i == 0;

                    //添加到对应相册
                    ImageItem imageItem;
                    for(MainListItem item : sel) {
                        imageItem = item.getImageItem();
                        if(!imageItem.isInBelongGalleries(galleryId))
                            imageItem.belongGalleries.add(galleryId);
                        if(isMove)
                            imageItem.belongGalleries.remove((Object)currentGalleryId);
                    }

                    currentGalleryChanged = true;
                    galleryGridList.setListCheckMode(false);

                    ToastUtils.show(R.string.text_add_success);
                });
            });
        }
    }
    private void onRenameGalleryClick() {
        new CommonDialog(this)
                .setEditTextVisible(true)
                .setEditHint(getString(R.string.text_enter_gallery_name))
                .setEditText(currentGallery.name)
                .setEditTextOnTextChangedListener((newText, dialog) -> dialog.setPositiveEnabled(newText.length() > 0))
                .setTitle(getString(R.string.text_rename_gallery))
                .setOnClickBottomListener(new CommonDialog.OnClickBottomListener() {
                    @Override
                    public void onPositiveClick(CommonDialog dialog) {
                        dialog.dismiss();
                        String newName = dialog.getEditText().getText().toString();
                        titleBar.setTitle(newName);
                        currentGalleryChanged = true;
                        listDataService.renameGalleryItem(currentGallery.id, newName);
                    }
                    @Override
                    public void onNegativeClick(CommonDialog dialog) { dialog.dismiss(); }
                })
                .show();
    }

    //====================================================
    //其他事件
    //====================================================

    //标题栏选择数据更改
    private void onTitleSelectionChanged(boolean isSelectionMode, int selCount, boolean isAll) {
        if(isSelectionMode) {

            DrawableCompat.setTint(titleBar.getRightButton().getForeground(),
                    isAll ?
                            resources.getColor(R.color.colorPrimary, null) :
                            Color.BLACK);

            titleBar.setTitle(selCount > 0 ?
                    String.format(getString(R.string.text_choosed_items), selCount) :
                    resources.getString(R.string.text_please_choose_item));
            titleBar.setLeftButtonIconResource(R.drawable.ic_close);
            titleBar.setRightButtonIconResource(R.drawable.ic_check_all);
        }
        else {
            DrawableCompat.setTint(titleBar.getRightButton().getForeground(), Color.BLACK);
            titleBar.setTitle(currentGalleryName);
            titleBar.setLeftButtonIconResource(R.drawable.ic_back);
            titleBar.setRightButtonIconResource(R.drawable.ic_more);
            titleBar.setCustomViewsVisible(View.VISIBLE);
        }
    }
    //标题栏更多按钮点击
    private void onMoreClicked() {
        if(galleryGridList.isListCheckMode()) {
            if(galleryGridList.getSelectedItemCount() >= galleryGridList.getCheckableItemsCount())
                galleryGridList.clearSelectedItems();
            else galleryGridList.selectAllItems();
        } else mainMenu.show();
    }
    @Override
    public void onBackPressed() {
        if(galleryGridList.isListCheckMode())
            galleryGridList.setListCheckMode(false);
        else back();
    }

    private void back() {
        Intent intent = new Intent();
        intent.putExtra("galleryId", currentGalleryId);
        intent.putExtra("galleryChanged", currentGalleryChanged);
        setResult(0, intent);
        finish();
    }

    //====================================================
    //handler
    //====================================================

    private static class SubHandler extends Handler {
        private final WeakReference<GalleryActivity> mTarget;

        SubHandler(GalleryActivity target) {
            mTarget = new WeakReference<>(target);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MainMessages.MSG_REFRESH_LIST: {
                    mTarget.get().galleryGridList.refresh();
                    break;
                }
            }
        }
    }
    private final SubHandler handler = new SubHandler(this);

    //====================================================
    //回调
    //====================================================

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == Codes.REQUEST_CODE_OPENIMAGE && data != null) {
            //获取选择器返回的数据
            ArrayList<String> images = data.getStringArrayListExtra(ImageSelector.SELECT_RESULT);
            if(images!=null) {
                for (String path : images)
                    galleryGridList.addItem(new MainListItem(listDataService.addImageItem(path, currentGalleryId)), false);
                galleryGridList.sort();
                galleryGridList.notifyChange();
            }
        }
        else if (requestCode == Codes.REQUEST_CODE_PANO && data != null) {
            if(data.getBooleanExtra("isDeleteFile", false)) {
                MainListItem item = galleryGridList.findImageItem(data.getStringExtra("filePath"));
                if(item != null) {
                    listDataService.removeImageItem(item.getImageItem());
                    galleryGridList.deleteItem(item);
                    galleryGridList.notifyChange();
                    currentGalleryChanged = true;
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}