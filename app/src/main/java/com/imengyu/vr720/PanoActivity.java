package com.imengyu.vr720;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
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
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.exifinterface.media.ExifInterface;
import androidx.preference.PreferenceManager;

import com.hjq.toast.ToastUtils;
import com.imengyu.vr720.config.MainMessages;
import com.imengyu.vr720.core.NativeVR720;
import com.imengyu.vr720.core.NativeVR720GLSurfaceView;
import com.imengyu.vr720.core.NativeVR720Renderer;
import com.imengyu.vr720.core.sensor.ImprovedOrientationSensor1Provider;
import com.imengyu.vr720.dialog.CommonDialog;
import com.imengyu.vr720.dialog.LoadingDialog;
import com.imengyu.vr720.utils.DateUtils;
import com.imengyu.vr720.utils.FileSizeUtil;
import com.imengyu.vr720.utils.FileUtils;
import com.imengyu.vr720.utils.ImageUtils;
import com.imengyu.vr720.utils.StatusBarUtils;
import com.imengyu.vr720.core.representation.MatrixF4x4;
import com.imengyu.vr720.core.representation.Quaternion;
import com.imengyu.vr720.widget.MyTitleBar;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
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

    private View pano_bar;
    private View pano_error;
    private View pano_loading;

    private View pano_menu;
    private View pano_info;
    private View pano_tools;

    private TextView text_pano_error;
    private TextView text_pano_info_size;
    private TextView text_pano_info_file_size;
    private TextView text_pano_info_file_path;
    private TextView text_pano_info_image_time;
    private TextView text_pano_info_shutter_time;
    private TextView text_pano_info_exposure_bias_value;
    private TextView text_pano_info_iso_sensitivit;

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

    private boolean closeMarked = false;
    private boolean initialized = false;


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

        //加载图片基础信息
        loadImageInfo();
        //加载图片
        loadImage();
    }

    @Override
    protected void onDestroy() {
        unInitSensor();
        renderer.onDestroy();
        super.onDestroy();
    }

    private String imageSize = "";
    private String imageTime = "";
    private String imageFileSize = "";
    private String imageShutterTime = "0s";
    private String imageExposureBiasValue = "0";
    private String imageISOSensitivity = "0";

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
    private boolean lastTouchMoved = false;

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

        text_debug = findViewById(R.id.text_debug);
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

        text_pano_error = findViewById(R.id.text_pano_error);
        text_pano_info_size = findViewById(R.id.text_pano_info_size);
        text_pano_info_file_size = findViewById(R.id.text_pano_info_file_size);
        text_pano_info_file_path = findViewById(R.id.text_pano_info_file_path);
        text_pano_info_image_time = findViewById(R.id.text_pano_info_image_time);
        text_pano_info_shutter_time = findViewById(R.id.text_pano_info_shutter_time);
        text_pano_info_exposure_bias_value = findViewById(R.id.text_pano_info_exposure_bias_value);
        text_pano_info_iso_sensitivit = findViewById(R.id.text_pano_info_iso_sensitivit);

        text_pano_error = findViewById(R.id.text_pano_error);
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
    }
    private void initButtons() {

        SwitchCompat switch_enable_vr = findViewById(R.id.switch_enable_vr);
        SwitchCompat switch_enable_gyro = findViewById(R.id.switch_enable_gyro);
        button_mode = findViewById(R.id.button_mode);
        button_mode.setOnClickListener(v -> changeMode());

        switch_enable_gyro.setOnCheckedChangeListener((compoundButton, b) -> {
            gyroEnabled = compoundButton.isChecked();
            if(gyroEnabled) orientationSensor1Provider.start();
            else orientationSensor1Provider.stop();
            renderer.setGyroEnable(gyroEnabled);
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
        titlebar.setLeftIconOnClickListener(v -> onBackPressed());

        findViewById(R.id.button_short).setOnClickListener(v -> screenShot());
        findViewById(R.id.button_more).setOnClickListener(v -> showMore());
        findViewById(R.id.button_close_pano_info).setOnClickListener(v -> showTools());
        findViewById(R.id.action_view_file_info).setOnClickListener(v -> showFileInfo());
        findViewById(R.id.action_delete_file).setOnClickListener(v -> deleteFile());
        findViewById(R.id.action_openwith_text).setOnClickListener(v -> openWithOtherApp());
        findViewById(R.id.action_back_main).setOnClickListener(v -> closeImage());
        findViewById(R.id.button_back).setOnClickListener(v -> closeImage());
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

    //界面控制
    //**********************

    private int currentPanoMode = 0;
    private boolean panoEnableFull = false;
    private boolean vrEnabled = false;
    private boolean gyroEnabled = false;
    private boolean dragVelocityEnabled = true;

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
    }
    private void showTools() {
        pano_info.setVisibility(View.GONE);
        pano_menu.setVisibility(View.GONE);
        pano_tools.startAnimation(AnimationUtils.loadAnimation(PanoActivity.this, R.anim.bottom_up));
        pano_tools.setVisibility(View.VISIBLE);
    }
    private void showMore() {
        if(pano_menu.getVisibility() == View.GONE) {
            pano_menu.startAnimation(AnimationUtils.loadAnimation(PanoActivity.this, R.anim.bottom_up));
            pano_menu.setVisibility(View.VISIBLE);
        } else {
            pano_menu.startAnimation(AnimationUtils.loadAnimation(PanoActivity.this, R.anim.fade_hide));
            pano_menu.setVisibility(View.GONE);
        }
    }
    private void showFileInfo() {
        pano_tools.setVisibility(View.GONE);
        pano_menu.setVisibility(View.GONE);
        pano_info.startAnimation(AnimationUtils.loadAnimation(PanoActivity.this, R.anim.bottom_up));
        pano_info.setVisibility(View.VISIBLE);
    }
    private void deleteFile() {
        new CommonDialog(this)
            .setTitle2(getString(R.string.text_sure_delete_pano))
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
    private void loadFileInfo() {
        text_pano_info_size.setText(imageSize);
        text_pano_info_file_size.setText(imageFileSize);
        text_pano_info_image_time.setText(imageTime);
        text_pano_info_file_path.setText(filePath);
        text_pano_info_shutter_time.setText(imageShutterTime);
        text_pano_info_exposure_bias_value.setText(imageExposureBiasValue);
        text_pano_info_iso_sensitivit.setText(imageISOSensitivity);
    }
    private void showErr(String err) {
        pano_error.setVisibility(View.VISIBLE);
        pano_loading.setVisibility(View.GONE);
        text_pano_error.setText(err);
        disableToolBar();
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

    private LoadingDialog captureLoadingDialog = null;

    /**
     *  标题栏开启关闭与全屏
     */
    private boolean disableToolbar = false;
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
    private void disableToolBar() {
        pano_bar.setVisibility(View.GONE);
        disableToolbar = true;
    }
    private void switchToolBar(){
        if(!disableToolbar) {
            if(toolbarOn) {
                if(pano_menu.getVisibility() == View.VISIBLE) {
                    showTools();
                    return;
                }
                if(pano_info.getVisibility() == View.VISIBLE) {
                    showTools();
                    return;
                }
            }
            toolbarOn = !toolbarOn;
            //switchFullScreen(!toolbarOn);
            if (toolbarOn) {
                pano_bar.startAnimation(AnimationUtils.loadAnimation(PanoActivity.this, R.anim.bottom_up));
                titlebar.startAnimation(AnimationUtils.loadAnimation(PanoActivity.this, R.anim.top_down));
                pano_bar.setVisibility(View.VISIBLE);
                titlebar.setVisibility(View.VISIBLE);
            } else {
                pano_bar.startAnimation(AnimationUtils.loadAnimation(PanoActivity.this, R.anim.buttom_down));
                titlebar.startAnimation(AnimationUtils.loadAnimation(PanoActivity.this, R.anim.top_up));
                pano_bar.setVisibility(View.GONE);
                titlebar.setVisibility(View.GONE);
            }
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
                    mTarget.get().loadFileInfo();
                    break;
                case MainMessages.MSG_QUIT_LATE:
                    mTarget.get().onQuitLate();
                    break;
                case MainMessages.MSG_FORCE_PAUSE:
                    mTarget.get().onPause();
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }
    private final MainHandler handler = new MainHandler(this);

    //重载相关响应函数
    //**********************

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {
        super.onPointerCaptureChanged(hasCapture);
    }

    private void onRendererIsReady() {
        Log.d(TAG, "Renderer is ok");
        Log.d(TAG, "Load image file : " + filePath);

        renderer.setPanoramaMode(NativeVR720Renderer.PanoramaMode_PanoramaSphere);

        titlebar.setTitle(FileUtils.getFileName(filePath));
        titlebar.setVisibility(View.VISIBLE);
        pano_loading.setVisibility(View.GONE);
        pano_bar.setVisibility(View.VISIBLE);
        pano_tools.setVisibility(View.VISIBLE);
    }
    private void onRendererMessage(int msg) {
        switch (msg) {
            case NativeVR720Renderer.MobileGameUIEvent_FileClosed:
                Log.i(TAG, "FileClosed");
                disableToolBar();
                if(closeMarked) {
                    Log.i(TAG, "renderer.destroy()");
                    renderer.destroy();
                }
                break;
            case NativeVR720Renderer.MobileGameUIEvent_MarkLoadFailed:
                titlebar.setVisibility(View.VISIBLE);
                showErr(renderer.getLastError());
                break;
            case NativeVR720Renderer.MobileGameUIEvent_MarkLoadingEnd:
                onRendererIsReady();
                disableToolbar = false;
                break;
            case NativeVR720Renderer.MobileGameUIEvent_MarkLoadingStart:
                pano_loading.setVisibility(View.VISIBLE);
                pano_menu.setVisibility(View.GONE);
                disableToolBar();
                break;
            case NativeVR720Renderer.MobileGameUIEvent_UiInfoChanged:
                break;
            case NativeVR720Renderer.MobileGameUIEvent_DestroyComplete:
                Log.i(TAG, "DestroyComplete");
                handler.sendEmptyMessage(MainMessages.MSG_FORCE_PAUSE);
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        handler.sendEmptyMessage(MainMessages.MSG_QUIT_LATE);
                    }
                },  300);
                break;
        }
    }

    private void onQuitLate() {
        Intent intent = new Intent();
        intent.putExtra("filePath", filePath);
        setResult(0, intent);
        finish();
    }

    @Override
    protected void onPause() {

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
        super.onResume();

        if(initialized && !closeMarked) {
            if(gyroEnabled)
                orientationSensor1Provider.start();
            startUpdateThread();
            glSurfaceView.onResume();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public void onBackPressed() {
        closeImage();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        //glSurfaceView.getLayoutParams().width = initialWidth;
        //glSurfaceView.getLayoutParams().height = initialHeight;
    }

    //主图片处理逻辑
    //**********************

    private void loadImageInfo() {
        new Thread(() -> {

            imageFileSize = FileSizeUtil.getAutoFileOrFilesSize(filePath);
            imageTime = DateUtils.format(new Date((new File(filePath)).lastModified()));

            try {
                ExifInterface exifInterface = new ExifInterface(filePath);
                imageTime = exifInterface.getAttribute(ExifInterface.TAG_DATETIME);
                imageSize = exifInterface.getAttribute(ExifInterface.TAG_IMAGE_WIDTH) +
                        "x" + exifInterface.getAttribute(ExifInterface.TAG_IMAGE_LENGTH);
                imageISOSensitivity = exifInterface.getAttribute(ExifInterface.TAG_RW2_ISO);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    imageShutterTime = exifInterface.getAttribute(ExifInterface.TAG_EXPOSURE_TIME);
                    imageExposureBiasValue = exifInterface.getAttribute(ExifInterface.TAG_EXPOSURE_BIAS_VALUE);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }

            handler.sendEmptyMessage(MainMessages.MSG_LOAD_IMAGE_INFO);
        }).start();
    }
    private void loadImage() { renderer.openFile(filePath); }
    private void closeImage() {
        if(!closeMarked) {
            closeMarked = true;
            renderer.closeFile();
        }
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
