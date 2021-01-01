package com.imengyu.vr720.fragment;

import android.content.Intent;
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
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.imengyu.vr720.GalleryActivity;
import com.imengyu.vr720.MainActivity;
import com.imengyu.vr720.R;
import com.imengyu.vr720.VR720Application;
import com.imengyu.vr720.config.Codes;
import com.imengyu.vr720.config.MainMessages;
import com.imengyu.vr720.dialog.CommonDialog;
import com.imengyu.vr720.list.GalleryList;
import com.imengyu.vr720.model.GalleryItem;
import com.imengyu.vr720.model.TitleSelectionChangedCallback;
import com.imengyu.vr720.model.list.GalleryListItem;
import com.imengyu.vr720.service.ListDataService;
import com.imengyu.vr720.widget.MyTitleBar;
import com.scwang.smart.refresh.header.ClassicsHeader;
import com.scwang.smart.refresh.layout.api.RefreshLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class GalleryFragment extends Fragment implements IMainFragment {

    public GalleryFragment() {
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
    public GalleryFragment(Handler handler, MyTitleBar titleBar, ListDataService listDataService) {
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
        return inflater.inflate(R.layout.layout_gallery, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        initMenu();
        initView(view);
        loadList();
    }

    private GalleryList galleryList = null;
    private RefreshLayout refreshLayout;

    private void initView(View view) {
        final FloatingActionButton fab = view.findViewById(R.id.fab);
        fab.setOnClickListener(v -> onAddGalleryClick());

        final LinearLayout footerSelection = view.findViewById(R.id.footer_select);
        footerSelection.setVisibility(View.GONE);
        ListView listView = view.findViewById(R.id.list_gallery);

        final View button_rename = view.findViewById(R.id.button_rename);
        final View button_delete = view.findViewById(R.id.button_delete);

        button_rename.setOnClickListener(v -> onRenameGalleryClick());
        button_delete.setOnClickListener(v -> onDeleteGalleryClick());

        galleryList = new GalleryList(getContext(), ((VR720Application)getActivity().getApplication()).getListImageCacheService());
        galleryList.init(handler, listView);
        galleryList.setListCheckableChangedListener(checkable -> {
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
        galleryList.setListCheckItemCountChangedListener((count) -> {

            button_rename.setEnabled(count == 1);
            button_delete.setEnabled(count > 0);

            if(titleSelectionChangedCallback != null)
                titleSelectionChangedCallback.onTitleSelectionChangedCallback(
                        galleryList.isListCheckMode(), count, count == galleryList.getCheckableItemsCount());
        });
        galleryList.setListOnItemClickListener((parent, v, position, id) -> {
            GalleryListItem item = galleryList.getListAdapter().getItem(position);
            if(item != null) {
                onOpenGalleryClick(item);
            }
        });

        listView.setDividerHeight(0);
        listView.setDivider(null);
        listView.setEmptyView(view.findViewById(R.id.empty_main));

        refreshLayout = view.findViewById(R.id.refreshLayout);
        refreshLayout.setRefreshHeader(new ClassicsHeader(getContext()));
        refreshLayout.setOnRefreshListener(refreshlayout -> loadList());
    }

    //====================================================
    //菜单
    //====================================================
    private PopupMenu mainMenu;

    private void initMenu() {
        mainMenu = new PopupMenu(getActivity(), titleBar.getRightButton(), Gravity.TOP);
        mainMenu.getMenuInflater().inflate(R.menu.menu_gallery_list, mainMenu.getMenu());
        mainMenu.setOnMenuItemClickListener(this::onOptionsItemSelected);
    }

    @Override
    public void showMore() {
        mainMenu.show();
    }

    //====================================================
    //列表事件
    //====================================================

    private void loadList() {
        galleryList.clear();
        ArrayList<GalleryItem> items = listDataService.getGalleryList();
        for(GalleryItem item : items) {
            GalleryListItem listItem = new GalleryListItem(item);
            galleryList.addItem(listItem, false);
            onGalleryRefresh(listItem);
        }

        galleryList.notifyChange();

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                handler.sendEmptyMessage(MainMessages.MSG_LIST_LOAD_FINISH);
            }
        }, 800);
    }

    //====================================================
    //公共方法
    //====================================================

    private TitleSelectionChangedCallback titleSelectionChangedCallback = null;
    @Override
    public void setTitleSelectionChangedCallback(TitleSelectionChangedCallback callback) {
        titleSelectionChangedCallback = callback;
    }
    @Override
    public void setTitleSelectionCheckAllSwitch() {
        if(galleryList.getSelectedItemCount() >= galleryList.getCheckableItemsCount())
            galleryList.clearSelectedItems();
        else galleryList.selectAllItems();
    }
    @Override
    public void setTitleSelectionQuit() { galleryList.setListCheckMode(false); }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(requestCode == Codes.REQUEST_CODE_GALLERY) {
            if(data != null && data.getBooleanExtra("galleryChanged", false)) {
                int id = data.getIntExtra("galleryId", 0);
                if(id > 0) {
                    GalleryListItem item = galleryList.findItem(id);
                    if (item != null)
                        onGalleryRefresh(item);
                    handler.sendEmptyMessage(MainMessages.MSG_REFRESH_GALLERY_LIST);
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onBackPressed() {
        if(galleryList.isListCheckMode()) {
            galleryList.setListCheckMode(false);
            return true;
        }
        return false;
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case MainMessages.MSG_LIST_LOAD_FINISH: refreshLayout.finishRefresh(); break;
            case MainMessages.MSG_REFRESH_GALLERY_LIST: galleryList.refresh(); break;
            case MainMessages.MSG_GALLERY_LIST_ADD_ITEM: {
                int id = (int) msg.obj;
                GalleryItem item = listDataService.getGalleryItem(id);
                if (item != null)
                    galleryList.addItem(new GalleryListItem(item), true);
                break;
            }
            case MainMessages.MSG_REFRESH_GALLERY_ITEM: {
                int id = (int) msg.obj;
                GalleryListItem item = galleryList.findItem(id);
                if(item != null) {
                    onGalleryRefresh(item);
                    galleryList.refresh();
                }
                break;
            }
            case MainMessages.MSG_FORCE_LOAD_LIST:
                loadList();
                break;
        }
    }

    //====================================================
    //按钮事件
    //====================================================

    private void onAddGalleryClick() {
        new CommonDialog(getContext())
                .setEditTextVisible(true)
                .setEditHint(getString(R.string.text_enter_gallery_name))
                .setTitle(getString(R.string.action_new_gallery))
                .setPositiveEnabled(false)
                .setCanCancelable(true)
                .setEditTextOnTextChangedListener((newText, dialog) -> dialog.setPositiveEnabled(newText.length() > 0))
                .setOnClickBottomListener(new CommonDialog.OnClickBottomListener() {
                    @Override
                    public void onPositiveClick(CommonDialog dialog) {
                        dialog.dismiss();

                        GalleryListItem listItem = new GalleryListItem();
                        listItem.setId(listDataService.getGalleryListMinId());
                        listItem.setName(dialog.getEditText().getText().toString());

                        galleryList.addItem(listItem, true);
                        listDataService.addGalleryItem(listItem.toGalleryItem());
                    }
                    @Override
                    public void onNegativeClick(CommonDialog dialog) { dialog.dismiss(); }
                })
                .show();
    }
    private void onDeleteGalleryClick() {
        final List<GalleryListItem> sel = galleryList.getSelectedItems();
        if(sel.size() > 0) {
            new CommonDialog(getContext())
                    .setTitle(getString(R.string.text_sure_delete_gallery))
                    .setMessage(getText(R.string.text_gallery_list_will_be_clear))
                    .setNegative(getText(R.string.action_sure_delete))
                    .setPositive(getText(R.string.action_cancel))
                    .setCanCancelable(true)
                    .setOnClickBottomListener(new CommonDialog.OnClickBottomListener() {
                        @Override
                        public void onNegativeClick(CommonDialog dialog) {
                            //delete in listDataService
                            for(GalleryListItem item : sel)
                                listDataService.removeGalleryItem(item.getId());
                            //del
                            galleryList.deleteItems(sel);
                            dialog.dismiss();
                        }
                        @Override
                        public void onPositiveClick(CommonDialog dialog) { dialog.dismiss(); }
                    })
                    .show();
        }
    }
    private void onRenameGalleryClick() {

        final List<GalleryListItem> sel = galleryList.getSelectedItems();
        if(sel.size() > 0) {
            final GalleryListItem item = sel.get(0);
            new CommonDialog(getContext())
                    .setEditTextVisible(true)
                    .setEditHint(getString(R.string.text_enter_gallery_name))
                    .setEditText(item.getName())
                    .setCanCancelable(true)
                    .setEditTextOnTextChangedListener((newText, dialog) -> dialog.setPositiveEnabled(newText.length() > 0))
                    .setTitle(getString(R.string.text_rename_gallery))
                    .setOnClickBottomListener(new CommonDialog.OnClickBottomListener() {
                        @Override
                        public void onPositiveClick(CommonDialog dialog) {
                            dialog.dismiss();

                            String newName = dialog.getEditText().getText().toString();

                            item.setName(newName);
                            galleryList.notifyChange();
                            listDataService.renameGalleryItem(item.id, newName);
                        }

                        @Override
                        public void onNegativeClick(CommonDialog dialog) { dialog.dismiss(); }
                    })
                    .show();
        }
    }
    private void onOpenGalleryClick(GalleryListItem item) {
        Intent intent = new Intent(getActivity(), GalleryActivity.class);
        intent.putExtra("galleryId", item.getId());
        startActivityForResult(intent, Codes.REQUEST_CODE_GALLERY);
    }
    private void onGalleryRefresh(GalleryListItem item) {
        item.refresh(listDataService);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId() == R.id.action_new_gallery) {
            onAddGalleryClick();
        } else if(item.getItemId() == R.id.action_sort_date) {
            galleryList.sort(GalleryList.GALLERY_SORT_DATE);
        } else if(item.getItemId() == R.id.action_sort_name) {
            galleryList.sort(GalleryList.GALLERY_SORT_NAME);
        } else if(item.getItemId() == R.id.action_sort_custom) {
            galleryList.sort(GalleryList.GALLERY_SORT_CUSTOM);
        }
        return super.onOptionsItemSelected(item);
    }
}