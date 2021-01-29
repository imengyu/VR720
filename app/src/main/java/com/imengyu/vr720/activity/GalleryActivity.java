package com.imengyu.vr720.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.preference.PreferenceManager;

import com.hjq.toast.ToastUtils;
import com.huantansheng.easyphotos.EasyPhotos;
import com.huantansheng.easyphotos.models.album.entity.Photo;
import com.imengyu.vr720.R;
import com.imengyu.vr720.VR720Application;
import com.imengyu.vr720.config.Codes;
import com.imengyu.vr720.config.Constants;
import com.imengyu.vr720.config.MainMessages;
import com.imengyu.vr720.dialog.CommonDialog;
import com.imengyu.vr720.dialog.fragment.ChooseGalleryDialogFragment;
import com.imengyu.vr720.dialog.fragment.ChooseItemDialogFragment;
import com.imengyu.vr720.dialog.fragment.ChooseSystemGalleryDialogFragment;
import com.imengyu.vr720.list.MainList;
import com.imengyu.vr720.model.GalleryItem;
import com.imengyu.vr720.model.ImageItem;
import com.imengyu.vr720.model.list.MainListItem;
import com.imengyu.vr720.plugin.GlideEngine;
import com.imengyu.vr720.service.ListDataService;
import com.imengyu.vr720.utils.FileUtils;
import com.imengyu.vr720.utils.ShareUtils;
import com.imengyu.vr720.utils.StatusBarUtils;
import com.imengyu.vr720.widget.MyTitleBar;
import com.imengyu.vr720.widget.RecyclerViewEmptySupport;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class GalleryActivity extends AppCompatActivity {

    private Resources resources;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);

        Intent intent = getIntent();
        currentGalleryId = intent.getIntExtra("galleryId", 0);

        listDataService = ((VR720Application)getApplication()).getListDataService();
        galleryGridList = new MainList(this, ((VR720Application)getApplication()).getListImageCacheService());
        resources = getResources();

        initView();
        initMenu();
        loadSettings();
        initGallery();
        onUpdateScreenOrientation(getResources().getConfiguration());
    }

    @Override
    protected void onPause() {
        saveSettings();
        super.onPause();
    }

    private int currentGalleryId = 0;
    private boolean currentGalleryChanged = false;
    private GalleryItem currentGallery = null;
    private String currentGalleryName = "";

    private MainList galleryGridList = null;
    private ListDataService listDataService = null;

    private MyTitleBar titleBar;

    //====================================================
    //菜单
    //====================================================

    private PopupMenu mainMenu;
    private MenuItem action_sort_date;
    private MenuItem action_sort_name;
    private MenuItem action_sort_size;
    private MenuItem action_list_mode;
    private MenuItem action_show_hide_date;

    private void initMenu() {
        mainMenu = new PopupMenu(this, titleBar.getRightButton(), Gravity.TOP);
        Menu menu = mainMenu.getMenu();
        mainMenu.getMenuInflater().inflate(R.menu.menu_gallery, menu);
        mainMenu.setOnMenuItemClickListener(this::onOptionsItemSelected);

        action_list_mode = menu.findItem(R.id.action_list_mode);
        action_show_hide_date = menu.findItem(R.id.action_show_hide_date);
        action_sort_date = menu.findItem(R.id.action_sort_date);
        action_sort_name = menu.findItem(R.id.action_sort_name);
        action_sort_size = menu.findItem(R.id.action_sort_size);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId() == R.id.action_import_pano) {
            ChooseItemDialogFragment chooseItemDialogFragment = new ChooseItemDialogFragment(
                    getString(R.string.action_import_pano),
                    new String[] {
                            getString(R.string.text_import_files),
                            getString(R.string.text_choose_system_gallery_to_import),
                    }
            );
            chooseItemDialogFragment.setOnChooseItemListener((choosed, index, str) -> {
                if(index == 0) {
                    EasyPhotos.createAlbum(this, false, GlideEngine.getInstance())
                            .setPuzzleMenu(false)
                            .setCount(500)
                            .setVideo(true)
                            .setFileProviderAuthority(Constants.FILE_PROVIDER_NAME)
                            .start(Codes.REQUEST_CODE_OPEN_IMAGE);
                }
                else if (index == 1) {
                    ChooseSystemGalleryDialogFragment chooseSystemGalleryDialog = new ChooseSystemGalleryDialogFragment();
                    chooseSystemGalleryDialog.setOnChooseGalleryListener((album) -> importFiles(album.photos));
                    chooseSystemGalleryDialog.show(getSupportFragmentManager(), "ChooseSystemGallery");
                }
            });
            chooseItemDialogFragment.show(getSupportFragmentManager(), "AddChooseItem");
        }
        else if(item.getItemId() == R.id.action_sort_name) {
            galleryGridList.sort(MainList.SORT_NAME);
            updateSortMenuActive();
        }
        else if(item.getItemId() == R.id.action_sort_date) {
            galleryGridList.sort(MainList.SORT_DATE);
            updateSortMenuActive();
        }
        else if(item.getItemId() == R.id.action_sort_size) {
            galleryGridList.sort(MainList.SORT_SIZE);
            updateSortMenuActive();
        }
        else if(item.getItemId() == R.id.action_rename) {
            onRenameGalleryClick();
        }
        else if(item.getItemId() == R.id.action_show_in_main) {
            listDataService.setGalleryListItemShowInMain(currentGalleryId, true);
            listDataService.setDataDirty(true);
            ToastUtils.show(getString(R.string.text_select_items_showed_in_main));
        }
        else if(item.getItemId() == R.id.action_hide_in_main) {
            listDataService.setGalleryListItemShowInMain(currentGalleryId, false);
            listDataService.setDataDirty(true);
            ToastUtils.show(getString(R.string.text_select_items_hidden_in_main));
        }
        else if(item.getItemId() == R.id.action_list_mode) {
            galleryGridList.setListIsGrid(!galleryGridList.isGrid());
            updateListGridMode();
        }
        else if(item.getItemId() == R.id.action_show_hide_date) {
            galleryGridList.setGroupByDate(!galleryGridList.isGroupByDate());
            updateShowHideDateMenuActive();
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

        RecyclerViewEmptySupport recyclerView = findViewById(R.id.grid_gallery);
        recyclerView.setAdapter(galleryGridList.getMainListAdapter());
        recyclerView.setEmptyView(findViewById(R.id.layout_empty));

        View layout_selection = findViewById(R.id.layout_selection);
        layout_selection.setVisibility(View.GONE);
        Button button_selection_open_with = layout_selection.findViewById(R.id.button_selection_open_with);
        Button button_selection_add_to = layout_selection.findViewById(R.id.button_selection_add_to);
        Button button_selection_delete = layout_selection.findViewById(R.id.button_selection_delete);
        Button button_selection_more = layout_selection.findViewById(R.id.button_selection_more);

        button_selection_open_with.setOnClickListener(v -> onOpenImageWithClick());
        button_selection_delete.setOnClickListener(v -> onDeleteImageClick());
        button_selection_add_to.setOnClickListener(v -> onAddImageToClick());
        button_selection_more.setOnClickListener(v -> onImageMoreClick());

        galleryGridList.init(recyclerView);
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

            button_selection_open_with.setEnabled(count == 1);
            button_selection_more.setEnabled(count > 0);
            button_selection_delete.setEnabled(count > 0);
            button_selection_add_to.setEnabled(count > 0);

            onTitleSelectionChanged(
                        galleryGridList.isListCheckMode(), count, count == galleryGridList.getCheckableItemsCount());
        });
        galleryGridList.setListOnItemClickListener((parent, v, position, id) -> {
            MainListItem item = galleryGridList.getMainListAdapter().getItem(position);
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

        galleryGridList.clear();

        List<ImageItem> list = listDataService.collectGalleryItems(currentGalleryId);
        for (ImageItem item : list)
            galleryGridList.addImageToItem(item);
        galleryGridList.sort();

        titleBar.setTitle(currentGalleryName);
    }
    private void importFiles(ArrayList<Photo> photos) {
        int addCount = 0;
        for (Photo photo : photos)
            if(listDataService.findImageItem(photo.path) == null) {
                galleryGridList.addImageToItem(listDataService.addImageItem(photo.path, currentGalleryId, false));
                addCount++;
            }
        galleryGridList.sort();
        galleryGridList.notifyChange();

        ToastUtils.show(String.format(getString(R.string.text_import_success_count), addCount));
    }

    //====================================================
    //列表事件
    //====================================================

    private void onOpenImageClick(String path) {
        Intent intent = new Intent(this, PanoActivity.class);
        intent.putExtra("openFilePath", path);
        intent.putExtra("openFileArgPath", path);
        intent.putCharSequenceArrayListExtra("fileList", galleryGridList.getMainListPathItems());
        startActivityForResult(intent, Codes.REQUEST_CODE_PANO);
    }
    private void onShareImageClick() {
        final List<MainListItem> sel = galleryGridList.getSelectedItems();
        if(sel.size() == 1)
            ShareUtils.shareFile(this, sel.get(0).getFilePath());
        else if(sel.size() > 1) {
            List<File> list = new ArrayList<>();
            for(MainListItem item : sel)
                list.add(new File(item.getFilePath()));
            ShareUtils.shareStreamMultiple(this, list);
        }

        galleryGridList.setListCheckMode(false);
    }
    private void onDeleteImageClick() {
        final List<MainListItem> sel = galleryGridList.getSelectedItems();
        if(sel.size() > 0){
            new CommonDialog(this)
                    .setTitle(resources.getString(R.string.text_sure_delete_pano))
                    .setMessage(String.format(resources.getString(R.string.text_you_can_remove_choose_images_from_gallery), sel.size()))
                    .setPositive(R.string.action_sure_delete)
                    .setNegative(R.string.action_cancel)
                    .setCheckBoxText(R.string.text_also_delete_from_main_list)
                    .setOnResult((result, dialog) -> {
                        if(result == CommonDialog.BUTTON_POSITIVE) {
                            //delete files
                            if(dialog.isCheckBoxChecked()) {
                                //delete in listDataService
                                for(MainListItem item : sel)
                                    listDataService.removeImageItem(item.getImageItem());
                            }
                            for(MainListItem item : sel)
                                item.getImageItem().belongGalleries.remove((Object)currentGalleryId);
                            //del
                            galleryGridList.deleteItems(sel);
                            galleryGridList.setListCheckMode(false);
                            //标记
                            currentGalleryChanged = true;
                            return true;
                        } else return result == CommonDialog.BUTTON_NEGATIVE;
                    })
                    .show();
        }
    }
    private void onOpenImageWithClick() {
        final List<MainListItem> sel = galleryGridList.getSelectedItems();
        if(sel.size() == 1)
            FileUtils.openFileWithApp(this, sel.get(0).getFilePath());
    }
    private void onAddImageToClick() {
        final List<MainListItem> sel = galleryGridList.getSelectedItems();
        if(sel.size() > 0) {
            ChooseGalleryDialogFragment chooseGalleryDialogFragment = new ChooseGalleryDialogFragment();
            chooseGalleryDialogFragment.setHandler(handler);
            chooseGalleryDialogFragment.setOnChooseGalleryListener((galleryId) -> {
                ChooseItemDialogFragment chooseItemDialogFragment = new ChooseItemDialogFragment(
                        getString(R.string.text_choose_move_method),
                        new String[] {
                                getString(R.string.text_method_move),
                                getString(R.string.text_method_copy)
                        });
                chooseItemDialogFragment.setOnChooseItemListener((choose, i, c) -> {
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
                chooseItemDialogFragment.show(getSupportFragmentManager(), "ChooseItem");
            });
            chooseGalleryDialogFragment.show(getSupportFragmentManager(), "ChooseGallery");
        }
    }
    private void onRenameGalleryClick() {
        new CommonDialog(this)
                .setEditTextHint(getString(R.string.text_enter_gallery_name))
                .setEditTextValue(currentGallery.name)
                .setOnEditTextChangedListener((newText, dialog) -> dialog.setPositiveEnable(newText.length() > 0))
                .setTitle(R.string.text_rename_gallery)
                .setNegative(R.string.action_cancel)
                .setPositive(R.string.action_ok)
                .setOnResult((button, dialog) -> {
                    if(button == CommonDialog.BUTTON_POSITIVE) {
                        String newName = dialog.getEditTextValue().toString();
                        titleBar.setTitle(newName);
                        currentGalleryChanged = true;
                        listDataService.renameGalleryItem(currentGallery.id, newName);
                        return true;
                    } else return button == CommonDialog.BUTTON_NEGATIVE;
                })
                .show();
    }
    private void onImageMoreClick() {
        final List<MainListItem> sel = galleryGridList.getSelectedItems();
        if(sel.size() > 0) {
            new ChooseItemDialogFragment(null, new String[] {
                    getString(R.string.text_show_in_main),
                    getString(R.string.text_do_not_show_in_main),
                    getString(R.string.action_share),
            })
                    .setOnChooseItemListener((choosed, index, item) -> {
                        if(choosed) {
                            if(index == 0 || index == 1) {
                                for(MainListItem listItem : sel)
                                    listItem.getImageItem().showInMain = index == 0;
                                listDataService.setDataDirty(true);
                                galleryGridList.setListCheckMode(false);

                                ToastUtils.show(getString(index == 0 ?
                                        R.string.text_select_items_showed_in_main :
                                        R.string.text_select_items_hidden_in_main));
                            } else if(index == 2) {
                                onShareImageClick();
                            }
                        }
                    })
                    .show(getSupportFragmentManager(), "ChooseImageMore");
        }
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

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        onUpdateScreenOrientation(newConfig);
        super.onConfigurationChanged(newConfig);
    }

    private void onUpdateScreenOrientation(Configuration newConfig) {
        if(newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            galleryGridList.setListIsHorizontal(false);
        } else if(newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            galleryGridList.setListIsHorizontal(true);
        }
    }


    private SharedPreferences sharedPreferences = null;

    private void loadSettings() {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        galleryGridList.setSortReverse(sharedPreferences.getBoolean("gallery_" + currentGalleryId + "_sort_reverse", false));
        galleryGridList.setSortType(sharedPreferences.getInt("gallery_" + currentGalleryId + "_sort_type", MainList.SORT_DATE));
        galleryGridList.setGroupByDate(sharedPreferences.getBoolean("gallery_" + currentGalleryId + "_group_by_date", true));
        galleryGridList.setListIsGrid(sharedPreferences.getBoolean("gallery_" + currentGalleryId + "_is_grid", true));
        galleryGridList.sort();

        updateSortMenuActive();
    }
    private void saveSettings() {
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putInt("gallery_" + currentGalleryId + "_sort_type", galleryGridList.getSortType());
        editor.putBoolean("gallery_" + currentGalleryId + "_sort_reverse", galleryGridList.isSortReverse());
        editor.putBoolean("gallery_" + currentGalleryId + "_is_grid", galleryGridList.isGrid());
        editor.putInt("gallery_" + currentGalleryId + "_sort_type", galleryGridList.getSortType());

        editor.apply();
    }

    //====================================================
    //handler
    //====================================================

    private static class SubHandler extends Handler {
        private final WeakReference<GalleryActivity> mTarget;

        SubHandler(GalleryActivity target) {
            super(Looper.myLooper());
            mTarget = new WeakReference<>(target);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MainMessages.MSG_TEST_LIST:
                    break;
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
        if (requestCode == Codes.REQUEST_CODE_OPEN_IMAGE && data != null) {
            //获取选择器返回的数据
            ArrayList<Photo> resultPhotos = data.getParcelableArrayListExtra(EasyPhotos.RESULT_PHOTOS);
            if(resultPhotos != null) importFiles(resultPhotos);
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


    private void updateListGridMode() {
        if(galleryGridList.isGrid()) {
            action_list_mode.setTitle(getString(R.string.text_list_mode));
            action_show_hide_date.setVisible(true);
        }
        else {
            action_list_mode.setTitle(getString(R.string.text_grid_mode));
            action_show_hide_date.setVisible(false);
        }
    }
    private void updateShowHideDateMenuActive() {
        action_show_hide_date.setTitle(galleryGridList.isGroupByDate() ?
                R.string.text_hide_date : R.string.text_show_date);
    }
    private void updateSortMenuActive() {

        int sort = galleryGridList.getSortType();
        int icon = galleryGridList.isSortReverse() ? R.drawable.ic_sort_up : R.drawable.ic_sort_down;

        action_sort_date.setIcon(R.drawable.ic_sort_none);
        action_sort_name.setIcon(R.drawable.ic_sort_none);
        action_sort_size.setIcon(R.drawable.ic_sort_none);

        switch (sort) {
            case MainList.SORT_DATE:
                action_sort_date.setIcon(icon);
                break;
            case MainList.SORT_NAME:
                action_sort_name.setIcon(icon);
                break;
            case MainList.SORT_SIZE:
                action_sort_size.setIcon(icon);
                break;
        }
    }
}