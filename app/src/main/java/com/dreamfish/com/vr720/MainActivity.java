package com.dreamfish.com.vr720;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.donkingliang.imageselector.utils.ImageSelector;
import com.dreamfish.com.vr720.utils.DatabaseHelper;
import com.dreamfish.com.vr720.utils.FileSizeUtil;
import com.dreamfish.com.vr720.utils.FileUtils;
import com.dreamfish.com.vr720.utils.ImageUtils;
import com.dreamfish.com.vr720.widget.MainThumbnailImageView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static com.donkingliang.imageselector.utils.ImageSelector.SELECT_RESULT;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> onAddImageClick());

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        final LinearLayout footerSelection = findViewById(R.id.footer_select_main);

        resources = getResources();
        supportActionBar = getSupportActionBar();

        mainListAdapter = new MainListAdapter(MainActivity.this, R.layout.item_main, mainListItems);
        mainListAdapter.setMainListCheckableChangedListener(checkable -> {
            if (checkable) {
                fab.hide();
                AnimationSet animationSet = (AnimationSet) AnimationUtils.loadAnimation(MainActivity.this, R.anim.bottom_up);
                footerSelection.startAnimation(animationSet);
                footerSelection.setVisibility(View.VISIBLE);
                supportActionBar.hide();
            } else {
                fab.show();
                AnimationSet animationSet = (AnimationSet) AnimationUtils.loadAnimation(MainActivity.this, R.anim.buttom_down);
                footerSelection.startAnimation(animationSet);
                footerSelection.setVisibility(View.GONE);
                supportActionBar.show();
            }
        }
        );
        mainListAdapter.registerDataSetObserver(new DataSetObserver() {

            private boolean nextChangedDoNotNotify = false;
            @Override
            public void onChanged() {
                super.onChanged();
                if(nextChangedDoNotNotify){
                    nextChangedDoNotNotify = false;
                    return;
                }
                if(mainListItems.size() > 6){
                    for(int i = mainListItems.size() - 1;i>=0;i--){
                        MainListItem item = mainListItems.get(i);
                        if(item.getForceItemType() == MainListItem.ITEM_TYPE_TEXT)
                            mainListItems.remove(item);
                    }
                    mainListItems.add(new MainListItem(resources.getString(R.string.text_end)));
                    mainListItems.add(new MainListItem("\n\n"));
                    nextChangedDoNotNotify=true;
                    mainListAdapter.notifyDataSetChanged();
                }else if(mainListItems.size() > 0){
                    for(int i = mainListItems.size() - 1;i>=0;i--){
                        MainListItem item = mainListItems.get(i);
                        if(item.getForceItemType() == MainListItem.ITEM_TYPE_TEXT)
                            mainListItems.remove(item);
                    }
                    nextChangedDoNotNotify=true;
                    mainListAdapter.notifyDataSetChanged();

                }

            }
        });

        ListView listView = findViewById(R.id.listview_main);
        listView.setAdapter(mainListAdapter);
        mainMultiChoiceModeListener = new MultiChoiceModeListener(listView);
        listView.setMultiChoiceModeListener(mainMultiChoiceModeListener);
        listView.setOnItemClickListener((parent, view, position, id) -> {
            MainListItem item = mainListAdapter.getItem(position);
            if(item != null)
                onOpenImageClick(item.getFilePath());
        });
        listView.setDividerHeight(0);
        listView.setDivider(null);
        listView.setEmptyView(findViewById(R.id.empty_main));

        loadList();

        findViewById(R.id.button_mainsel_openwith).setOnClickListener(v -> onOpenImageWithImageClick());
        findViewById(R.id.button_mainsel_delete).setOnClickListener(v -> onDeleteImageClick());
    }

    @Override
    protected void onDestroy() {
        saveList();
        super.onDestroy();
    }

    //====================================================
    //控件
    //====================================================

    private ActionBar supportActionBar = null;
    private Resources resources;

    //====================================================
    //返回键
    //====================================================

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    // 用来计算返回键的点击间隔时间
    private long exitTime = 0;

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
            DrawerLayout drawer = findViewById(R.id.drawer_layout);
            //关闭 DrawerLayout
            if (drawer.isDrawerOpen(GravityCompat.START)) {
                drawer.closeDrawer(GravityCompat.START);
            } else {
                if ((System.currentTimeMillis() - exitTime) > 2000) {
                    //弹出提示，可以有多种方式
                    Toast.makeText(getApplicationContext(), resources.getText(R.string.text_press_once_more_to_quit),
                            Toast.LENGTH_SHORT).show();
                    exitTime = System.currentTimeMillis();
                } else {
                    finish();
                }
            }
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    //====================================================
    //菜单
    //====================================================

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_clear) {
            onClearClick();
            return true;
        } else if (id == R.id.action_sort_date) {
            onSortClick(MAIN_SORT_DATE);
            return true;
        } else if (id == R.id.action_sort_name) {
            onSortClick(MAIN_SORT_NAME);
            return true;
        } else if (id == R.id.action_sort_size) {
            onSortClick(MAIN_SORT_SIZE);
            return true;
        }


        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            onAddImageClick();
        } else if (id == R.id.nav_manage) {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_send) {

        } else if (id == R.id.nav_about) {
            Intent intent = new Intent(MainActivity.this, AboutActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_quit) {
            finish();
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }


    //====================================================
    //主线程handler
    //====================================================

    private static final int MSG_REFESH_LIST = 335;
    private static final int MSG_TEST_LIST = 336;

    private static class MainHandler extends Handler {
        private final WeakReference<MainActivity> mTarget;

        MainHandler(MainActivity target) {
            mTarget = new WeakReference<>(target);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_REFESH_LIST:
                    mTarget.get().mainListAdapter.notifyDataSetChanged();
                    break;
                case MSG_TEST_LIST:
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }
    private final MainHandler handler = new MainHandler(this);

    //====================================================
    //主列表控制
    //====================================================

    private List<MainListItem> mainListItems = new ArrayList<>();
    private MainListAdapter mainListAdapter = null;
    private MultiChoiceModeListener mainMultiChoiceModeListener= null;

    private interface OnMainListCheckableChangedListener {
        void onMainListCheckableChangedListener(boolean checkable);
    }

    private class ViewHolder {
        MainThumbnailImageView imageView;
        TextView textView;
    }

    //主列表数据
    private class MainListItem {

        private static final int ITEM_TYPE_NORMAL = 827;
        private static final int ITEM_TYPE_TEXT = 828;

        private String filePath;
        private String fileName;
        private String fileSize;
        private Drawable thumbnail;
        private boolean thumbnailLoading;
        private boolean thumbnailLoadingStarted;
        private boolean thumbnailFail;
        private boolean checked;
        private int checkeIndex;
        private long fileSizeValue;
        private long fileModifyDate;
        private int forceItemType;

        MainListItem(String itemText){
            this.forceItemType = ITEM_TYPE_TEXT;
            this.fileName = itemText;
        }
        MainListItem(String filePath, String fileName, String fileSize) {
            this.filePath = filePath;
            this.fileName = fileName;
            this.fileSize = fileSize;
            this.forceItemType = ITEM_TYPE_NORMAL;
        }

        Drawable getThumbnail() {
            return thumbnail;
        }
        void setThumbnail(Drawable thumbnail) {
            this.thumbnail = thumbnail;
        }
        String getFilePath() {
            return filePath;
        }
        void setFilePath(String filePath) {
            this.filePath = filePath;
        }
        String getFileName() {
            return fileName;
        }
        void setFileName(String fileName) {
            this.fileName = fileName;
        }
        String getFileSize() {
            return fileSize;
        }
        void setFileSize(String fileSize) {
            this.fileSize = fileSize;
        }
        boolean isChecked() {
            return checked;
        }
        void setChecked(boolean checked) {
            this.checked = checked;
        }
        int getCheckeIndex() {
            return checkeIndex;
        }
        void setCheckeIndex(int checkeIndex) {
            this.checkeIndex = checkeIndex;
        }
        boolean isThumbnailLoading() {
            return thumbnailLoading;
        }
        void setThumbnailLoading(boolean thumbnailLoading) {
            this.thumbnailLoading = thumbnailLoading;
        }
        boolean isThumbnailFail() {
            return thumbnailFail;
        }
        void setThumbnailFail(boolean thumbnailFail) {
            this.thumbnailFail = thumbnailFail;
        }
        long getFileSizeValue() {
            return fileSizeValue;
        }
        void setFileSizeValue(long fileSizeValue) {
            this.fileSizeValue = fileSizeValue;
        }
        long getFileModifyDate() {
            return fileModifyDate;
        }
        void setFileModifyDate(long fileModifyDate) {
            this.fileModifyDate = fileModifyDate;
        }
        boolean isThumbnailLoadingStarted() {
            return thumbnailLoadingStarted;
        }
        void setThumbnailLoadingStarted(boolean thumbnailLoadingStarted) {
            this.thumbnailLoadingStarted = thumbnailLoadingStarted;
        }
        int getForceItemType() {
            return forceItemType;
        }
        void setForceItemType(int forceItemType) {
            this.forceItemType = forceItemType;
        }
    }

    //主列表适配器
    public class MainListAdapter extends ArrayAdapter<MainListItem> {

        private boolean mCheckable;
        private OnMainListCheckableChangedListener mainListCheckableChangedListener;

        MainListAdapter(Context context, int layoutId, List<MainListItem> list) {
            super(context, layoutId, list);
        }

        boolean isCheckable() {
            return mCheckable;
        }

        void setCheckable(boolean mCheckable) {
            this.mCheckable = mCheckable;
            if (this.mainListCheckableChangedListener != null)
                this.mainListCheckableChangedListener.onMainListCheckableChangedListener(mCheckable);
        }

        void setMainListCheckableChangedListener(OnMainListCheckableChangedListener mainListCheckableChangedListener) {
            this.mainListCheckableChangedListener = mainListCheckableChangedListener;
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            final MainListItem item = getItem(position);

            ViewHolder viewHolder;

            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_main, parent, false);
                viewHolder = new ViewHolder();
                viewHolder.textView = convertView.findViewById(R.id.text_item);
                viewHolder.imageView = convertView.findViewById(R.id.img_item);

                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            if(item != null) {
                if (item.getForceItemType() == MainListItem.ITEM_TYPE_NORMAL) {
                    viewHolder.imageView.setChecked(item.isChecked());
                    if (item.isThumbnailFail())
                        viewHolder.imageView.setImageResource(R.drawable.ic_noprob);
                    else if (item.isThumbnailLoading()) {
                        viewHolder.imageView.setImageResource(R.drawable.ic_tumb);

                        if (!item.isThumbnailLoadingStarted()) {
                            item.setThumbnailLoadingStarted(true);
                            //在背景线程进行缩略图加载
                            new Thread(() -> {
                                try {
                                    item.thumbnail = new BitmapDrawable(resources, ImageUtils.revitionImageSize(item.filePath, 800, 400));
                                    item.setThumbnailLoading(false);
                                    handler.sendEmptyMessage(MSG_REFESH_LIST);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    item.setThumbnailLoading(false);
                                    item.setThumbnailFail(true);
                                    handler.sendEmptyMessage(MSG_REFESH_LIST);
                                }
                            }).start();
                        }
                    } else viewHolder.imageView.setImageDrawable(item.getThumbnail());

                    viewHolder.imageView.setVisibility(View.VISIBLE);
                    viewHolder.textView.setVisibility(View.GONE);

                    viewHolder.imageView.setImageText(item.getFileName());
                    viewHolder.imageView.setImageSize(item.getFileSize());
                    viewHolder.imageView.setCheckIndex(item.getCheckeIndex());
                } else if (item.getForceItemType() == MainListItem.ITEM_TYPE_TEXT) {
                    viewHolder.imageView.setVisibility(View.GONE);
                    viewHolder.textView.setVisibility(View.VISIBLE);

                    viewHolder.textView.setText(item.getFileName());
                }
            }
            return convertView;
        }
    }

    //主列表多选
    private class MultiChoiceModeListener implements AbsListView.MultiChoiceModeListener {

        private ListView mListView;
        private TextView mTitleTextView;

        List<MainListItem> mSelectedItems = new ArrayList<>();

        private MultiChoiceModeListener(ListView listView) {
            mListView = listView;
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.check_task_priority, menu);

            @SuppressLint("InflateParams")
            View multiSelectActionBarView = LayoutInflater.from(MainActivity.this)
                    .inflate(R.layout.app_bar_selection, null);
            mode.setCustomView(multiSelectActionBarView);
            mTitleTextView = multiSelectActionBarView.findViewById(R.id.textview_main_selected_count);
            mTitleTextView.setText("已选择 0 项");

            mainListAdapter.setCheckable(true);
            mainListAdapter.notifyDataSetChanged();

            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            for (MainListItem item : mSelectedItems) {
                item.setChecked(false);
                item.setCheckeIndex(0);
            }
            mSelectedItems.clear();
            mainListAdapter.setCheckable(false);
            mainListAdapter.notifyDataSetChanged();
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            final int id = item.getItemId();
            switch (id) {
                case R.id.check_all:
                    mSelectedItems.clear();
                    mSelectedItems.addAll(0, mainListItems);
                    int i = 1;
                    for (MainListItem m : mSelectedItems) {
                        m.setChecked(true);
                        m.setCheckeIndex(i);
                        i++;
                    }
                    mTitleTextView.setText(String.format(resources.getString(R.string.text_choosed_items), (i - 1)));
                    mainListAdapter.notifyDataSetChanged();
                    break;
                case R.id.check_cancel:
                    for (MainListItem m : mSelectedItems) {
                        m.setChecked(false);
                        m.setCheckeIndex(0);
                    }
                    mSelectedItems.clear();
                    mTitleTextView.setText(String.format(resources.getString(R.string.text_choosed_items), 0));
                    mainListAdapter.notifyDataSetChanged();
                    break;
                default:
                    break;
            }
            return true;
        }

        @Override
        public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
            MainListItem item = mainListAdapter.getItem(position);
            if (item != null) {
                item.setChecked(checked);
                if (checked)
                    mSelectedItems.add(item);
                else {
                    item.setCheckeIndex(0);
                    mSelectedItems.remove(item);
                }
            }
            rebuildCheckIndexs();
            mTitleTextView.setText(
                    String.format(resources.getString(R.string.text_choosed_items),
                            mListView.getCheckedItemCount()));
            mainListAdapter.notifyDataSetChanged();
        }

        private void rebuildCheckIndexs() {
            int i = 0;
            for (MainListItem item : mSelectedItems) {
                i++;
                item.setCheckeIndex(i);
            }
        }
    }

    //条目是否已存在
    private boolean existsImageItem(String filePath) {
        for(MainListItem m : mainListItems){
            if(filePath.equals(m.getFilePath()))
                return true;
        }
        return false;
    }
    //条目添加
    private void addImageToItem(String filePath, boolean notify) {
        File f = new File(filePath);
        if (f.exists()) {

            if(existsImageItem(filePath))
                return;

            final MainListItem newItem = new MainListItem(filePath, FileUtils.getFileName(filePath), FileSizeUtil.getAutoFileOrFilesSize(filePath));
            newItem.setThumbnailLoading(true);
            newItem.setThumbnailFail(false);
            newItem.setFileModifyDate(f.lastModified());
            newItem.setFileSizeValue(f.length());

            mainListItems.add(newItem);
            if (notify) mainListAdapter.notifyDataSetChanged();
        }
    }

    //====================================================
    //条目排序
    //====================================================

    private static final int MAIN_SORT_DATE = 681;
    private static final int MAIN_SORT_NAME = 682;
    private static final int MAIN_SORT_SIZE = 683;

    private int mainSortType = MAIN_SORT_DATE;

    private class MainComparatorValues implements Comparator<MainListItem> {

        @Override
        public int compare(MainListItem m1, MainListItem m2) {
            int result = 0;
            if(mainSortType==MAIN_SORT_DATE){
                long old1=m1.getFileModifyDate();
                long old2=m2.getFileModifyDate();
                if (old1> old2) result = 1;
                if (old1 < old2) result = -1;
            } else if(mainSortType==MAIN_SORT_NAME){
                result = m1.getFileName().compareTo(m2.getFileName());
            } else if(mainSortType==MAIN_SORT_SIZE){
                long old1=m1.getFileSizeValue();
                long old2=m2.getFileSizeValue();
                if (old1> old2) result = 1;
                if (old1 < old2) result = -1;
            }

            return result;
        }
    }


    private void sort(int sortType) {
        mainSortType = sortType;
        Collections.sort(mainListItems, new MainComparatorValues());
        mainListAdapter.notifyDataSetChanged();
    }


    //====================================================
    //条目保存与读取
    //====================================================

    private DatabaseHelper databaseHelper;

    private void loadList() {
        databaseHelper = new DatabaseHelper(this);

        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("select * from image_list", null);
        while (cursor.moveToNext()) {
            String path = cursor.getString(1);
            addImageToItem(path, false);
        }

        mainListAdapter.notifyDataSetChanged();

        cursor.close();
        db.close();

    }
    private void saveList() {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();

        db.execSQL("delete from 'image_list'; ");
        for(MainListItem i : mainListItems) {
            if(i.getForceItemType() == MainListItem.ITEM_TYPE_NORMAL) {
                String sql = "insert into image_list(path) values('" + i.filePath + "')";
                db.execSQL(sql);
            }
        }
        db.close();
        databaseHelper.close();
    }

    //====================================================
    //Activity 返回值
    //====================================================

    private static final int REQUEST_CODE_OPENIMAGE = 732;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_OPENIMAGE && data != null) {
            //获取选择器返回的数据
            ArrayList<String> images = data.getStringArrayListExtra(SELECT_RESULT);
            if(images!=null) {
                for (String path : images)
                    addImageToItem(path, false);
                mainListAdapter.notifyDataSetChanged();
            }
        }
    }

    //====================================================
    //按钮事件
    //====================================================

    /**
     * 添加图片点击
     */
    private void onAddImageClick(){
        //不限数量的多选
        ImageSelector.builder()
                .useCamera(false) // 设置是否使用拍照
                .setSingle(false)  //设置是否单选
                .setMaxSelectCount(0) // 图片的最大选择数量，小于等于0时，不限数量。
                .start(this, REQUEST_CODE_OPENIMAGE); // 打开相册

    }
    private void onClearClick() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(resources.getText(R.string.text_would_you_want_clear_list));
        builder.setMessage(resources.getText(R.string.action_cancel));
        builder.setPositiveButton(resources.getText(R.string.text_all_list_wiil_be_clear), (dialog, which) -> {
            mainListItems.clear();
            mainListAdapter.notifyDataSetChanged();
        });
        builder.setNegativeButton(resources.getText(R.string.action_cancel), null);
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }
    private void onSortClick(int sortType){
        sort(sortType);
    }
    private void onOpenImageClick(String filePath) {
        Intent intent = new Intent(MainActivity.this, PanoActivity.class);
        intent.putExtra("filePath", filePath);
        startActivity(intent);
    }
    private void onDeleteImageClick(){
        final int selCount = mainMultiChoiceModeListener.mSelectedItems.size();
        if(selCount > 0){
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.text_sure_delete_pano);
            builder.setMessage(
                    String.format(resources.getString(R.string.text_youcan_remove_choosed_images),
                            selCount)
            );
            builder.setPositiveButton(R.string.action_move_out_list, (dialog, which) -> {
                for(int i=0;i<selCount;i++)
                    mainListItems.remove(mainMultiChoiceModeListener.mSelectedItems.get(i));
                mainListAdapter.notifyDataSetChanged();
            });
            builder.setNeutralButton(R.string.action_delete_file, (dialog, which) -> {
                for(int i=0;i<selCount;i++) {
                    mainListItems.remove(mainMultiChoiceModeListener.mSelectedItems.get(i));
                }
                mainListAdapter.notifyDataSetChanged();
            });
            builder.setNegativeButton(R.string.action_cancel, null);
            AlertDialog alertDialog = builder.create();
            alertDialog.show();
        }else Toast.makeText(this, R.string.text_choose_some_file_to_del, Toast.LENGTH_LONG).show();
    }
    private void onOpenImageWithImageClick(){
        if(mainMultiChoiceModeListener.mSelectedItems.size() == 1){
            FileUtils.openFile(this, mainMultiChoiceModeListener.mSelectedItems.get(0).filePath);
        }else Toast.makeText(this, resources.getText(R.string.text_choose_one_file_to_open), Toast.LENGTH_LONG).show();
    }
}
