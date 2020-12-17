package com.imengyu.vr720.fragment;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.donkingliang.imageselector.utils.ImageSelector;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.hjq.toast.ToastUtils;
import com.imengyu.vr720.MainActivity;
import com.imengyu.vr720.PanoActivity;
import com.imengyu.vr720.R;
import com.imengyu.vr720.VR720Application;
import com.imengyu.vr720.config.Codes;
import com.imengyu.vr720.config.MainMessages;
import com.imengyu.vr720.dialog.AppDialogs;
import com.imengyu.vr720.dialog.CommonDialog;
import com.imengyu.vr720.list.MainList;
import com.imengyu.vr720.model.ImageItem;
import com.imengyu.vr720.model.list.MainListItem;
import com.imengyu.vr720.model.TitleSelectionChangedCallback;
import com.imengyu.vr720.service.ListDataService;
import com.imengyu.vr720.utils.FileUtils;
import com.imengyu.vr720.widget.MyTitleBar;
import com.scwang.smart.refresh.header.ClassicsHeader;
import com.scwang.smart.refresh.layout.api.RefreshLayout;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class HomeFragment extends Fragment implements IMainFragment {

    public HomeFragment() {
        MainActivity mainActivity = (MainActivity)getActivity();
        if(mainActivity != null) {
            listDataService = mainActivity.getListDataService();
            handler = mainActivity.getHandler();
            titleBar = mainActivity.getToolbar();
        } else {
            listDataService = null;
            handler = null;
            titleBar = null;
        }
    }
    public HomeFragment(Handler handler, MyTitleBar titleBar, ListDataService listDataService) {
        this.handler = handler;
        this.titleBar = titleBar;
        this.listDataService = listDataService;
    }

    private final ListDataService listDataService;
    private final Handler handler;
    private final MyTitleBar titleBar;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.layout_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        resources = getResources();

        initView(view);
        initMenu();

        loadSettings();
        loadList();

        onUpdateScreenOrientation(getResources().getConfiguration());
    }

    @Override
    public void onPause() {
        saveSettings();
        super.onPause();
    }

    private Resources resources;

    private MainList mainList = null;
    private GridView grid_main;
    private RefreshLayout refreshLayout;

    private void initView(View view) {
        final FloatingActionButton fab = view.findViewById(R.id.fab);
        fab.setOnClickListener(v -> onAddImageClick());

        final LinearLayout footerSelection = view.findViewById(R.id.footer_select_main);
        footerSelection.setVisibility(View.GONE);
        grid_main = view.findViewById(R.id.grid_main);

        final View button_mainsel_openwith = view.findViewById(R.id.button_mainsel_openwith);
        final View button_mainsel_delete = view.findViewById(R.id.button_mainsel_delete);
        final View button_mainsel_share = view.findViewById(R.id.button_mainsel_share);
        final View button_mainsel_add_to = view.findViewById(R.id.button_mainsel_add_to);

        button_mainsel_openwith.setOnClickListener(v -> onOpenImageWithClick());
        button_mainsel_delete.setOnClickListener(v -> onDeleteImageClick());
        button_mainsel_share.setOnClickListener(v -> onShareImageClick());
        button_mainsel_add_to.setOnClickListener(v -> onAddImageToClick());

        mainList = new MainList(getContext(), ((VR720Application)getActivity().getApplication()).getListImageCacheService());
        mainList.init(handler, grid_main);
        mainList.setListCheckableChangedListener(checkable -> {
            if (checkable) {
                fab.hide();
                AnimationSet animationSet = (AnimationSet) AnimationUtils.loadAnimation(getContext(), R.anim.bottom_up);
                footerSelection.startAnimation(animationSet);
                footerSelection.setVisibility(View.VISIBLE);

                if(titleSelectionChangedCallback != null)
                    titleSelectionChangedCallback.onTitleSelectionChangedCallback(
                            true, 0, false);
            } else {
                fab.show();
                AnimationSet animationSet = (AnimationSet) AnimationUtils.loadAnimation(getContext(), R.anim.bottom_down);
                footerSelection.startAnimation(animationSet);
                footerSelection.setVisibility(View.GONE);

                if(titleSelectionChangedCallback != null)
                    titleSelectionChangedCallback.onTitleSelectionChangedCallback(
                            false, 0, false);
            }
        });
        mainList.setListCheckItemCountChangedListener((count) -> {

            button_mainsel_openwith.setEnabled(count == 1);
            button_mainsel_share.setEnabled(count == 1);
            button_mainsel_delete.setEnabled(count > 0);
            button_mainsel_add_to.setEnabled(count > 0);

            if(titleSelectionChangedCallback != null)
                titleSelectionChangedCallback.onTitleSelectionChangedCallback(
                        mainList.isListCheckMode(), count, count == mainList.getMainListItemCount());
        });
        mainList.setListOnItemClickListener((parent, v, position, id) -> {
            MainListItem item = mainList.getMainListAdapter().getItem(position);
            if(item != null) {
                onOpenImageClick(item.getFilePath());
            }
        });

        grid_main.setEmptyView(view.findViewById(R.id.empty_main));

        refreshLayout = view.findViewById(R.id.refreshLayout);
        refreshLayout.setRefreshHeader(new ClassicsHeader(getContext()));
        refreshLayout.setOnRefreshListener(refreshlayout -> loadList());
    }

    private void onUpdateScreenOrientation(Configuration newConfig) {
        if(newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            grid_main.setNumColumns(1);
        } else if(newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            grid_main.setNumColumns(2);
        }
    }

    //====================================================
    //菜单
    //====================================================

    private PopupMenu mainMenu;

    private void initMenu() {
        mainMenu = new PopupMenu(getActivity(), titleBar.getRightButton(), Gravity.TOP);
        mainMenu.getMenuInflater().inflate(R.menu.menu_main, mainMenu.getMenu());
        mainMenu.setOnMenuItemClickListener(this::onOptionsItemSelected);
    }

    @Override
    public void showMore() { mainMenu.show(); }

    //====================================================
    //公共方法
    //====================================================

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        onUpdateScreenOrientation(newConfig);
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Codes.REQUEST_CODE_OPENIMAGE && data != null) {
            //获取选择器返回的数据
            ArrayList<String> images = data.getStringArrayListExtra(ImageSelector.SELECT_RESULT);
            if(images!=null) {
                for (String path : images)
                    mainList.addImageToItem(listDataService.addImageItem(path), false);
                mainList.sort();
                mainList.notifyChange();
            }
        }
        else if (requestCode == Codes.REQUEST_CODE_PANO && data != null) {
            if(data.getBooleanExtra("isDeleteFile", false)) {
                MainListItem item = mainList.findImageItem(data.getStringExtra("filePath"));
                if(item != null) {
                    listDataService.removeImageItem(item.getImageItem());
                    mainList.deleteItem(item);
                    mainList.notifyChange();
                }
            }
        }
    }

    private TitleSelectionChangedCallback titleSelectionChangedCallback = null;
    @Override
    public void setTitleSelectionChangedCallback(TitleSelectionChangedCallback callback) {
        titleSelectionChangedCallback = callback;
    }
    @Override
    public void setTitleSelectionCheckAllSwitch() {
        if(mainList.getSelectedItemCount() >= mainList.getMainListItemCount())
            mainList.clearSelectedItems();
        else
            mainList.selectAllItems();
    }
    @Override
    public void setTitleSelectionQuit() { mainList.setListCheckMode(false); }

    @Override
    public boolean onBackPressed() {
        if(mainList.isListCheckMode()) {
            mainList.setListCheckMode(false);
            return true;
        }
        return false;
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case MainMessages.MSG_REFRESH_LIST:
                mainList.refresh();
                break;
            case MainMessages.MSG_LIST_LOAD_FINISH:
                refreshLayout.finishRefresh();
                break;
            case MainMessages.MSG_ADD_IMAGE:
                onAddImageClick();
                break;
        }
    }

    //====================================================
    //列表操作
    //====================================================

    private void loadList() {
        mainList.clear();
        ArrayList<ImageItem> items = listDataService.getImageList();
        for(ImageItem imageItem : items)
            mainList.addImageToItem(imageItem, false);
        mainList.notifyChange();

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                handler.sendEmptyMessage(MainMessages.MSG_LIST_LOAD_FINISH);
            }
        }, 800);
    }

    //====================================================
    //设置保存与读取
    //====================================================

    private void loadSettings() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        mainList.setMainSortReverse(sharedPreferences.getBoolean("main_list_sort_reverse", false));
        mainList.setMainSortType(sharedPreferences.getInt("main_list_sort_type", MainList.MAIN_SORT_DATE));
        mainList.sort();
    }
    private void saveSettings() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putInt("main_list_sort_type", mainList.getMainSortType());
        editor.putBoolean("main_list_sort_reverse", mainList.isMainSortReverse());

        editor.apply();
    }

    //====================================================
    //按钮事件
    //====================================================

    private void onAddImageClick(){
        //不限数量的多选
        ImageSelector.builder()
                .useCamera(false) // 设置是否使用拍照
                .setSingle(false)  //设置是否单选
                .setMaxSelectCount(0) // 图片的最大选择数量，小于等于0时，不限数量。
                .start(this, Codes.REQUEST_CODE_OPENIMAGE); // 打开相册
    }
    private void onClearClick() {
        new CommonDialog(getContext())
                .setTitle(resources.getString(R.string.text_would_you_want_clear_list))
                .setMessage(resources.getText(R.string.text_all_list_will_be_clear))
                .setNegative(resources.getText(R.string.action_sure_clear))
                .setPositive(resources.getText(R.string.action_cancel))
                .setOnClickBottomListener(new CommonDialog.OnClickBottomListener() {
                    @Override
                    public void onNegativeClick(CommonDialog dialog) {
                        //clear in listDataService
                        listDataService.clearImageItems();
                        //clear
                        mainList.clear();
                        dialog.dismiss();
                    }
                    @Override
                    public void onPositiveClick(CommonDialog dialog) { dialog.dismiss(); }
                })
                .show();
    }
    private void onSortClick(int sortType){ mainList.sort(sortType); }
    private void onOpenImageClick(String filePath) {
        Intent intent = new Intent(getActivity(), PanoActivity.class);
        intent.putExtra("filePath", filePath);
        intent.putCharSequenceArrayListExtra("fileList", mainList.getMainListPathItems());
        startActivityForResult(intent, Codes.REQUEST_CODE_PANO);
    }
    private void onShareImageClick() {
        final List<MainListItem> sel = mainList.getSelectedItems();
        if(sel.size() > 0)
            FileUtils.shareFile(getContext(), sel.get(0).getFilePath());

        mainList.setListCheckMode(false);
    }
    private void onDeleteImageClick() {
        final List<MainListItem> sel = mainList.getSelectedItems();
        if(sel.size() > 0) {
            new CommonDialog(getContext())
                    .setTitle(resources.getString(R.string.text_sure_delete_pano))
                    .setMessage(String.format(resources.getString(R.string.text_you_can_remove_choosed_images), sel.size()))
                    .setNegative(resources.getText(R.string.action_sure_delete))
                    .setPositive(resources.getText(R.string.action_cancel))
                    .setCheckText(resources.getText(R.string.text_also_delete_files))
                    .setCanCancelable(true)
                    .setOnClickBottomListener(new CommonDialog.OnClickBottomListener() {
                        @Override
                        public void onNegativeClick(CommonDialog dialog) {
                            //delete files
                            if(dialog.isCheckBoxChecked()) {
                                int deleteFileCount = 0;
                                for (MainListItem item : sel) {
                                    File file = new File(item.getFilePath());
                                    if(file.exists() && file.canWrite() && file.delete())
                                        deleteFileCount++;
                                }
                                ToastUtils.show(String.format(getString(R.string.text_file_delete_count), deleteFileCount));
                            }
                            //delete in listDataService
                            for(MainListItem item : sel)
                                listDataService.removeImageItem(item.getImageItem());
                            //del
                            mainList.deleteItems(sel);
                            mainList.setListCheckMode(false);
                            dialog.dismiss();
                        }
                        @Override
                        public void onPositiveClick(CommonDialog dialog) { dialog.dismiss(); }
                    })
                    .show();
        }
    }
    private void onOpenImageWithClick() {
        final List<MainListItem> sel = mainList.getSelectedItems();
        if(sel.size() == 1)
            FileUtils.openFileWithApp(getActivity(), sel.get(0).getFilePath());

        mainList.setListCheckMode(false);
    }
    private void onAddImageToClick() {
        final List<MainListItem> sel = mainList.getSelectedItems();
        if(sel.size() > 0) {
            AppDialogs.showChooseGalleryDialog(handler, getActivity(), listDataService, galleryId -> {

                //添加到对应相册
                ImageItem imageItem;
                for(MainListItem item : sel) {
                    imageItem = item.getImageItem();
                    if(!imageItem.isInBelongGalleries(galleryId))
                        imageItem.belongGalleries.add(galleryId);
                }

                //发送消息到相册界面进行相册缩略图刷新
                Message message = new Message();
                message.what = MainMessages.MSG_REFRESH_GALLERY_ITEM;
                message.obj = galleryId;
                handler.sendMessage(message);

                ToastUtils.show(R.string.text_add_success);

                //关闭选择模式
                mainList.setListCheckMode(false);
            });
        }
    }

    //====================================================
    //其他事件
    //====================================================

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.action_clear) {
            onClearClick();
            return true;
        }
        if(id == R.id.action_sort_date) {
            onSortClick(MainList.MAIN_SORT_DATE);
            return true;
        }
        if(id == R.id.action_sort_name) {
            onSortClick(MainList.MAIN_SORT_NAME);
            return true;
        }
        if(id == R.id.action_sort_size) {
            onSortClick(MainList.MAIN_SORT_SIZE);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}