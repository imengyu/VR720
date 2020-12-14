package com.imengyu.vr720;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.exifinterface.media.ExifInterface;
import androidx.preference.PreferenceManager;

import com.donkingliang.imageselector.utils.StringUtils;
import com.hjq.toast.ToastUtils;
import com.imengyu.vr720.config.MainMessages;
import com.imengyu.vr720.core.NativeVR720;
import com.imengyu.vr720.core.NativeVR720GLSurfaceView;
import com.imengyu.vr720.core.NativeVR720Renderer;
import com.imengyu.vr720.core.sensor.ImprovedOrientationSensor1Provider;
import com.imengyu.vr720.dialog.CommonDialog;
import com.imengyu.vr720.dialog.LoadingDialog;
import com.imengyu.vr720.model.ImageInfo;
import com.imengyu.vr720.utils.DateUtils;
import com.imengyu.vr720.utils.FileSizeUtil;
import com.imengyu.vr720.utils.FileUtils;
import com.imengyu.vr720.utils.ImageUtils;
import com.imengyu.vr720.utils.StatusBarUtils;
import com.imengyu.vr720.widget.MyTitleBar;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PanoActivity extends AppCompatActivity {

    /** Tag for logging. */
    public static final String TAG = PanoActivity.class.getSimpleName();

    private Context mContext;
    private Resources resources;

    private String filePath;
    private ArrayList<CharSequence> fileList;
    private boolean fileLoading = false;
    private boolean fileLoaded = false;

    private int findIndexInPathList(String path) {
        for(int i = 0; i < fileList.size(); i++) {
            if(path.contentEquals(fileList.get(i)))
                return i;
        }
        return -1;
    }

    private View pano_bar;
    private View pano_error;
    private View pano_loading;
    private View pano_menu;
    private View pano_info;
    private View pano_tools;
    private View layout_debug;
    private TextView text_pano_error;
    private TextView text_pano_info_size;
    private TextView text_pano_info_file_size;
    private TextView text_pano_info_file_path;
    private TextView text_pano_info_image_time;
    private TextView text_pano_info_shutter_time;
    private TextView text_pano_info_exposure_bias_value;
    private TextView text_pano_info_iso_sensitivit;
    private TextView text_sensor_cant_use_in_mode;
    private ImageButton button_prev;
    private ImageButton button_next;
    private SeekBar seek_x;
    private SeekBar seek_y;
    private SeekBar seek_z;
    private TextView text_debug;
    private MyTitleBar titlebar;
    private Button button_mode;
    private NativeVR720GLSurfaceView glSurfaceView;

    private Drawable iconModeBall;
    private Drawable iconModeLittlePlanet;
    private Drawable iconModeRectiliner;
    private Drawable iconModeSource;
    private Drawable iconModeMercator;
    private Drawable iconModeVideoBall;

    private Animation bottom_up;
    private Animation bottom_down;
    private Animation fade_hide;
    private Animation fade_show;
    private Animation top_down;
    private Animation top_up;

    private String openNextPath;
    private boolean openNextMarked = false;
    private boolean closeMarked = false;
    private boolean initialized = false;
    private boolean lastTouchMoved = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        //设置全屏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_pano);

        StatusBarUtils.setDarkMode(this);
        StatusBarUtils.setStatusBarColor(this, getColor(R.color.colorPrimary));

        resources = getResources();
        mContext = getApplicationContext();
        filePath = getIntent().getStringExtra("filePath");
        fileList = getIntent().getCharSequenceArrayListExtra("fileList");

        initAnimations();
        initResources();
        initSettings();
        initSensor();

        NativeVR720.updateAssetManagerPtr(getAssets());
        //初始化内核
        if(!NativeVR720Renderer.checkSupportsEs3(this)) {
            showErr(getString(R.string.text_your_device_dosnot_support_es20));
            return;
        }

        initRenderer();
        initControls();
        initButtons();

        initialized = true;
        onResume();

        //加载图片
        loadImage(filePath);
    }

    @Override
    protected void onDestroy() {
        unInitSensor();
        glSurfaceView.setCaptureCallback(null);
        renderer.onDestroy();
        super.onDestroy();
    }

    private NativeVR720Renderer renderer;
    /**
     * 更新任务
     */
    private final TimerTask task = new TimerTask() {
        @Override
        public void run() {
            renderer.onMainThread();
        }
    };
    private ScheduledExecutorService pool = null;

    private void initRenderer() {

        renderer = new NativeVR720Renderer(handler);
        renderer.setEnableFullChunks(panoEnableFull);
        renderer.setOnRequestGyroValueCallback(quaternion -> {
            orientationSensor1Provider.getQuaternion(quaternion);
        });
        glSurfaceView = findViewById(R.id.pano_view);
        glSurfaceView.setNativeRenderer(renderer);
        glSurfaceView.setCaptureCallback(this::screenShotCallback);
        glSurfaceView.setDragVelocityEnabled(dragVelocityEnabled);

        startUpdateThread();
    }
    private void initControls() {
        titlebar = findViewById(R.id.titlebar);
        titlebar.setLeftIconOnClickListener((v) -> onBackPressed());
        titlebar.setRightIconOnClickListener((v) -> shareThisImage());

        layout_debug = findViewById(R.id.layout_debug);
        text_debug = findViewById(R.id.text_debug);
        if(!debugEnabled)
            text_debug.setVisibility(View.GONE);
        pano_bar = findViewById(R.id.pano_toolbar_view);
        pano_error = findViewById(R.id.pano_error_view);
        pano_error.setVisibility(View.GONE);
        pano_loading = findViewById(R.id.pano_loading_view);
        pano_menu = findViewById(R.id.pano_menu);
        pano_info = findViewById(R.id.pano_info);
        pano_tools = findViewById(R.id.pano_tools);
        pano_info.setVisibility(View.GONE);
        pano_menu.setVisibility(View.GONE);
        pano_tools.setVisibility(View.VISIBLE);

        text_sensor_cant_use_in_mode = findViewById(R.id.text_sensor_cant_use_in_mode);
        text_sensor_cant_use_in_mode.setVisibility(View.GONE);
        text_pano_error = findViewById(R.id.text_pano_error);
        text_pano_info_size = findViewById(R.id.text_pano_info_size);
        text_pano_info_file_size = findViewById(R.id.text_pano_info_file_size);
        text_pano_info_file_path = findViewById(R.id.text_pano_info_file_path);
        text_pano_info_image_time = findViewById(R.id.text_pano_info_image_time);
        text_pano_info_shutter_time = findViewById(R.id.text_pano_info_shutter_time);
        text_pano_info_exposure_bias_value = findViewById(R.id.text_pano_info_exposure_bias_value);
        text_pano_info_iso_sensitivit = findViewById(R.id.text_pano_info_iso_sensitivit);

        text_pano_error = findViewById(R.id.text_pano_error);

        if(!debugEnabled)
            layout_debug.setVisibility(View.GONE);
    }
    private void initAnimations() {
        bottom_up = AnimationUtils.loadAnimation(PanoActivity.this, R.anim.bottom_up);
        bottom_down = AnimationUtils.loadAnimation(PanoActivity.this, R.anim.bottom_down);
        fade_hide = AnimationUtils.loadAnimation(PanoActivity.this, R.anim.fade_hide);
        fade_show = AnimationUtils.loadAnimation(PanoActivity.this, R.anim.fade_show);
        top_down = AnimationUtils.loadAnimation(PanoActivity.this, R.anim.top_down);
        top_up = AnimationUtils.loadAnimation(PanoActivity.this, R.anim.top_up);
    }
    private void initResources() {
        //加载图标
        Resources resources = mContext.getResources();
        iconModeBall = ResourcesCompat.getDrawable(resources, R.drawable.ic_bar_projection_ball, null);
        iconModeLittlePlanet = ResourcesCompat.getDrawable(resources, R.drawable.ic_bar_projection_little_planet, null);
        iconModeRectiliner = ResourcesCompat.getDrawable(resources, R.drawable.ic_bar_projection_rectilinear, null);
        iconModeSource = ResourcesCompat.getDrawable(resources, R.drawable.ic_bar_projection_source, null);
        iconModeVideoBall = ResourcesCompat.getDrawable(resources, R.drawable.ic_bar_projection_video_ball, null);
        iconModeMercator = ResourcesCompat.getDrawable(resources, R.drawable.ic_bar_projection_mercator, null);

        iconModeBall.setBounds(0,0, iconModeBall.getMinimumWidth(), iconModeBall.getMinimumHeight());
        iconModeLittlePlanet.setBounds(0,0, iconModeLittlePlanet.getMinimumWidth(), iconModeLittlePlanet.getMinimumHeight());
        iconModeRectiliner.setBounds(0,0, iconModeRectiliner.getMinimumWidth(), iconModeRectiliner.getMinimumHeight());
        iconModeSource.setBounds(0,0, iconModeSource.getMinimumWidth(), iconModeSource.getMinimumHeight());
        iconModeVideoBall.setBounds(0,0,iconModeVideoBall.getMinimumWidth(), iconModeVideoBall.getMinimumHeight());
        iconModeMercator.setBounds(0,0, iconModeMercator.getMinimumWidth(), iconModeMercator.getMinimumHeight());
    }
    private void initSettings() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        panoEnableFull = sharedPreferences.getBoolean("enable_full_chunks", false);
        currentPanoMode = sharedPreferences.getInt("pano_mode", 0);
        vrEnabled = sharedPreferences.getBoolean("pano_enable_vr", false);
        gyroEnabled = sharedPreferences.getBoolean("pano_enable_gyro", false);
        dragVelocityEnabled = sharedPreferences.getBoolean("enable_drag_velocity", true);
        debugEnabled = sharedPreferences.getBoolean("show_debug_tool", false);
        if(sharedPreferences.getBoolean("enable_non_fullscreen", false))
            switchFullScreen(false);
    }
    private void initButtons() {

        TextView text_not_support_sensor = findViewById(R.id.text_not_support_sensor);
        SwitchCompat switch_enable_vr = findViewById(R.id.switch_enable_vr);
        SwitchCompat switch_enable_gyro = findViewById(R.id.switch_enable_gyro);

        //判断陀螺仪是否可用，否则禁用界面
        if (!orientationSensor1Provider.isDeviceSupport()) {
            text_not_support_sensor.setVisibility(View.VISIBLE);
            switch_enable_gyro.setChecked(false);
            switch_enable_gyro.setEnabled(false);
            gyroEnabled = false;
        } else {
            text_not_support_sensor.setVisibility(View.GONE);
        }

        button_mode = findViewById(R.id.button_mode);
        button_mode.setOnClickListener(v -> changeMode());

        switch_enable_gyro.setOnCheckedChangeListener((compoundButton, b) -> {
            gyroEnabled = compoundButton.isChecked();
            if(gyroEnabled) orientationSensor1Provider.start();
            else orientationSensor1Provider.stop();
            renderer.setGyroEnable(gyroEnabled);

            updateModeText();
        });
        switch_enable_vr.setOnCheckedChangeListener((compoundButton, b) -> {
            vrEnabled = compoundButton.isChecked();
            renderer.setVREnable(vrEnabled);
        });
        switch_enable_gyro.setChecked(gyroEnabled);
        switch_enable_vr.setChecked(vrEnabled);

        renderer.setVREnable(vrEnabled);
        renderer.setGyroEnable(gyroEnabled);

        //视图点击事件
        glSurfaceView.setOnTouchListener((view, motionEvent) -> {
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_MOVE:
                    lastTouchMoved = true;
                    break;
                case MotionEvent.ACTION_DOWN:
                    lastTouchMoved = false;
                    if(pano_menu.getVisibility() == View.VISIBLE) showMore();
                    if(pano_info.getVisibility() == View.VISIBLE) pano_info.setVisibility(View.GONE);
                    break;
                case MotionEvent.ACTION_UP:
                    if(!lastTouchMoved) {
                        switchToolBar();
                        view.performClick();
                    }
                    break;
            }
            return false;
        });

        button_prev = findViewById(R.id.button_prev);
        button_next = findViewById(R.id.button_next);

        button_next.setOnClickListener((v) -> nextImage());
        button_prev.setOnClickListener((v) -> prevImage());

        seek_x = findViewById(R.id.seek_x);
        seek_y = findViewById(R.id.seek_y);
        seek_z = findViewById(R.id.seek_z);

        titlebar.setLeftIconOnClickListener(v -> onBackPressed());

        findViewById(R.id.button_short).setOnClickListener(v -> {
            if(!fileLoading && fileLoaded)
                screenShot();
        });
        findViewById(R.id.button_more).setOnClickListener(v -> {
            if(!fileLoading && fileLoaded)
                showMore();
        });
        findViewById(R.id.button_close_pano_info).setOnClickListener(v -> {
            if(!fileLoading && fileLoaded)
                showTools();
        });

        findViewById(R.id.action_view_file_info).setOnClickListener(v -> showFileInfo());
        findViewById(R.id.action_delete_file).setOnClickListener(v -> deleteFile());
        findViewById(R.id.action_openwith_text).setOnClickListener(v -> openWithOtherApp());
        findViewById(R.id.action_back_main).setOnClickListener(v -> closeImage(true));
        findViewById(R.id.button_back).setOnClickListener(v -> closeImage(true));

        SeekBar.OnSeekBarChangeListener listener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                updateDebugSeek();
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        };

        seek_x.setOnSeekBarChangeListener(listener);
        seek_y.setOnSeekBarChangeListener(listener);
        seek_z.setOnSeekBarChangeListener(listener);
    }
    private void saveSettings() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putInt("pano_mode", currentPanoMode);
        editor.putBoolean("pano_enable_vr", vrEnabled);
        editor.putBoolean("pano_enable_gyro", gyroEnabled);

        editor.apply();
    }
    private void startUpdateThread() {
        if(pool == null) {
            pool = Executors.newScheduledThreadPool(1);
            pool.scheduleAtFixedRate(task, 0, 150, TimeUnit.MILLISECONDS);
        }
    }
    private void stopUpdateThread() {
        if(pool != null) {
            pool.shutdown();
            try {
                if(!pool.awaitTermination(1000, TimeUnit.MILLISECONDS))
                    pool.shutdownNow();
            } catch (InterruptedException e) {
                e.printStackTrace();
                pool.shutdownNow();
            }
            pool = null;
        }
    }
    private void updateDebugSeek() {
        renderer.updateDebugValue(
                (seek_x.getProgress() / 100.0f) * 360.0f,
                (seek_y.getProgress() / 100.0f) * 360.0f,
                (seek_z.getProgress() / 100.0f) * 360.0f,
                0,0,0);
    }
    private void updateDebugValue() {
    }

    //界面控制
    //**********************

    private int currentPanoMode = 0;
    private boolean panoEnableFull = false;
    private boolean vrEnabled = false;
    private boolean gyroEnabled = false;
    private boolean dragVelocityEnabled = true;
    private boolean debugEnabled = true;

    private void changeMode() {
        if(currentPanoMode < NativeVR720Renderer.PanoramaMode_PanoramaModeMax - 1)
            currentPanoMode++;
        else
            currentPanoMode = NativeVR720Renderer.PanoramaMode_PanoramaSphere;
        renderer.setPanoramaMode(currentPanoMode);
        updateModeButton();
    }
    private void updateModeButton() {
        switch (currentPanoMode) {
            case NativeVR720Renderer.PanoramaMode_PanoramaSphere:
                button_mode.setText(getString(R.string.text_mode_ball));
                button_mode.setCompoundDrawables(null, iconModeBall, null, null);
                break;
            case NativeVR720Renderer.PanoramaMode_PanoramaCylinder:
                button_mode.setText(getString(R.string.text_mode_rectilinear));
                button_mode.setCompoundDrawables(null, iconModeBall, null, null);
                break;
            case NativeVR720Renderer.PanoramaMode_PanoramaAsteroid:
                button_mode.setText(getString(R.string.text_mode_little_planet));
                button_mode.setCompoundDrawables(null, iconModeLittlePlanet, null, null);
                break;
            case NativeVR720Renderer.PanoramaMode_PanoramaOuterBall:
                button_mode.setText(getString(R.string.text_mode_video_ball));
                button_mode.setCompoundDrawables(null, iconModeVideoBall, null, null);
                break;
            case NativeVR720Renderer.PanoramaMode_PanoramaFull360:
                button_mode.setText(getString(R.string.text_mode_360_pano));
                button_mode.setCompoundDrawables(null, iconModeRectiliner, null, null);
                break;
            case NativeVR720Renderer.PanoramaMode_PanoramaFullOrginal:
                button_mode.setText(getString(R.string.text_mode_source));
                button_mode.setCompoundDrawables(null, iconModeSource, null, null);
                break;
            case NativeVR720Renderer.PanoramaMode_PanoramaMercator:
                button_mode.setText(getString(R.string.text_mode_mercator));
                button_mode.setCompoundDrawables(null, iconModeMercator, null, null);
                break;
        }
        updateModeText();
    }
    private void updateModeText() {
        if(gyroEnabled) {
            switch (currentPanoMode) {
                case NativeVR720Renderer.PanoramaMode_PanoramaSphere:
                    text_sensor_cant_use_in_mode.setVisibility(View.GONE);
                    break;
                case NativeVR720Renderer.PanoramaMode_PanoramaCylinder:
                    text_sensor_cant_use_in_mode.setVisibility(View.GONE);
                    break;
                case NativeVR720Renderer.PanoramaMode_PanoramaAsteroid:
                    text_sensor_cant_use_in_mode.setVisibility(View.GONE);
                    break;
                case NativeVR720Renderer.PanoramaMode_PanoramaOuterBall:
                    text_sensor_cant_use_in_mode.setVisibility(View.GONE);
                    break;
                case NativeVR720Renderer.PanoramaMode_PanoramaFull360:
                    text_sensor_cant_use_in_mode.setVisibility(View.VISIBLE);
                    break;
                case NativeVR720Renderer.PanoramaMode_PanoramaFullOrginal:
                    text_sensor_cant_use_in_mode.setVisibility(View.VISIBLE);
                    break;
                case NativeVR720Renderer.PanoramaMode_PanoramaMercator:
                    text_sensor_cant_use_in_mode.setVisibility(View.VISIBLE);
                    break;
            }
        }else {
            text_sensor_cant_use_in_mode.setVisibility(View.GONE);
        }
    }
    private void showTools() {
        pano_info.setVisibility(View.GONE);
        pano_menu.setVisibility(View.GONE);
        pano_tools.startAnimation(bottom_up);
        pano_tools.setVisibility(View.VISIBLE);
    }
    private void showMore() {
        if(pano_menu.getVisibility() == View.GONE) {
            pano_menu.startAnimation(bottom_up);
            pano_menu.setVisibility(View.VISIBLE);
        } else {
            pano_menu.startAnimation(fade_hide);
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    runOnUiThread(() -> {
                        pano_menu.setVisibility(View.GONE);
                    });
                }
            }, 200);

        }
    }
    private void showFileInfo() {
        pano_tools.setVisibility(View.GONE);
        pano_menu.setVisibility(View.GONE);
        pano_info.startAnimation(bottom_up);
        pano_info.setVisibility(View.VISIBLE);
    }
    private void deleteFile() {
        new CommonDialog(this)
            .setTitle2(getString(R.string.text_sure_delete_this_image))
            .setMessage(filePath)
            .setNegative(resources.getString(R.string.action_cancel))
            .setPositive(resources.getString(R.string.action_sure_delete))
            .setOnClickBottomListener(new CommonDialog.OnClickBottomListener() {
                @Override
                public void onPositiveClick(CommonDialog dialog) {
                    if(FileUtils.deleteFile(filePath)) {
                        ToastUtils.show(getString(R.string.text_delete_success));
                        Intent intent = new Intent();
                        intent.putExtra("isDeleteFile", true);
                        intent.putExtra("filePath", filePath);
                        setResult(0, intent);
                        finish();
                    }else
                        ToastUtils.show(getString(R.string.text_delete_failed));

                    dialog.dismiss();
                }
                @Override
                public void onNegativeClick(CommonDialog dialog) {
                    dialog.dismiss();
                }
            })
            .show();
    }
    private void openWithOtherApp() {
        FileUtils.openFile(this, filePath);
    }
    private void shareThisImage() {
        FileUtils.shareFile(this, filePath);
    }
    private void showErr(String err) {
        pano_error.setVisibility(View.VISIBLE);
        pano_loading.setVisibility(View.GONE);
        text_pano_error.setText(err);
    }
    private void screenShot() {
        captureLoadingDialog = new LoadingDialog(this);
        captureLoadingDialog.show();
        glSurfaceView.startCapture();
    }
    private void screenShotCallback(Bitmap b) {
        //保存图像
        ImageUtils.SaveImageResult result = ImageUtils.saveImageToStorageWithAutoName(b);
        //显示
        runOnUiThread(() -> {
            captureLoadingDialog.cancel();
            captureLoadingDialog = null;

            if(result.success)
                ToastUtils.show(String.format(getString(R.string.text_image_save_success), result.path));
            else
                ToastUtils.show(String.format(getString(R.string.text_image_save_failed), result.error));
        });
    }
    private void updatePrevNextBtnState() {
        int currentIndex = findIndexInPathList(filePath);
        currentCanNext = currentIndex < fileList.size() - 1;
        currentCanPrev = currentIndex > 0;
        button_next.setVisibility(currentCanNext ? View.VISIBLE : View.GONE);
        button_prev.setVisibility(currentCanPrev ? View.VISIBLE : View.GONE);
    }

    private boolean currentCanNext = false;
    private boolean currentCanPrev = false;
    private LoadingDialog captureLoadingDialog = null;

    /*
     *  标题栏开启关闭与全屏
     */

    private boolean toolbarOn = true;
    private void switchFullScreen(boolean full){
        if(full){
            WindowManager.LayoutParams attrs = getWindow().getAttributes();
            attrs.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
            getWindow().setAttributes(attrs);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        }else{
            WindowManager.LayoutParams attrs = getWindow().getAttributes();
            attrs.flags &= (~WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().setAttributes(attrs);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        }
    }
    private void switchToolBar() {
        if(toolbarOn && pano_tools.getVisibility() != View.VISIBLE) {
            pano_menu.setVisibility(View.GONE);
            pano_info.setVisibility(View.GONE);
            showTools();
        }
        toolbarOn = !toolbarOn;
        if (toolbarOn) {
            pano_bar.startAnimation(bottom_up);
            titlebar.startAnimation(top_down);
            button_next.startAnimation(fade_show);
            button_prev.startAnimation(fade_show);

            pano_bar.setVisibility(View.VISIBLE);
            titlebar.setVisibility(View.VISIBLE);

            if(currentCanNext)
                button_next.setVisibility(View.VISIBLE);
            if(currentCanPrev)
                button_prev.setVisibility(View.VISIBLE);

        } else {
            pano_bar.startAnimation(bottom_down);
            titlebar.startAnimation(top_up);
            button_next.startAnimation(fade_hide);
            button_prev.startAnimation(fade_hide);

            pano_bar.setVisibility(View.GONE);
            titlebar.setVisibility(View.GONE);
            if(currentCanNext)
                button_next.setVisibility(View.GONE);
            if(currentCanPrev)
                button_prev.setVisibility(View.GONE);
        }
    }

    /**
     * 主线程handler
     */
    private static class MainHandler extends Handler {
        private final WeakReference<PanoActivity> mTarget;

        MainHandler(PanoActivity target) {
            mTarget = new WeakReference<>(target);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MainMessages.MSG_NATIVE_MESSAGE:
                    mTarget.get().onRendererMessage((int)msg.obj);
                    break;
                case MainMessages.MSG_LOAD_IMAGE_INFO:
                    mTarget.get().loadImageInfoToUI();
                    break;
                case MainMessages.MSG_QUIT_LATE:
                    mTarget.get().onQuitLate();
                    break;
                case MainMessages.MSG_FORCE_PAUSE:
                    mTarget.get().onPause();
                    break;
                case MainMessages.MSG_TEST_VAL:
                    mTarget.get().updateDebugValue();
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }
    private final MainHandler handler = new MainHandler(this);

    //重载相关响应函数
    //**********************

    private void onRendererIsReady() {
        Log.d(TAG, "Renderer is ok");
        Log.d(TAG, "Load image file : " + filePath);

        //设置上次模式
        updateModeButton();
        renderer.setPanoramaMode(currentPanoMode);

        titlebar.setTitle(FileUtils.getFileName(filePath));
        titlebar.setVisibility(View.VISIBLE);
        pano_loading.setVisibility(View.GONE);
        pano_bar.setVisibility(View.VISIBLE);
        pano_tools.setVisibility(View.VISIBLE);
    }
    private void onRendererMessage(int msg) {
        switch (msg) {
            case NativeVR720Renderer.MobileGameUIEvent_FileClosed: {
                Log.i(TAG, "FileClosed");

                fileLoaded = false;
                if (closeMarked) {
                    Log.i(TAG, "Destroy renderer at FileClosed");
                    renderer.destroy();
                }
                if (openNextMarked) {
                    openNextMarked = false;
                    loadImage(openNextPath);
                    openNextPath = null;
                }
                break;
            }
            case NativeVR720Renderer.MobileGameUIEvent_MarkLoadFailed: {
                fileLoading = false;
                titlebar.setVisibility(View.VISIBLE);
                String error = renderer.getLastError();
                Log.w(TAG, String.format("Image load failed : %s", error));
                showErr(error);
                break;
            }
            case NativeVR720Renderer.MobileGameUIEvent_MarkLoadingEnd: {
                fileLoading = false;
                fileLoaded = true;
                onRendererIsReady();
                break;
            }
            case NativeVR720Renderer.MobileGameUIEvent_MarkLoadingStart: {
                fileLoading = true;
                fileLoaded = false;
                titlebar.setTitle(getString(R.string.text_loading_wait));
                pano_loading.setVisibility(View.VISIBLE);
                pano_menu.setVisibility(View.GONE);
                pano_info.setVisibility(View.GONE);
                pano_error.setVisibility(View.GONE);
                break;
            }
            case NativeVR720Renderer.MobileGameUIEvent_UiInfoChanged: break;
            case NativeVR720Renderer.MobileGameUIEvent_DestroyComplete: {
                Log.i(TAG, "DestroyComplete");

                fileLoaded = false;
                fileLoading = false;
                onPause();
                glSurfaceView.onDestroyComplete();
                handler.sendEmptyMessageDelayed(MainMessages.MSG_QUIT_LATE, 200);
            }
        }
    }
    private void onQuitLate() {

        Log.i(TAG, "onQuitLate");

        Intent intent = new Intent();
        intent.putExtra("filePath", filePath);
        setResult(0, intent);
        finish();
    }

    @Override
    protected void onPause() {

        Log.i(TAG, "onPause");

        if(captureLoadingDialog != null) {
            captureLoadingDialog.cancel();
            captureLoadingDialog = null;
        }

        saveSettings();
        stopUpdateThread();

        if(gyroEnabled)
            orientationSensor1Provider.stop();

        glSurfaceView.onPause();

        super.onPause();
    }
    @Override
    protected void onResume() {

        Log.i(TAG, "onResume");

        if(initialized && !closeMarked) {
            if(gyroEnabled)
                orientationSensor1Provider.start();

            glSurfaceView.onResume();

            startUpdateThread();
        }

        super.onResume();
    }
    @Override
    public void onBackPressed() {
        if(!closeMarked) closeImage(true);
        else super.onBackPressed();
    }

    //主图片处理逻辑
    //**********************

    private final ImageInfo imageInfo = new ImageInfo();

    private void loadImageInfo() {
        final String text_un_know = getResources().getString(R.string.text_un_know);
        new Thread(() -> {

            imageInfo.imageFileSize = FileSizeUtil.getAutoFileOrFilesSize(filePath);
            imageInfo.imageTime = DateUtils.format(new Date((new File(filePath)).lastModified()));

            try {
                ExifInterface exifInterface = new ExifInterface(filePath);
                imageInfo.imageTime = exifInterface.getAttribute(ExifInterface.TAG_DATETIME);
                imageInfo.imageSize = exifInterface.getAttribute(ExifInterface.TAG_IMAGE_WIDTH) +
                        "x" + exifInterface.getAttribute(ExifInterface.TAG_IMAGE_LENGTH);
                imageInfo.imageISOSensitivity = exifInterface.getAttribute(ExifInterface.TAG_RW2_ISO);

                if(StringUtils.isEmptyString(imageInfo.imageISOSensitivity))
                    imageInfo.imageISOSensitivity = text_un_know;

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    imageInfo.imageShutterTime = exifInterface.getAttribute(ExifInterface.TAG_EXPOSURE_TIME);
                    imageInfo.imageExposureBiasValue = exifInterface.getAttribute(ExifInterface.TAG_EXPOSURE_BIAS_VALUE);
                }else {
                    imageInfo.imageShutterTime = text_un_know;
                    imageInfo.imageExposureBiasValue = text_un_know;
                }

            } catch (IOException e) {
                e.printStackTrace();
            }

            handler.sendEmptyMessage(MainMessages.MSG_LOAD_IMAGE_INFO);
        }).start();
    }
    private void loadImageInfoToUI() {
        text_pano_info_size.setText(imageInfo.imageSize);
        text_pano_info_file_size.setText(imageInfo.imageFileSize);
        text_pano_info_image_time.setText(imageInfo.imageTime);
        text_pano_info_file_path.setText(filePath);
        text_pano_info_shutter_time.setText(imageInfo.imageShutterTime);
        text_pano_info_exposure_bias_value.setText(imageInfo.imageExposureBiasValue);
        text_pano_info_iso_sensitivit.setText(imageInfo.imageISOSensitivity);
    }
    private void loadImage(String path) {

        //如果已经打开文件，那么先关闭
        if(renderer.isFileOpen()) {
            openNextPath = path;
            openNextMarked = true;
            closeImage(false);
            return;
        }

        filePath = path;

        //刷新前后按钮状态
        updatePrevNextBtnState();

        //加载
        renderer.openFile(path);
        //加载图片基础信息
        loadImageInfo();
    }
    private void closeImage(boolean quit) {
        closeMarked = quit;
        renderer.closeFile();
    }
    private void nextImage() {
        if(fileLoading)
            return;
        int currentIndex = findIndexInPathList(filePath);
        if(currentIndex >= fileList.size() - 1) {
            ToastUtils.show(getString(R.string.text_this_is_last_image));
        } else {
            loadImage(fileList.get(currentIndex + 1).toString());
        }
    }
    private void prevImage() {
        if(fileLoading)
            return;
        int currentIndex = findIndexInPathList(filePath);
        if(currentIndex == 0) {
            ToastUtils.show(getString(R.string.text_this_is_first_image));
            return;
        }
        loadImage(fileList.get(currentIndex - 1).toString());
    }

    //陀螺仪控制
    //**********************

    private ImprovedOrientationSensor1Provider orientationSensor1Provider = null;

    private void initSensor() {
        orientationSensor1Provider = new ImprovedOrientationSensor1Provider(
                (SensorManager) getSystemService(Activity.SENSOR_SERVICE));
    }
    private void unInitSensor() {
        orientationSensor1Provider.stop();
    }
}
