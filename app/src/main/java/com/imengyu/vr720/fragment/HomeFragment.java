package com.imengyu.vr720.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.hjq.toast.ToastUtils;
import com.huantansheng.easyphotos.EasyPhotos;
import com.huantansheng.easyphotos.models.album.entity.Photo;
import com.imengyu.vr720.MainActivity;
import com.imengyu.vr720.PanoActivity;
import com.imengyu.vr720.R;
import com.imengyu.vr720.VR720Application;
import com.imengyu.vr720.config.Codes;
import com.imengyu.vr720.config.Constants;
import com.imengyu.vr720.config.MainMessages;
import com.imengyu.vr720.dialog.AppDialogs;
import com.imengyu.vr720.dialog.CommonDialog;
import com.imengyu.vr720.dialog.LoadingDialog;
import com.imengyu.vr720.list.MainList;
import com.imengyu.vr720.model.ImageItem;
import com.imengyu.vr720.model.TitleSelectionChangedCallback;
import com.imengyu.vr720.model.list.MainListItem;
import com.imengyu.vr720.plugin.GlideEngine;
import com.imengyu.vr720.service.ListDataService;
import com.imengyu.vr720.utils.FileUtils;
import com.imengyu.vr720.utils.KeyBoardUtil;
import com.imengyu.vr720.widget.MyTitleBar;
import com.imengyu.vr720.widget.RecyclerViewEmptySupport;
import com.scwang.smart.refresh.footer.ClassicsFooter;
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
    private Context context;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.layout_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        resources = getResources();
        context = requireContext();

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
    private RefreshLayout refreshLayout;

    private void initView(View view) {
        final FloatingActionButton fab = view.findViewById(R.id.fab);
        edit_search = view.findViewById(R.id.edit_search);
        final LinearLayout footerSelection = view.findViewById(R.id.footer_select_main);

        empty_main = view.findViewById(R.id.empty_main);
        final View empty_search_main = view.findViewById(R.id.empty_search_main);

        footerSelection.setVisibility(View.GONE);
        recycler_main = view.findViewById(R.id.recycler_main);
        recycler_main.setEmptyView(empty_main);

        final View button_mainsel_openwith = view.findViewById(R.id.button_mainsel_openwith);
        final View button_mainsel_delete = view.findViewById(R.id.button_mainsel_delete);
        final View button_mainsel_share = view.findViewById(R.id.button_mainsel_share);
        final View button_mainsel_add_to = view.findViewById(R.id.button_mainsel_add_to);

        button_mainsel_openwith.setOnClickListener(v -> onOpenImageWithClick());
        button_mainsel_delete.setOnClickListener(v -> onDeleteImageClick());
        button_mainsel_share.setOnClickListener(v -> onShareImageClick());
        button_mainsel_add_to.setOnClickListener(v -> onAddImageToClick());

        VR720Application application = (VR720Application) requireActivity().getApplication();

        //List
        mainList = new MainList(context, application.getListImageCacheService(), application.getListDataService());
        mainList.init(handler, recycler_main);
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

        refreshLayout = view.findViewById(R.id.refreshLayout);
        refreshLayout.setRefreshHeader(new ClassicsHeader(requireContext()));
        refreshLayout.setRefreshFooter(new ClassicsFooter(requireContext()));
        refreshLayout.setEnableLoadMore(false);
        refreshLayout.setOnRefreshListener(refreshlayout -> loadList());

        fab.setOnClickListener(v -> onAddImageClick());

        //搜索类型选择

        button_search_all = view.findViewById(R.id.button_search_all);
        button_search_image = view.findViewById(R.id.button_search_image);
        button_search_video = view.findViewById(R.id.button_search_video);

        searchChooseTypeButtonArr = new Button[] {
                button_search_all, button_search_image, button_search_video
        };

        button_search_all.setOnClickListener((v) -> {
            checkChooseTypeButton(button_search_all);
            currentSearchChooseType = SEARCH_TYPE_ALL;
            startSearch();
        });
        button_search_image.setOnClickListener((v) -> {
            checkChooseTypeButton(button_search_image);
            currentSearchChooseType = SEARCH_TYPE_IMAGE;
            startSearch();
        });
        button_search_video.setOnClickListener((v) -> {
            checkChooseTypeButton(button_search_video);
            currentSearchChooseType = SEARCH_TYPE_VIDEO;
            startSearch();
        });
        button_search_all.setVisibility(View.GONE);
        button_search_image.setVisibility(View.GONE);
        button_search_video.setVisibility(View.GONE);

        //Search
        edit_search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                currentSearchKeyword = s.toString();
                if(currentIsSearchMode) {
                    if(s.length() > 0) {
                        button_search_all.setVisibility(View.VISIBLE);
                        button_search_image.setVisibility(View.VISIBLE);
                        button_search_video.setVisibility(View.VISIBLE);
                    } else
                        quitSearchMode();
                }
            }
        });
        edit_search.setOnFocusChangeListener((v, hasFocus) -> {
            if(currentIsSearchMode != hasFocus) {
                currentIsSearchMode = hasFocus;
                if(currentIsSearchMode) {
                    recycler_main.setEmptyView(empty_search_main);
                } else
                    quitSearchMode();
            }
        });
        edit_search.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {

                if(currentSearchKeyword.isEmpty())
                    return false;

                // 当按了搜索之后关闭软键盘
                KeyBoardUtil.closeKeyboard(edit_search);

                //进行搜索
                startSearch();
                return true;
            }
            return false;
        });
    }

    private View empty_main = null;
    private RecyclerViewEmptySupport recycler_main = null;
    private Button button_search_all = null;
    private Button button_search_image = null;
    private Button button_search_video = null;
    private boolean currentIsSearchMode = false;
    private int currentSearchChooseType = 0;
    private String currentSearchKeyword = "";
    private Button[] searchChooseTypeButtonArr = null;
    private LoadingDialog rotateLoading = null;
    private EditText edit_search = null;

    private void startSearch() {
        mainList.doSearch(currentSearchKeyword, currentSearchChooseType);
        rotateLoading = new LoadingDialog(context);
        rotateLoading.show();
        handler.sendEmptyMessageDelayed(MainMessages.MSG_CLOSE_LOADING, 500);
    }
    private void checkChooseTypeButton(Button target) {
        for(Button b : searchChooseTypeButtonArr) {
            if(b == target) {
                b.setBackgroundResource(R.drawable.btn_round_primary_n);
                b.setTextColor(Color.WHITE);
            } else {
                b.setBackgroundResource(R.drawable.btn_round_n_light);
                b.setTextColor(Color.BLACK);
            }
        }
    }
    private void quitSearchMode() {
        if(edit_search.getText().length() > 0)
            edit_search.setText("");
        if(edit_search.hasFocus())
            edit_search.clearFocus();
        recycler_main.setEmptyView(empty_main);
        mainList.resetSearch();
        button_search_all.setVisibility(View.GONE);
        button_search_image.setVisibility(View.GONE);
        button_search_video.setVisibility(View.GONE);
        KeyBoardUtil.closeKeyboard(edit_search);
    }

    public static final int SEARCH_TYPE_ALL = 0;
    public static final int SEARCH_TYPE_VIDEO = 2;
    public static final int SEARCH_TYPE_IMAGE = 1;

    private void onUpdateScreenOrientation(Configuration newConfig) {
        if(newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            mainList.setListIsGrid(false);
        } else if(newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            mainList.setListIsGrid(true);
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
        if(listDataService.isDataDirty())
            handler.sendEmptyMessageDelayed(MainMessages.MSG_FORCE_LOAD_LIST, 1000);
        if (requestCode == Codes.REQUEST_CODE_OPEN_IMAGE && data != null) {
            //获取选择器返回的数据
            ArrayList<Photo> resultPhotos = data.getParcelableArrayListExtra(EasyPhotos.RESULT_PHOTOS);
            if(resultPhotos != null) {
                for (Photo photo : resultPhotos)
                    mainList.addImageToItem(listDataService.addImageItem(photo.path), false);
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
            case MainMessages.MSG_FORCE_LOAD_LIST:
                loadList();
                break;
            case MainMessages.MSG_CLOSE_LOADING:
                if(rotateLoading != null) {
                    rotateLoading.dismiss();
                    rotateLoading = null;
                }
                break;
        }
    }

    //====================================================
    //列表操作
    //====================================================

    private void loadList() {

        if(currentIsSearchMode)
            quitSearchMode();

        mainList.clear();
        ArrayList<ImageItem> items = listDataService.getImageList();
        for(ImageItem imageItem : items)
            mainList.addImageToItem(imageItem, false);
        mainList.sort();

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
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        mainList.setMainSortReverse(sharedPreferences.getBoolean("main_list_sort_reverse", false));
        mainList.setMainSortType(sharedPreferences.getInt("main_list_sort_type", MainList.MAIN_SORT_DATE));
        mainList.sort();
    }
    private void saveSettings() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putInt("main_list_sort_type", mainList.getMainSortType());
        editor.putBoolean("main_list_sort_reverse", mainList.isMainSortReverse());

        editor.apply();
    }

    //====================================================
    //按钮事件
    //====================================================

    private void onAddImageClick(){
        EasyPhotos.createAlbum(this, false, GlideEngine.getInstance())
                .setPuzzleMenu(false)
                .setCount(32)
                .setVideo(true)
                .setFileProviderAuthority(Constants.FILE_PROVIDER_NAME)
                .start(Codes.REQUEST_CODE_OPEN_IMAGE);
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