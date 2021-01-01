package com.imengyu.vr720;

import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.navigation.NavigationView;
import com.hjq.toast.ToastUtils;
import com.imengyu.vr720.adapter.MyFragmentAdapter;
import com.imengyu.vr720.config.Codes;
import com.imengyu.vr720.config.MainMessages;
import com.imengyu.vr720.dialog.AppDialogs;
import com.imengyu.vr720.fragment.GalleryFragment;
import com.imengyu.vr720.fragment.HomeFragment;
import com.imengyu.vr720.fragment.IMainFragment;
import com.imengyu.vr720.model.TitleSelectionChangedCallback;
import com.imengyu.vr720.service.ListDataService;
import com.imengyu.vr720.utils.AlertDialogTool;
import com.imengyu.vr720.utils.StatusBarUtils;
import com.imengyu.vr720.widget.MyTitleBar;
import com.imengyu.vr720.widget.ToolbarButton;

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
        initList();
        initView();
    }
    @Override
    protected void onDestroy() {
        ((VR720Application)getApplication()).onQuit();
        super.onDestroy();
    }
    @Override
    protected void onPause() {
        listDataService.saveList();
        super.onPause();
    }

    //====================================================
    //控件
    //====================================================

    private Resources resources;

    private MyTitleBar toolbar;
    private DrawerLayout drawerLayout;

    public MyTitleBar getToolbar() {
        return toolbar;
    }

    private ViewPager mViewPager;
    private IMainFragment currentFragment;
    private final List<Fragment> fragments = new ArrayList<>();
    private boolean currentTitleIsSelectMode = false;

    private ListDataService listDataService = null;
    public ListDataService getListDataService() {
        return listDataService;
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
    private void initView() {

        mViewPager = findViewById(R.id.view_pager_main);

        List<String> sTitle = new ArrayList<>();
        sTitle.add("");
        sTitle.add("");

        //标题栏按钮

        ToolbarButton buttonTabHome = findViewById(R.id.button_tab_home);
        ToolbarButton buttonTabGallery = findViewById(R.id.button_tab_gallery);
        buttonTabHome.setChecked(true);
        buttonTabHome.setOnClickListener((v) -> mViewPager.setCurrentItem(0));
        buttonTabGallery.setOnClickListener((v) -> mViewPager.setCurrentItem(1));

        //两个Fragment
        HomeFragment homeFragment = new HomeFragment(handler, toolbar, listDataService);
        GalleryFragment galleryFragment = new GalleryFragment(handler, toolbar, listDataService);

        //两个Fragment的选择模式标题栏回调
        TitleSelectionChangedCallback titleSelectionChangedCallback = (isSelectionMode, selCount, isAll) -> {
            currentTitleIsSelectMode = isSelectionMode;

            if(isSelectionMode) {

                DrawableCompat.setTint(toolbar.getRightButton().getForeground(),
                        isAll ?
                                resources.getColor(R.color.colorPrimary, null) :
                                Color.BLACK);

                toolbar.setTitle(selCount > 0 ?
                        String.format(getString(R.string.text_choosed_items), selCount) :
                        resources.getString(R.string.text_please_choose_item));
                toolbar.setLeftButtonIconResource(R.drawable.ic_close);
                toolbar.setRightButtonIconResource(R.drawable.ic_check_all);
                toolbar.setCustomViewsVisible(View.GONE);
            }
            else {
                DrawableCompat.setTint(toolbar.getRightButton().getForeground(), Color.BLACK);
                toolbar.setTitle("");
                toolbar.setLeftButtonIconResource(R.drawable.ic_menu);
                toolbar.setRightButtonIconResource(R.drawable.ic_more);
                toolbar.setCustomViewsVisible(View.VISIBLE);
            }
        };

        homeFragment.setTitleSelectionChangedCallback(titleSelectionChangedCallback);
        galleryFragment.setTitleSelectionChangedCallback(titleSelectionChangedCallback);

        //添加
        fragments.add(homeFragment);
        fragments.add(galleryFragment);

        //初始
        currentFragment = homeFragment;

        //适配器
        MyFragmentAdapter adapter = new MyFragmentAdapter(getSupportFragmentManager(), fragments, sTitle);
        mViewPager.setAdapter(adapter);
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }
            @Override
            public void onPageSelected(int position) {

                //退出选择模式
                currentFragment.setTitleSelectionQuit();

                currentFragment = (IMainFragment) fragments.get(position);
                switch (position) {
                    case 0:
                        buttonTabGallery.setChecked(false);
                        buttonTabHome.setChecked(true);
                        break;
                    case 1:
                        buttonTabGallery.setChecked(true);
                        buttonTabHome.setChecked(false);
                        break;
                }
            }
            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        //标题栏事件
        toolbar.setLeftIconOnClickListener((v) -> {
            if(currentTitleIsSelectMode) {
                currentFragment.setTitleSelectionQuit();
            }else {
                if (drawerLayout.isDrawerOpen(GravityCompat.START))
                    drawerLayout.closeDrawer(GravityCompat.START);
                else
                    drawerLayout.openDrawer(GravityCompat.START);
            }
        });
        toolbar.setRightIconOnClickListener((v) -> {
            if(currentTitleIsSelectMode)
                currentFragment.setTitleSelectionCheckAllSwitch();
            else
                currentFragment.showMore();
        });

        //添加按钮到标题栏
        ((ViewGroup)buttonTabHome.getParent()).removeView(buttonTabHome);
        ((ViewGroup)buttonTabGallery.getParent()).removeView(buttonTabGallery);
        toolbar.addCustomView(buttonTabHome);
        toolbar.addCustomView(buttonTabGallery);
    }
    private void initList() {
        listDataService = ((VR720Application)getApplication()).getListDataService();
        listDataService.loadList();
    }

    //====================================================
    //返回键
    //====================================================

    // 用来计算返回键的点击间隔时间
    private long exitTime = 0;

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
            DrawerLayout drawer = findViewById(R.id.drawer_layout);
            //关闭 DrawerLayout
            if (drawer.isDrawerOpen(GravityCompat.START)) {
                drawer.closeDrawer(GravityCompat.START);
                return true;
            }

            //自定义处理
            if(currentFragment.onBackPressed())
                return true;

            if ((System.currentTimeMillis() - exitTime) > 2000) {
                ToastUtils.show(resources.getText(R.string.text_press_once_more_to_quit));
                exitTime = System.currentTimeMillis();
            } else {
                quit();
            }

            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    private void quit() {
        finish();
        //NativeVR720.releaseNative();
        //System.exit(0);
    }

    //====================================================
    //菜单
    //====================================================

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        if (id == R.id.nav_import) {
            handler.sendEmptyMessage(MainMessages.MSG_ADD_IMAGE);
        } else if (id == R.id.nav_manage) {
            AppDialogs.showSettings(this);
        } else if (id == R.id.nav_help) {
            AppDialogs.showHelp(this);
        } else if (id == R.id.nav_send) {
            AppDialogs.showFeedBack(this);
        } else if (id == R.id.nav_about) {
            AppDialogs.showAbout(this);
        } else if (id == R.id.nav_quit) {
            quit();
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
            super(Looper.myLooper());
            mTarget = new WeakReference<>(target);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            for(Fragment fragment : mTarget.get().fragments)
                ((IMainFragment)fragment).handleMessage(msg);
        }
    }
    private final MainHandler handler = new MainHandler(this);

    public MainHandler getHandler() {
        return handler;
    }

    //====================================================
    //Activity 返回值
    //====================================================

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == Codes.REQUEST_CODE_SETTING && data != null
                && data.getBooleanExtra("needRestart", false)) {
            Intent intent = new Intent(this, LunchActivity.class);
            intent.setAction(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            startActivity(intent);
            finish();
        }
        for(Fragment fragment : fragments)
            ((IMainFragment)fragment).onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onActionModeStarted(ActionMode mode) {
        super.onActionModeStarted(mode);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        AlertDialogTool.notifyConfigurationChangedForDialog(this);
        super.onConfigurationChanged(newConfig);
    }
}
