package com.imengyu.vr720.fragment;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
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
import androidx.preference.PreferenceManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.hjq.toast.ToastUtils;
import com.imengyu.vr720.activity.GalleryActivity;
import com.imengyu.vr720.activity.MainActivity;
import com.imengyu.vr720.R;
import com.imengyu.vr720.VR720Application;
import com.imengyu.vr720.config.Codes;
import com.imengyu.vr720.config.MainMessages;
import com.imengyu.vr720.dialog.CommonDialog;
import com.imengyu.vr720.dialog.fragment.ChooseItemDialogFragment;
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

    public GalleryFragment() {}

    private ListDataService listDataService;
    private Handler handler;
    private MyTitleBar titleBar;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.layout_gallery, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        MainActivity mainActivity = (MainActivity)getActivity();
        if(mainActivity != null) {
            listDataService = mainActivity.getListDataService();
            handler = mainActivity.getHandler();
            titleBar = mainActivity.getToolbar();
        }

        initMenu();
        initView(view);
        loadSettings();
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
        final View button_selection_more = view.findViewById(R.id.button_selection_more);

        button_rename.setOnClickListener(v -> onRenameGalleryClick());
        button_delete.setOnClickListener(v -> onDeleteGalleryClick());
        button_selection_more.setOnClickListener(v -> onGalleryMoreClick());

        galleryList = new GalleryList(getActivity(),
                requireContext(), ((VR720Application)requireActivity().getApplication()).getListImageCacheService());
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
        refreshLayout.setRefreshHeader(new ClassicsHeader(requireContext()).setEnableLastTime(false));
        refreshLayout.setOnRefreshListener(refreshlayout -> loadList());
    }

    //====================================================
    //菜单
    //====================================================
    private PopupMenu mainMenu;
    private MenuItem action_sort_date;
    private MenuItem action_sort_name;

    private void initMenu() {
        mainMenu = new PopupMenu(getActivity(), titleBar == null ? null : titleBar.getRightButton(), Gravity.TOP);
        Menu menu = mainMenu.getMenu();
        mainMenu.getMenuInflater().inflate(R.menu.menu_gallerys, menu);
        mainMenu.setOnMenuItemClickListener(this::onOptionsItemSelected);
        action_sort_date = menu.findItem(R.id.action_sort_date);
        action_sort_name = menu.findItem(R.id.action_sort_name);
    }

    @Override
    public void showMore() { mainMenu.show(); }

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

    SharedPreferences sharedPreferences;

    private void loadSettings() {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
        galleryList.setSortReverse(sharedPreferences.getBoolean("gallery_list_sort_reverse", false));
        galleryList.setSortType(sharedPreferences.getInt("gallery_list_sort_type", GalleryList.GALLERY_SORT_NAME));
        galleryList.sort();

        updateSortMenuActive();
    }
    private void saveSettings() {
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putInt("gallery_list_sort_type", galleryList.getSortType());
        editor.putBoolean("gallery_list_sort_reverse", galleryList.isSortReverse());

        editor.apply();
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
            if (data != null) {
                if (data.getBooleanExtra("galleryChanged", false)) {
                    int id = data.getIntExtra("galleryId", 0);
                    if (id > 0) {
                        GalleryListItem item = galleryList.findItem(id);
                        if (item != null)
                            onGalleryRefresh(item);
                        handler.sendEmptyMessage(MainMessages.MSG_REFRESH_GALLERY_LIST);
                    }
                }
                if (data.getBooleanExtra("galleryItemVisibleChanged", false)) {
                    handler.sendEmptyMessage(MainMessages.MSG_RELOAD_MAIN_LIST);
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
                if(id == -6) {
                    loadList();
                } else {
                    GalleryListItem item = galleryList.findItem(id);
                    if (item != null) {
                        onGalleryRefresh(item);
                        galleryList.refresh();
                    }
                }
                break;
            }
            case MainMessages.MSG_FORCE_LOAD_LIST:
                loadList();
                break;
        }
    }

    @Override
    public void onPause() {
        saveSettings();
        super.onPause();
    }

    //====================================================
    //按钮事件
    //====================================================

    private void onAddGalleryClick() {
        new CommonDialog(requireActivity())
                .setEditTextHint(R.string.text_enter_gallery_name)
                .setTitle(R.string.action_new_gallery)
                .setPositiveEnable(false)
                .setCancelable(true)
                .setOnEditTextChangedListener((newText, dialog) -> dialog.setPositiveEnable(newText.length() > 0))
                .setPositive(R.string.action_ok)
                .setNegative(R.string.action_cancel)
                .setOnResult((b, dialog) -> {
                    if(b == CommonDialog.BUTTON_POSITIVE) {
                        GalleryListItem listItem = new GalleryListItem();
                        listItem.setId(listDataService.getGalleryListMinId());
                        listItem.setName(dialog.getEditText().getText().toString());

                        galleryList.addItem(listItem, true);
                        listDataService.addGalleryItem(listItem.toGalleryItem());
                        return true;
                    } else return b == CommonDialog.BUTTON_NEGATIVE;
                })
                .show();
    }
    private void onDeleteGalleryClick() {
        final List<GalleryListItem> sel = galleryList.getSelectedItems();
        if(sel.size() > 0) {
            new CommonDialog(requireActivity())
                    .setTitle(R.string.text_sure_delete_gallery)
                    .setMessage(R.string.text_gallery_list_will_be_clear)
                    .setPositive(R.string.action_sure_delete)
                    .setNegative(R.string.action_cancel)
                    .setCancelable(true)
                    .setOnResult((b, dialog) -> {
                        if(b == CommonDialog.BUTTON_POSITIVE) {
                            //delete in listDataService
                            for(GalleryListItem item : sel)
                                listDataService.removeGalleryItem(item.getId());
                            //del
                            galleryList.deleteItems(sel);
                            return true;
                        } else return b == CommonDialog.BUTTON_NEGATIVE;
                    })
                    .show();
        }
    }
    private void onRenameGalleryClick() {

        final List<GalleryListItem> sel = galleryList.getSelectedItems();
        if(sel.size() > 0) {
            final GalleryListItem item = sel.get(0);
            new CommonDialog(requireActivity())
                    .setEditTextHint(R.string.text_enter_gallery_name)
                    .setEditTextValue(item.getName())
                    .setTitle(R.string.text_rename_gallery)
                    .setCancelable(true)
                    .setOnEditTextChangedListener((newText, dialog) -> dialog.setPositiveEnable(newText.length() > 0))
                    .setPositive(R.string.action_ok)
                    .setNegative(R.string.action_cancel)
                    .setOnResult((b, dialog) -> {
                        if(b == CommonDialog.BUTTON_POSITIVE) {
                            String newName = dialog.getEditTextValue().toString();

                            item.setName(newName);
                            galleryList.notifyChange();
                            listDataService.renameGalleryItem(item.id, newName);
                            return true;
                        } else return b == CommonDialog.BUTTON_NEGATIVE;
                    })
                    .show();
        }
    }
    private void onGalleryMoreClick() {
        final List<GalleryListItem> sel = galleryList.getSelectedItems();
        if(sel.size() > 0) {
            new ChooseItemDialogFragment(null, new String[] {
                        getString(R.string.text_show_in_main),
                        getString(R.string.text_do_not_show_in_main),
                    })
                    .setOnChooseItemListener((choosed, index, item) -> {
                        if(choosed) {
                            if(index == 0 || index == 1) {
                                for(GalleryListItem galleryListItem : sel) {
                                    listDataService.setGalleryListItemShowInMain(
                                            galleryListItem.getId(), index == 0);
                                }
                                ToastUtils.show(getString(index == 0 ?
                                        R.string.text_select_items_showed_in_main :
                                        R.string.text_select_items_hidden_in_main));

                                listDataService.setDataDirty(true);
                                handler.sendEmptyMessageDelayed(MainMessages.MSG_FORCE_LOAD_LIST, 1200);
                                galleryList.setListCheckMode(false);
                            }
                        }
                    })
                    .show(getParentFragmentManager(), "ChooseGalleryMore");
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
        }
        else if(item.getItemId() == R.id.action_sort_date) {
            galleryList.sort(GalleryList.GALLERY_SORT_DATE);
            updateSortMenuActive();
        }
        else if(item.getItemId() == R.id.action_sort_name) {
            galleryList.sort(GalleryList.GALLERY_SORT_NAME);
            updateSortMenuActive();
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateSortMenuActive() {

        int sort = galleryList.getSortType();
        int icon = galleryList.isSortReverse() ? R.drawable.ic_sort_up : R.drawable.ic_sort_down;

        action_sort_date.setIcon(R.drawable.ic_sort_none);
        action_sort_name.setIcon(R.drawable.ic_sort_none);

        switch (sort) {
            case GalleryList.GALLERY_SORT_DATE:
                action_sort_date.setIcon(icon);
                break;
            case GalleryList.GALLERY_SORT_NAME:
                action_sort_name.setIcon(icon);
                break;
        }
    }
}