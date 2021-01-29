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
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;
import com.hjq.toast.ToastUtils;
import com.huantansheng.easyphotos.EasyPhotos;
import com.huantansheng.easyphotos.models.album.entity.Photo;
import com.imengyu.vr720.R;
import com.imengyu.vr720.VR720Application;
import com.imengyu.vr720.activity.MainActivity;
import com.imengyu.vr720.activity.PanoActivity;
import com.imengyu.vr720.config.Codes;
import com.imengyu.vr720.config.Constants;
import com.imengyu.vr720.config.MainMessages;
import com.imengyu.vr720.dialog.CommonDialog;
import com.imengyu.vr720.dialog.LoadingDialog;
import com.imengyu.vr720.dialog.fragment.AppraiseDialogFragment;
import com.imengyu.vr720.dialog.fragment.ChooseGalleryDialogFragment;
import com.imengyu.vr720.dialog.fragment.ChooseItemDialogFragment;
import com.imengyu.vr720.dialog.fragment.ChooseSystemGalleryDialogFragment;
import com.imengyu.vr720.list.MainList;
import com.imengyu.vr720.model.GalleryItem;
import com.imengyu.vr720.model.ImageItem;
import com.imengyu.vr720.model.TitleSelectionChangedCallback;
import com.imengyu.vr720.model.list.MainListItem;
import com.imengyu.vr720.plugin.GlideEngine;
import com.imengyu.vr720.service.ListDataService;
import com.imengyu.vr720.utils.FileUtils;
import com.imengyu.vr720.utils.KeyBoardUtil;
import com.imengyu.vr720.utils.ShareUtils;
import com.imengyu.vr720.widget.MyTitleBar;
import com.imengyu.vr720.widget.RecyclerViewEmptySupport;
import com.scwang.smart.refresh.footer.ClassicsFooter;
import com.scwang.smart.refresh.header.ClassicsHeader;
import com.scwang.smart.refresh.layout.api.RefreshLayout;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class HomeFragment extends Fragment implements IMainFragment {

    public HomeFragment() {}

    private ListDataService listDataService;
    private Handler handler;
    private MyTitleBar titleBar;
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

        MainActivity mainActivity = (MainActivity)getActivity();
        if(mainActivity != null) {
            listDataService = mainActivity.getListDataService();
            handler = mainActivity.getHandler();
            titleBar = mainActivity.getToolbar();
        }

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

        fab_right = view.findViewById(R.id.fab_right);
        final FloatingActionButton fab_import_file = view.findViewById(R.id.fab_import_file);
        final FloatingActionButton fab_import_gallery = view.findViewById(R.id.fab_import_gallery);
        fab_import_file.setOnClickListener(v -> {
            fab_right.collapse();
            onAddImageFile();
        });
        fab_import_gallery.setOnClickListener(v -> {
            fab_right.collapse();
            onAddImageSystemGallery();
        });

        edit_search = view.findViewById(R.id.edit_search);
        final LinearLayout footerSelection = view.findViewById(R.id.footer_select_main);

        fade_hide = AnimationUtils.loadAnimation(context, R.anim.fade_hide);
        fade_show = AnimationUtils.loadAnimation(context, R.anim.fade_show);

        final View empty_main = view.findViewById(R.id.empty_main);
        empty_search_main = view.findViewById(R.id.empty_search_main);
        text_search_empty_title = view.findViewById(R.id.text_search_empty_title);

        footerSelection.setVisibility(View.GONE);
        RecyclerViewEmptySupport recycler_main = view.findViewById(R.id.recycler_main);
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
        mainList = new MainList(context, application.getListImageCacheService());
        mainList.init(recycler_main);
        mainList.setListCheckableChangedListener(checkable -> {
            if (checkable) {
                fab_right.startAnimation(fade_hide);
                fab_right.setVisibility(View.GONE);
                AnimationSet animationSet = (AnimationSet) AnimationUtils.loadAnimation(getContext(), R.anim.bottom_up);
                footerSelection.startAnimation(animationSet);
                footerSelection.setVisibility(View.VISIBLE);

                if(titleSelectionChangedCallback != null)
                    titleSelectionChangedCallback.onTitleSelectionChangedCallback(
                            true, 0, false);
            } else {
                fab_right.startAnimation(fade_show);
                fab_right.setVisibility(View.VISIBLE);
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
            button_mainsel_share.setEnabled(count > 0);
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
        refreshLayout.setRefreshHeader(new ClassicsHeader(requireContext()).setEnableLastTime(false));
        refreshLayout.setRefreshFooter(new ClassicsFooter(requireContext()));
        refreshLayout.setEnableLoadMore(false);
        refreshLayout.setOnRefreshListener(refreshlayout -> loadList());

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
                        text_search_empty_title.setText(R.string.text_nothing_searched);
                    } else {
                        button_search_all.setVisibility(View.GONE);
                        button_search_image.setVisibility(View.GONE);
                        button_search_video.setVisibility(View.GONE);
                        text_search_empty_title.setText(R.string.text_search_your_pano);
                    }
                }
            }
        });
        edit_search.setOnFocusChangeListener((v, hasFocus) -> {
            if(currentIsSearchMode != hasFocus) {
                currentIsSearchMode = hasFocus;
                if(!currentIsSearchMode) quitSearchMode();
                else enterSearchMode();
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

    private FloatingActionsMenu fab_right = null;
    private Animation fade_hide = null;
    private Animation fade_show = null;
    private View empty_search_main = null;
    private Button button_search_all = null;
    private Button button_search_image = null;
    private Button button_search_video = null;
    private boolean currentIsSearchMode = false;
    private int currentSearchChooseType = 0;
    private String currentSearchKeyword = "";
    private Button[] searchChooseTypeButtonArr = null;
    private LoadingDialog rotateLoading = null;
    private EditText edit_search = null;
    private TextView text_search_empty_title = null;

    private void startSearch() {
        boolean searched = mainList.doSearch(currentSearchKeyword, currentSearchChooseType) > 0;
        empty_search_main.setVisibility(searched ? View.GONE : View.VISIBLE);
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
    private void enterSearchMode() {
        empty_search_main.startAnimation(fade_show);
        empty_search_main.setVisibility(View.VISIBLE);
        text_search_empty_title.setText(R.string.text_search_your_pano);
        fab_right.startAnimation(fade_hide);
        fab_right.setVisibility(View.GONE);
    }
    private void quitSearchMode() {
        if(edit_search.getText().length() > 0)
            edit_search.setText("");
        if(edit_search.hasFocus())
            edit_search.clearFocus();
        mainList.resetSearch();
        empty_search_main.startAnimation(fade_hide);
        empty_search_main.setVisibility(View.GONE);
        button_search_all.setVisibility(View.GONE);
        button_search_image.setVisibility(View.GONE);
        button_search_video.setVisibility(View.GONE);
        fab_right.startAnimation(fade_show);
        fab_right.setVisibility(View.VISIBLE);
        KeyBoardUtil.closeKeyboard(edit_search);
    }

    public static final int SEARCH_TYPE_ALL = 0;
    public static final int SEARCH_TYPE_VIDEO = 2;
    public static final int SEARCH_TYPE_IMAGE = 1;

    private void onUpdateScreenOrientation(Configuration newConfig) {
        if(newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            mainList.setListIsHorizontal(false);
        } else if(newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            mainList.setListIsHorizontal(true);
        }
    }

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
        mainMenu = new PopupMenu(getActivity(), titleBar == null ? null : titleBar.getRightButton(), Gravity.TOP);
        Menu menu = mainMenu.getMenu();
        mainMenu.getMenuInflater().inflate(R.menu.menu_main, menu);
        mainMenu.setOnMenuItemClickListener(this::onOptionsItemSelected);

        action_sort_date = menu.findItem(R.id.action_sort_date);
        action_sort_name = menu.findItem(R.id.action_sort_name);
        action_sort_size = menu.findItem(R.id.action_sort_size);
        action_list_mode = menu.findItem(R.id.action_list_mode);
        action_show_hide_date = menu.findItem(R.id.action_show_hide_date);
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
            handler.sendEmptyMessageDelayed(MainMessages.MSG_FORCE_LOAD_LIST, 700);
        if (requestCode == Codes.REQUEST_CODE_OPEN_IMAGE && data != null) {
            //获取选择器返回的数据
            ArrayList<Photo> resultPhotos = data.getParcelableArrayListExtra(EasyPhotos.RESULT_PHOTOS);
            if(resultPhotos != null) importFiles(resultPhotos);
        }
        else if (requestCode == Codes.REQUEST_CODE_PANO && data != null) {
            boolean needShowAddAskDialog = false;

            String filePath = data.getStringExtra("filePath");
            if(data.getBooleanExtra("isDeleteFile", false)) {
                MainListItem item = mainList.findImageItem(filePath);
                if(item != null) {
                    listDataService.removeImageItem(item.getImageItem());
                    mainList.deleteItem(item);
                    mainList.notifyChange();
                }
            }
            else if (listDataService.findImageItem(filePath) == null) {

                //打开文件之后添加到列表
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
                if(!sharedPreferences.getBoolean("force_no_auto_import", false)) {
                    if (sharedPreferences.getBoolean("auto_import", false)) {

                        mainList.addImageToItem(listDataService.addImageItem(filePath));
                        mainList.sort();

                    } else {
                        needShowAddAskDialog = true;

                        //打开文件之后询问是否添加到列表
                        new CommonDialog(requireActivity())
                                .setTitle(R.string.text_tip)
                                .setMessage(R.string.text_do_you_want_import_to_list)
                                .setPositive(R.string.action_yes)
                                .setNegative(R.string.action_no)
                                .setNeutral(R.string.text_don_not_import_to_list_and_do_not_remind_me_next)
                                .setCheckBoxText(R.string.text_import_to_list_and_do_not_remind_me_next)
                                .setOnResult((b, dialog) -> {
                                    if (b == CommonDialog.BUTTON_POSITIVE) {
                                        mainList.addImageToItem(listDataService.addImageItem(filePath));
                                        mainList.sort();

                                        if (dialog.isCheckBoxChecked()) {
                                            sharedPreferences.edit()
                                                    .putBoolean("auto_import", true)
                                                    .apply();
                                        }

                                        ToastUtils.show(String.format(getString(R.string.text_import_success_count), 1));
                                        return true;
                                    } else if (b == CommonDialog.BUTTON_NEGATIVE) {
                                        sharedPreferences.edit()
                                                .putBoolean("force_no_auto_import", true)
                                                .apply();
                                        return true;
                                    }
                                    return false;
                                })
                                .showAllowingStateLoss();
                    }
                }
            }

            if(!appraiseDialogShowed && !needShowAddAskDialog)
                testAndShowAppraiseDialog();
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
            case MainMessages.MSG_IMPORT_DEMO:{
                String path = (String)msg.obj;
                if(listDataService.findImageItem(path) == null) {
                    mainList.addImageToItem(listDataService.addImageItem(path));
                    mainList.sort();
                }
                break;
            }
            case MainMessages.MSG_LIST_LOAD_FINISH:
                refreshLayout.finishRefresh();
                break;
            case MainMessages.MSG_ADD_IMAGE:
                onAddImageFile();
                break;
            case MainMessages.MSG_ADD_IMAGE_GALLERY:
                onAddImageSystemGallery();
                break;
            case MainMessages.MSG_RELOAD_MAIN_LIST:
            case MainMessages.MSG_FORCE_LOAD_LIST:
                loadList();
                break;
            case MainMessages.MSG_CLOSE_LOADING:
                if(rotateLoading != null) {
                    rotateLoading.dismiss();
                    rotateLoading = null;
                }
                break;
            case MainMessages.MSG_LATE_SHOW_APPRAISE:
                AppraiseDialogFragment appraiseDialogFragment = new AppraiseDialogFragment();
                appraiseDialogFragment.show(getParentFragmentManager(), "AppraiseDialog");
                break;
        }
    }

    public void importFiles(ArrayList<CharSequence> list, int importCount) {
        Log.d("MainList", "importFiles: " + importCount);
        ArrayList<String> photoPath = new ArrayList<>();
        for (CharSequence photo : list)
            photoPath.add(photo.toString());
        importFilesPath(photoPath);
    }
    private void importFiles(ArrayList<Photo> photos) {
        ArrayList<String> photoPath = new ArrayList<>();
        for (Photo photo : photos)
            photoPath.add(photo.path);
        importFilesPath(photoPath);
    }
    private void importFilesPath(ArrayList<String> photos) {
        int addCount = 0;
        for (String photo : photos) {
            ImageItem imageItem = listDataService.findImageItem(photo);
            if (imageItem == null){
                mainList.addImageToItem(listDataService.addImageItem(photo));
                addCount++;
            } else if(!imageItem.showInMain) {
                imageItem.showInMain = true;
                mainList.addImageToItem(imageItem);
                addCount++;
            }
        }
        mainList.sort();

        ToastUtils.show(String.format(getString(R.string.text_import_success_count), addCount));
    }
    private void notifyGalleriesUpdate(int galleryId) {
        Message message = new Message();
        message.what = MainMessages.MSG_REFRESH_GALLERY_ITEM;
        message.obj = galleryId;
        handler.sendMessage(message);
    }

    //====================================================
    //列表操作
    //====================================================

    private void loadList() {

        if(currentIsSearchMode)
            quitSearchMode();

        mainList.clear();
        ArrayList<ImageItem> items = listDataService.getImageList();
        for(ImageItem imageItem : items) {
            if(imageItem.showInMain)
                mainList.addImageToItem(imageItem);
        }
        mainList.sort();

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                handler.sendEmptyMessage(MainMessages.MSG_LIST_LOAD_FINISH);
            }
        }, 800);
    }

    //====================================================
    //评价对话框
    //====================================================

    private boolean appraiseDialogShowed = false;
    private void testAndShowAppraiseDialog() {
        appraiseDialogShowed = sharedPreferences.getBoolean("appraise_dialog_opened", false);
        if(!appraiseDialogShowed) {
            long lastShowTime = sharedPreferences.getLong("last_show_appraise_dialog", 0);
            if(new Date().getTime() - lastShowTime > 311040000) {
                handler.sendEmptyMessageDelayed(MainMessages.MSG_LATE_SHOW_APPRAISE, 1200);
            }
        }
    }

    //====================================================
    //设置保存与读取
    //====================================================

    private SharedPreferences sharedPreferences = null;

    private void loadSettings() {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        appraiseDialogShowed = sharedPreferences.getBoolean("appraise_dialog_opened", false);
        mainList.setSortReverse(sharedPreferences.getBoolean("main_list_sort_reverse", false));
        mainList.setGroupByDate(sharedPreferences.getBoolean("main_list_group_by_date", true));
        mainList.setListIsGrid(sharedPreferences.getBoolean("main_list_is_grid", false));
        mainList.setSortType(sharedPreferences.getInt("main_list_sort_type", MainList.SORT_DATE));
        mainList.sort();

        updateSortMenuActive();
        updateShowHideDateMenuActive();
        updateListGridMode();
    }
    private void saveSettings() {
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putBoolean("main_list_group_by_date", mainList.isGroupByDate());
        editor.putBoolean("main_list_is_grid", mainList.isGrid());
        editor.putInt("main_list_sort_type", mainList.getSortType());
        editor.putBoolean("main_list_sort_reverse", mainList.isSortReverse());

        editor.apply();
    }

    //====================================================
    //按钮事件
    //====================================================

    private void onAddImageFile() {
        EasyPhotos.createAlbum(this, false, GlideEngine.getInstance())
                .setPuzzleMenu(false)
                .setCount(500)
                .setVideo(true)
                .setFileProviderAuthority(Constants.FILE_PROVIDER_NAME)
                .start(Codes.REQUEST_CODE_OPEN_IMAGE);
    }
    private void onAddImageSystemGallery() {
        ChooseSystemGalleryDialogFragment chooseSystemGalleryDialog = new ChooseSystemGalleryDialogFragment();
        chooseSystemGalleryDialog.setOnChooseGalleryListener((album) -> {

            ChooseItemDialogFragment chooseImportMethod = new ChooseItemDialogFragment(
                    getString(R.string.text_how_to_import_gallery),
                    new String[] {
                            getString(R.string.text_import_all_images),
                            String.format(getString(R.string.text_import_to_gallery), album.name),
                    }
            );
            chooseImportMethod.setOnChooseItemListener((c, i, str) -> {
                if(i == 0) importFiles(album.photos);
                else if(i == 1) {
                    //新建相册
                    GalleryItem galleryItem = GalleryItem.newInstance(listDataService, album.name);
                    listDataService.addGalleryItem(galleryItem);

                    //导入图片至相册
                    int addCount = 0;
                    for (Photo photo : album.photos) {
                        ImageItem imageItem = listDataService.findImageItem(photo.path);
                        if (imageItem == null) {
                            imageItem = listDataService.addImageItem(photo.path, galleryItem.id, false);
                            mainList.addImageToItem(imageItem);
                        } else {
                            imageItem.belongGalleries.add(galleryItem.id);
                        }
                        addCount++;
                    }
                    mainList.sort();

                    //相册界面更新
                    notifyGalleriesUpdate(-6);

                    ToastUtils.show(String.format(getString(R.string.text_import_success_count), addCount));
                }
            });
            chooseImportMethod.show(getParentFragmentManager(), "ChooseGalleryImportMethod");
        });
        chooseSystemGalleryDialog.show(getParentFragmentManager(), "ChooseSystemGallery");
    }
    private void onClearClick() {
        new CommonDialog(requireActivity())
                .setTitle(R.string.text_would_you_want_clear_list)
                .setMessage(R.string.text_all_list_will_be_clear)
                .setPositive(R.string.action_sure_clear)
                .setNegative(R.string.action_cancel)
                .setOnResult((b, dialog) -> {
                    if(b == CommonDialog.BUTTON_POSITIVE) {
                        //clear in listDataService
                        listDataService.clearImageItems();
                        //clear
                        mainList.clear();
                        return true;
                    } else return b == CommonDialog.BUTTON_NEGATIVE;
                })
                .show();
    }
    private void onSortClick(int sortType){ mainList.sort(sortType); }
    private void onOpenImageClick(String filePath) {
        Intent intent = new Intent(getActivity(), PanoActivity.class);
        intent.putExtra("openFilePath", filePath);
        intent.putExtra("openFileArgPath", filePath);
        intent.putCharSequenceArrayListExtra("fileList", mainList.getMainListPathItems());
        startActivityForResult(intent, Codes.REQUEST_CODE_PANO);
    }
    private void onShareImageClick() {
        final List<MainListItem> sel = mainList.getSelectedItems();
        if(sel.size() == 1)
            ShareUtils.shareFile(getContext(), sel.get(0).getFilePath());
        else if(sel.size() > 1) {
            List<File> list = new ArrayList<>();
            for(MainListItem item : sel)
                list.add(new File(item.getFilePath()));
            ShareUtils.shareStreamMultiple(getContext(), list);
        }

        mainList.setListCheckMode(false);
    }
    private void onDeleteImageClick() {
        final List<MainListItem> sel = mainList.getSelectedItems();
        if(sel.size() > 0) {
            new CommonDialog(requireActivity())
                    .setTitle(R.string.text_sure_delete_pano)
                    .setMessage(String.format(resources.getString(R.string.text_you_can_remove_choosed_images), sel.size()))
                    .setPositive(R.string.action_sure_delete)
                    .setNegative(R.string.action_cancel)
                    .setCheckBoxText(R.string.text_also_delete_files)
                    .setCancelable(true)
                    .setOnResult((b, dialog) -> {
                        if(b == CommonDialog.BUTTON_POSITIVE) {
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
                            mainList.setListCheckMode(false, false);
                            return true;
                        } else return b == CommonDialog.BUTTON_NEGATIVE;
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
            ChooseGalleryDialogFragment chooseGalleryDialogFragment = new ChooseGalleryDialogFragment();
            chooseGalleryDialogFragment.setHandler(handler);
            chooseGalleryDialogFragment.setOnChooseGalleryListener(galleryId -> {

                //添加到对应相册
                ImageItem imageItem;
                for(MainListItem item : sel) {
                    imageItem = item.getImageItem();
                    if(!imageItem.isInBelongGalleries(galleryId))
                        imageItem.belongGalleries.add(galleryId);
                }

                //发送消息到相册界面进行相册缩略图刷新
                notifyGalleriesUpdate(galleryId);

                ToastUtils.show(R.string.text_add_success);

                //关闭选择模式
                mainList.setListCheckMode(false);
            });
            chooseGalleryDialogFragment.show(getParentFragmentManager(), "ChooseGallery");
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
            onSortClick(MainList.SORT_DATE);
            updateSortMenuActive();
            return true;
        }
        if(id == R.id.action_sort_name) {
            onSortClick(MainList.SORT_NAME);
            updateSortMenuActive();
            return true;
        }
        if(id == R.id.action_sort_size) {
            onSortClick(MainList.SORT_SIZE);
            updateSortMenuActive();
            return true;
        }
        if(id == R.id.action_list_mode) {
            mainList.setListIsGrid(!mainList.isGrid());
            updateListGridMode();
        }
        if(id == R.id.action_show_hide_date) {
            mainList.setGroupByDate(!mainList.isGroupByDate());
            updateShowHideDateMenuActive();
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateShowHideDateMenuActive() {
        action_show_hide_date.setTitle(mainList.isGroupByDate() ?
                R.string.text_hide_date : R.string.text_show_date);
    }
    private void updateSortMenuActive() {

        int sort = mainList.getSortType();
        int icon = mainList.isSortReverse() ? R.drawable.ic_sort_up : R.drawable.ic_sort_down;

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
    private void updateListGridMode() {
        if(mainList.isGrid()) {
            action_list_mode.setTitle(getString(R.string.text_list_mode));
            action_show_hide_date.setVisible(true);
        }
        else {
            action_list_mode.setTitle(getString(R.string.text_grid_mode));
            action_show_hide_date.setVisible(false);
        }
    }
}