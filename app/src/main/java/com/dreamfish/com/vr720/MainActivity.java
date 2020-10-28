package com.dreamfish.com.vr720;

import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.donkingliang.imageselector.utils.ImageSelector;
import com.dreamfish.com.vr720.config.MainMessages;
import com.dreamfish.com.vr720.dialog.CommonDialog;
import com.dreamfish.com.vr720.list.MainList;
import com.dreamfish.com.vr720.utils.DatabaseHelper;
import com.dreamfish.com.vr720.utils.FileUtils;
import com.dreamfish.com.vr720.utils.StatusBarUtils;
import com.dreamfish.com.vr720.widget.MyTitleBar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        resources = getResources();
        toolbar = findViewById(R.id.toolbar);

        StatusBarUtils.setLightMode(this);

        initDrawer();
        initControl();
        initMenu();

        loadList();
    }

    @Override
    protected void onDestroy() {
        saveList();
        super.onDestroy();
    }

    //====================================================
    //控件
    //====================================================

    private Resources resources;
    private MainList mainList = null;
    private MyTitleBar toolbar;
    private DrawerLayout drawerLayout;
    private PopupMenu mainMenu;

    private void initMenu() {
        mainMenu = new PopupMenu(MainActivity.this, toolbar.getRightButton());
        mainMenu.getMenuInflater().inflate(R.menu.menu_main, mainMenu.getMenu());
        mainMenu.setOnMenuItemClickListener(this::onOptionsItemSelected);
    }
    private void initDrawer() {

        drawerLayout = findViewById(R.id.drawer_layout);

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        DrawerLayout.DrawerListener drawerListener = new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                // 得到contentView 实现侧滑界面出现后主界面向右平移避免侧滑界面遮住主界面
                View content = drawerLayout.getChildAt(0);
                int offset = (int) (drawerView.getWidth() * slideOffset);
                content.setTranslationX(offset);
            }
            @Override
            public void onDrawerOpened(@NonNull View drawerView) {}
            @Override
            public void onDrawerClosed(@NonNull View drawerView) {}
            @Override
            public void onDrawerStateChanged(int newState) {}
        };

        drawerLayout.addDrawerListener(drawerListener);
        drawerLayout.setScrimColor(Color.TRANSPARENT);
        drawerLayout.setDrawerElevation(0.0f);
    }
    private void initControl() {
        final FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> onAddImageClick());

        final LinearLayout footerSelection = findViewById(R.id.footer_select_main);
        ListView listView = findViewById(R.id.listview_main);

        final View button_mainsel_openwith = findViewById(R.id.button_mainsel_openwith);
        final View button_mainsel_delete = findViewById(R.id.button_mainsel_delete);

        button_mainsel_openwith.setOnClickListener(v -> onOpenImageWithImageClick());
        button_mainsel_delete.findViewById(R.id.button_mainsel_delete).setOnClickListener(v -> onDeleteImageClick());

        toolbar.setLeftIconOnClickListener((v) -> {
            if(mainList.isMainListCheckMode()) {
                mainList.setMainListCheckMode(false);
            }else {
                if (drawerLayout.isDrawerOpen(GravityCompat.START))
                    drawerLayout.closeDrawer(GravityCompat.START);
                else
                    drawerLayout.openDrawer(GravityCompat.START);
            }
        });
        toolbar.setRightIconOnClickListener((v) -> {
            if(mainList.isMainListCheckMode()) {
                if(mainList.getSelectedItemCount() >= mainList.getMainListItemCount())
                    mainList.clearSelectedItems();
                else
                    mainList.selectAllItems();
            }else
                mainMenu.show();
        });

        mainList = new MainList(this);
        mainList.init(handler, listView);
        mainList.setMainListCheckableChangedListener(checkable -> {
            if (checkable) {
                fab.hide();
                AnimationSet animationSet = (AnimationSet) AnimationUtils.loadAnimation(this, R.anim.bottom_up);
                footerSelection.startAnimation(animationSet);
                footerSelection.setVisibility(View.VISIBLE);
                toolbar.setTitle(getString(R.string.text_please_choose_item));
                toolbar.setLeftButtonIconResource(R.drawable.ic_close);
                toolbar.setRightButtonIconResource(R.drawable.ic_check_all);
            } else {
                fab.show();
                AnimationSet animationSet = (AnimationSet) AnimationUtils.loadAnimation(this, R.anim.buttom_down);
                footerSelection.startAnimation(animationSet);
                footerSelection.setVisibility(View.GONE);
                toolbar.setTitle("");
                toolbar.setLeftButtonIconResource(R.drawable.ic_menu);
                toolbar.setRightButtonIconResource(R.drawable.ic_more);
            }
        });
        mainList.setMainListCheckItemCountChangedListener((count) -> {
            if(mainList.isMainListCheckMode()) {

                DrawableCompat.setTint(toolbar.getRightButton().getForeground(),
                        mainList.getSelectedItemCount() == mainList.getMainListItemCount() ?
                                resources.getColor(R.color.colorPrimary, null) :
                                Color.BLACK);

                toolbar.setTitle(count > 0 ?
                        String.format(getString(R.string.text_choosed_items), count) :
                        resources.getString(R.string.text_please_choose_item));

                button_mainsel_delete.setEnabled(count > 0);
                button_mainsel_openwith.setEnabled(count == 1);
            }
            else {
                DrawableCompat.setTint(toolbar.getRightButton().getForeground(), Color.BLACK);
                toolbar.setTitle("");
            }
        });
        mainList.setMainListOnItemClickListener((parent, view, position, id) -> {
            MainList.MainListItem item = mainList.getMainListAdapter().getItem(position);
            if(item != null) {
                onOpenImageClick(item.getFilePath());
            }
        });

        listView.setDividerHeight(0);
        listView.setDivider(null);
        listView.setEmptyView(findViewById(R.id.empty_main));
    }

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
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch (id) {
            case R.id.action_settings:
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
                return true;
            case R.id.action_clear:
                onClearClick();
                return true;
            case R.id.action_sort_date:
                onSortClick(MainList.MAIN_SORT_DATE);
                return true;
            case R.id.action_sort_name:
                onSortClick(MainList.MAIN_SORT_NAME);
                return true;
            case R.id.action_sort_size:
                onSortClick(MainList.MAIN_SORT_SIZE);
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

    private static class MainHandler extends Handler {
        private final WeakReference<MainActivity> mTarget;

        MainHandler(MainActivity target) {
            mTarget = new WeakReference<>(target);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MainMessages.MSG_REFRESH_LIST:
                    mTarget.get().mainList.refesh();
                    break;
                case MainMessages.MSG_TEST_LIST:
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }
    private final MainHandler handler = new MainHandler(this);


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
            mainList.addImageToItem(path, false);
        }

        mainList.notifyChange();

        cursor.close();
        db.close();

    }
    private void saveList() {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();

        db.execSQL("delete from 'image_list'; ");
        for(MainList.MainListItem i : mainList.getMainListItems()) {
            if(i.getForceItemType() == MainList.MainListItem.ITEM_TYPE_NORMAL) {
                String sql = "insert into image_list(path) values('" + i.getFilePath() + "')";
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
            ArrayList<String> images = data.getStringArrayListExtra(ImageSelector.SELECT_RESULT);
            if(images!=null) {
                for (String path : images)
                    mainList.addImageToItem(path, false);
                mainList.sort();
                mainList.notifyChange();
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
        new CommonDialog(this)
                .setMessage(resources.getText(R.string.text_would_you_want_clear_list))
                .setNegative(resources.getText(R.string.action_cancel))
                .setPositive(resources.getText(R.string.action_sure_delete))
                .setOnClickBottomListener(new CommonDialog.OnClickBottomListener() {
                    @Override
                    public void onPositiveClick(CommonDialog dialog) {
                        mainList.clear();
                        dialog.dismiss();
                    }
                    @Override
                    public void onNegativeClick(CommonDialog dialog) {
                        dialog.dismiss();
                    }
                })
                .show();
    }
    private void onSortClick(int sortType){
        mainList.sort(sortType);
    }
    private void onOpenImageClick(String filePath) {
        Intent intent = new Intent(MainActivity.this, PanoActivity.class);
        intent.putExtra("filePath", filePath);
        startActivity(intent);
    }
    private void onDeleteImageClick() {
        final List<MainList.MainListItem> sel = mainList.getSelectedItems();
        if(sel.size() > 0){
            new CommonDialog(this)
                .setTitle(resources.getString(R.string.text_sure_delete_pano))
                .setMessage(String.format(resources.getString(R.string.text_youcan_remove_choosed_images), sel.size()))
                .setNegative(resources.getText(R.string.action_cancel))
                .setPositive(resources.getText(R.string.action_sure_delete))
                .setCheckText(resources.getText(R.string.text_also_delete_files))
                .setOnClickBottomListener(new CommonDialog.OnClickBottomListener() {
                    @Override
                    public void onPositiveClick(CommonDialog dialog) {
                        if(dialog.isCheckBoxChecked()) {
                        }
                        mainList.clear();
                        dialog.dismiss();
                    }
                    @Override
                    public void onNegativeClick(CommonDialog dialog) {
                        dialog.dismiss();
                    }
                })
                .show();
        }else Toast.makeText(this, R.string.text_choose_some_file_to_del, Toast.LENGTH_LONG).show();
    }
    private void onOpenImageWithImageClick() {
        final List<MainList.MainListItem> sel = mainList.getSelectedItems();
        if(sel.size() == 1){
            FileUtils.openFile(this, sel.get(0).getFilePath());
        }else Toast.makeText(this, resources.getText(R.string.text_choose_one_file_to_open), Toast.LENGTH_LONG).show();
    }

    //====================================================
    //其他事件
    //====================================================

    @Override
    public void onActionModeStarted(ActionMode mode) {
        mode.setType(ActionMode.TYPE_FLOATING);
        super.onActionModeStarted(mode);
    }
}
