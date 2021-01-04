package com.imengyu.vr720;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.hardware.SensorManager;
import android.media.MediaMetadataRetriever;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.util.Size;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.widget.TextViewCompat;
import androidx.exifinterface.media.ExifInterface;
import androidx.preference.PreferenceManager;

import com.hjq.toast.ToastUtils;
import com.imengyu.vr720.config.MainMessages;
import com.imengyu.vr720.core.NativeVR720;
import com.imengyu.vr720.core.NativeVR720Renderer;
import com.imengyu.vr720.core.panorama.PanoramaViewBinder;
import com.imengyu.vr720.core.sensor.ImprovedOrientationSensor1Provider;
import com.imengyu.vr720.core.utils.GameUpdateThread;
import com.imengyu.vr720.dialog.CommonDialog;
import com.imengyu.vr720.dialog.LoadingDialog;
import com.imengyu.vr720.model.ImageInfo;
import com.imengyu.vr720.model.ImageItem;
import com.imengyu.vr720.service.ListDataService;
import com.imengyu.vr720.service.ListImageCacheService;
import com.imengyu.vr720.utils.DateUtils;
import com.imengyu.vr720.utils.FileSizeUtil;
import com.imengyu.vr720.utils.FileUtils;
import com.imengyu.vr720.utils.ImageUtils;
import com.imengyu.vr720.utils.PixelTool;
import com.imengyu.vr720.utils.RendererUtils;
import com.imengyu.vr720.utils.ScreenUtils;
import com.imengyu.vr720.utils.ShareUtils;
import com.imengyu.vr720.utils.StatusBarUtils;
import com.imengyu.vr720.utils.StorageDirUtils;
import com.imengyu.vr720.utils.StringUtils;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Date;

public class PanoActivity extends AppCompatActivity {

    /** Tag for logging. */
    public static final String TAG = PanoActivity.class.getSimpleName();

    private String filePathReal;
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

    private Size screenSize;

    private String openNextPath;
    private boolean openNextMarked = false;
    private boolean closeMarked = false;
    private boolean initialized = false;
    private boolean lastTouchMoved = false;
    private boolean lastDoubleClick = false;
    private long firstClickTime = 0;
    private final int MAX_LONG_PRESS_TIME = 250;// 长按/双击最长等待时间

    private ListDataService listDataService;
    private ListImageCacheService listImageCacheService;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_pano);
        //设置全屏
        ScreenUtils.setFullScreen(this, true);

        StatusBarUtils.setDarkMode(this);
        StatusBarUtils.setStatusBarColor(this, getColor(R.color.colorPrimary));

        VR720Application application = ((VR720Application)getApplication());
        if(!application.isInitFinish())
            finish();

        listImageCacheService = application.getListImageCacheService();
        listDataService = application.getListDataService();
        screenSize = ScreenUtils.getScreenSize(this);

        //初始化内核
        NativeVR720.initNative(getAssets(), this);

        viewBinder = new PanoramaViewBinder(this);

        initSettings();
        initSensor();
        initRenderer();
        initView();

        //检查
        if(!RendererUtils.checkSupportsEs3(this)) {
            showErr(getString(R.string.text_your_device_dosnot_support_es20));
            return;
        }

        onUpdateScreenOrientation(getResources().getConfiguration());

        initialized = true;
        onResume();

        //加载图片
        readArgAndLoadImage();
    }

    private void readArgAndLoadImage() {
        //读取参数
        Intent intent = getIntent();
        if (intent.hasExtra("fileList"))
            fileList = intent.getCharSequenceArrayListExtra("fileList");
        if (intent.hasExtra("openFilePath"))
            filePath = intent.getStringExtra("openFilePath");
        if (intent.hasExtra("openFileArgPath"))
            filePathReal = intent.getStringExtra("openFileArgPath");
        if (intent.hasExtra("openFileIsInCache"))
            currentFileIsOpenInCache = intent.getBooleanExtra("openFileIsInCache", false);

        loadFile(filePath, true);
    }

    @Override
    protected void onDestroy() {
        unInitSensor();
        viewBinder.glSurfaceView.setCaptureCallback(null);
        renderer.onDestroy();
        super.onDestroy();
    }

    private NativeVR720Renderer renderer;
    private GameUpdateThread gameUpdateThread = null;
    private PanoramaViewBinder viewBinder = null;
    private int updateFpsTick = 0;
    private final StringBuilder debugString = new StringBuilder();

    private void initRenderer() {
        renderer = new NativeVR720Renderer(handler);
        renderer.setEnableFullChunks(panoEnableFull);
        renderer.setOnRequestGyroValueCallback(quaternion -> orientationSensor1Provider.getQuaternion(quaternion));
        renderer.setCachePath(StorageDirUtils.getViewCachePath());
        renderer.setEnableCache(panoEnableCache);
        renderer.setPanoramaMode(currentPanoMode);
        renderer.setProp(NativeVR720Renderer.PROP_LOG_LEVEL, logLevel);
        renderer.setProp(NativeVR720Renderer.PROP_ENABLE_LOG, logEnabled);
        renderer.setProp(NativeVR720Renderer.PROP_ENABLE_NATIVE_DECODER, useNativeDecoder);
        viewBinder.glSurfaceView.setNativeRenderer(renderer);
        viewBinder.glSurfaceView.setCaptureCallback(this::screenShotCallback);
        viewBinder.glSurfaceView.setRendererCallback((gl10) -> {
            if(updateFpsTick < 0xffff) {
                updateFpsTick++;
                if(updateFpsTick % 20 == 0) doRenderTick();
            } else updateFpsTick = 0;
        });
        viewBinder.glSurfaceView.setDragVelocityEnabled(dragVelocityEnabled);
        if(enableCustomFps) viewBinder.glSurfaceView.setFps(customFps);
        else viewBinder.glSurfaceView.setUseLowFps(enableLowFps);
        gameUpdateThread = new GameUpdateThread(renderer);
        gameUpdateThread.startUpdateThread();
    }
    private void initView() {

        viewBinder.titlebar.setLeftIconOnClickListener((v) -> onBackPressed());
        viewBinder.titlebar.setRightIconOnClickListener((v) -> shareThisImage());
        viewBinder.layout_video_control.setVisibility(View.GONE);
        viewBinder.layout_debug.setVisibility(debugEnabled ? View.VISIBLE : View.GONE);
        viewBinder.pano_error.setVisibility(View.GONE);
        viewBinder.pano_info.setVisibility(View.GONE);
        viewBinder.pano_menu.setVisibility(View.GONE);
        viewBinder.pano_tools.setVisibility(View.VISIBLE);
        viewBinder.pano_info.setOnTouchListener((view, motionEvent) -> {
            if (motionEvent.getAction() == MotionEvent.ACTION_UP)
                view.performClick();
            return true;
        });
        viewBinder.text_debug.setVisibility(debugEnabled ? View.VISIBLE : View.GONE);
        viewBinder.text_sensor_cant_use_in_mode.setVisibility(View.GONE);

        //判断陀螺仪是否可用，否则禁用界面
        if (!orientationSensor1Provider.isDeviceSupport()) {
            viewBinder.text_not_support_sensor.setVisibility(View.VISIBLE);
            viewBinder.switch_enable_gyro.setChecked(false);
            viewBinder.switch_enable_gyro.setEnabled(false);
            gyroEnabled = false;
        } else {
            viewBinder.text_not_support_sensor.setVisibility(View.GONE);
        }

        viewBinder.button_mode.setOnClickListener(v -> changeMode());

        viewBinder.switch_enable_gyro.setOnCheckedChangeListener((compoundButton, b) -> {
            gyroEnabled = compoundButton.isChecked();
            if(gyroEnabled) orientationSensor1Provider.start();
            else orientationSensor1Provider.stop();
            renderer.setGyroEnable(gyroEnabled);
            viewBinder.updateModeText(gyroEnabled, currentPanoMode);
        });
        viewBinder.switch_enable_vr.setOnCheckedChangeListener((compoundButton, b) -> {
            vrEnabled = compoundButton.isChecked();
            renderer.setVREnable(vrEnabled);
        });
        viewBinder.switch_enable_gyro.setChecked(gyroEnabled);
        viewBinder.switch_enable_vr.setChecked(vrEnabled);

        renderer.setVREnable(vrEnabled);
        renderer.setGyroEnable(gyroEnabled);

        //视图点击事件
        viewBinder.glSurfaceView.setOnTouchListener((view, motionEvent) -> {
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_MOVE: {
                    lastTouchMoved = true;
                    break;
                }
                case MotionEvent.ACTION_DOWN: {
                    lastTouchMoved = false;
                    if (panoMenuShow)
                        setPanoMenuStatus(false, true);
                    if (panoInfoShow)
                        setPanoInfoStatus(false, false);

                    if (motionEvent.getPointerCount() == 1
                            && System.currentTimeMillis() - firstClickTime <= MAX_LONG_PRESS_TIME) {
                        //处理双击
                        lastDoubleClick = true;
                        firstClickTime = 0;

                        resetMode();
                    }
                    break;
                }
                case MotionEvent.ACTION_UP: {
                    if (!lastTouchMoved && motionEvent.getPointerCount() == 1)
                        firstClickTime = System.currentTimeMillis();
                    else
                        firstClickTime = 0;
                    if (!lastDoubleClick && !lastTouchMoved) {
                        switchToolBar();
                        view.performClick();
                    }
                    if(lastDoubleClick)
                        lastDoubleClick = false;
                    break;
                }
            }
            return false;
        });

        viewBinder.button_next.setOnClickListener((v) -> nextFile());
        viewBinder.button_prev.setOnClickListener((v) -> prevFile());
        viewBinder.button_next.setVisibility(View.GONE);
        viewBinder.button_prev.setVisibility(View.GONE);

        viewBinder.titlebar.setLeftIconOnClickListener(v -> onBackPressed());

        viewBinder.button_short.setOnClickListener(v -> {
            if(!fileLoading && fileLoaded)
                screenShot();
        });
        viewBinder.button_more.setOnClickListener(v -> {
            if(!fileLoading && fileLoaded)
                setPanoMenuStatus(!panoMenuShow, true);
        });
        viewBinder.button_like.setOnClickListener(v -> {
            if(currentImageItem != null) {
                if(currentImageItem.isInBelongGalleries(ListDataService.GALLERY_LIST_ID_I_LIKE)) {
                    currentImageItem.belongGalleries.remove((Integer) ListDataService.GALLERY_LIST_ID_I_LIKE);
                    ToastUtils.show(getString(R.string.text_removed_from_i_like));
                }
                else {
                    currentImageItem.belongGalleries.add(ListDataService.GALLERY_LIST_ID_I_LIKE);
                    ToastUtils.show(getString(R.string.text_added_to_i_like));
                }
                updateLikeButtonState();
            }
        });

        findViewById(R.id.action_view_file_info).setOnClickListener(v -> setPanoInfoStatus(true, true));
        findViewById(R.id.action_delete_file).setOnClickListener(v -> deleteFile());
        findViewById(R.id.action_openwith_text).setOnClickListener(v -> openWithOtherApp());
        findViewById(R.id.layout_info_path).setOnClickListener((v) -> {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData mClipData = ClipData.newPlainText(filePath, filePath);
            cm.setPrimaryClip(mClipData);
            ToastUtils.show(getString(R.string.text_path_copied_to_clipboard));
        });
        findViewById(R.id.button_close_pano_info).setOnClickListener(v -> {
            if(!fileLoading && fileLoaded)
                setPanoInfoStatus(false, true);
        });
        findViewById(R.id.button_back).setOnClickListener(v -> closeFile(true));
        findViewById(R.id.button_debug_close).setOnClickListener((v) -> viewBinder.layout_debug.setVisibility(View.GONE));

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

        viewBinder.seek_x.setOnSeekBarChangeListener(listener);
        viewBinder.seek_y.setOnSeekBarChangeListener(listener);
        viewBinder.seek_z.setOnSeekBarChangeListener(listener);

        viewBinder.text_debug.setOnClickListener((v) -> viewBinder.layout_debug.setVisibility(View.VISIBLE));
        viewBinder.button_video_play_pause.setOnClickListener((v) -> {
            if(fileLoaded) {
                int state = renderer.getVideoState();
                if(state == NativeVR720Renderer.VideoState_Playing)
                    renderer.updateVideoState(NativeVR720Renderer.VideoState_Paused);
                else if(state == NativeVR720Renderer.VideoState_Paused)
                    renderer.updateVideoState(NativeVR720Renderer.VideoState_Playing);
                else if(state == NativeVR720Renderer.VideoState_Ended) {
                    renderer.setVideoPos(0);
                    renderer.updateVideoState(NativeVR720Renderer.VideoState_Playing);
                }
            }
        });
        viewBinder.seek_video.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateVideoControlState();
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                lastVideoIsPlay = renderer.getVideoState() == NativeVR720Renderer.VideoState_Playing;
                renderer.updateVideoState(NativeVR720Renderer.VideoState_Paused);
                lockVideoSeekUpdate = true;
                updateVideoControlPlayState();
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                lockVideoSeekUpdate = false;
                if(fileLoaded)
                    renderer.setVideoPos((int) ((seekBar.getProgress() / 100.0f) * currentVideoLength));
                if(lastVideoIsPlay)
                    renderer.updateVideoState(NativeVR720Renderer.VideoState_Playing);
                updateVideoControlPlayState();
                updateVideoControlState();
            }
        });

        //设置上次模式
        viewBinder.updateModeButton(gyroEnabled, currentPanoMode);
    }
    private void initSettings() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        panoEnableCache = sharedPreferences.getBoolean("enable_view_cache", true);
        panoEnableFull = sharedPreferences.getBoolean("enable_full_chunks", false);
        currentPanoMode = sharedPreferences.getInt("pano_mode", 0);
        vrEnabled = sharedPreferences.getBoolean("pano_enable_vr", false);
        gyroEnabled = sharedPreferences.getBoolean("pano_enable_gyro", false);
        dragVelocityEnabled = sharedPreferences.getBoolean("enable_drag_velocity", true);
        debugEnabled = sharedPreferences.getBoolean("show_debug_tool", false);
        dontCheckImageNormalSize = sharedPreferences.getBoolean("do_not_check_pano_normal_size", false);
        enableCustomFps = sharedPreferences.getBoolean("enable_custom_fps", false);
        enableLowFps = sharedPreferences.getBoolean("enable_low_fps", false);
        customFps = sharedPreferences.getInt("custom_fps_value", 0);
        logLevel = sharedPreferences.getInt("native_log_level", 0);
        logEnabled = sharedPreferences.getBoolean("enable_native_log", true);
        useNativeDecoder = sharedPreferences.getBoolean("use_native_decoder", true);
        loopPlay = sharedPreferences.getBoolean("loop_play", false);

        //no full screen
        if(sharedPreferences.getBoolean("enable_non_fullscreen", false))
            ScreenUtils.setFullScreen(this, false);
        //keep screen on
        if(sharedPreferences.getBoolean("enable_keep_screen_on", false))
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
    private void saveSettings() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putInt("pano_mode", currentPanoMode);
        editor.putBoolean("pano_enable_vr", vrEnabled);
        editor.putBoolean("pano_enable_gyro", gyroEnabled);
        editor.putBoolean("do_not_check_pano_normal_size", dontCheckImageNormalSize);

        editor.apply();
    }
    private void updateDebugSeek() {
        renderer.updateDebugValue(
                (viewBinder.seek_x.getProgress() / 100.0f) * 360.0f,
                (viewBinder.seek_y.getProgress() / 100.0f) * 360.0f,
                (viewBinder.seek_z.getProgress() / 100.0f) * 360.0f,
                0,0,0);
    }
    private void doRenderTick() {
        //FPS计算
        if(debugEnabled) {
            //FPS
            debugString.append("FPS: ");
            debugString.append(viewBinder.glSurfaceView.getFps());

            handler.sendEmptyMessage(MainMessages.MSG_TEST_VAL);
        }

        handler.sendEmptyMessage(MainMessages.MSG_RENDER_TICK);
    }
    private void handleRenderTick() {
        //视频控件状态更新
        if(currentVideoPlaying)
            updateVideoControlState();
    }

    //界面控制
    //**********************

    private boolean currentFileHasError = false;
    private boolean currentFileIsVideo = false;
    private boolean currentFileIsOpenInCache = false;
    private int currentVideoLength = 0;
    private boolean currentVideoPlaying = false;
    private Size currentImageSize = new Size(0,0);
    private ImageItem currentImageItem = null;
    private int currentPanoMode = 0;
    private int logLevel = 0;
    private boolean logEnabled = true;
    private boolean loopPlay= false;
    private boolean panoEnableCache = true;
    private boolean panoEnableFull = false;
    private boolean vrEnabled = false;
    private boolean gyroEnabled = false;
    private boolean dragVelocityEnabled = true;
    private boolean debugEnabled = true;
    private boolean enableLowFps = false;
    private boolean enableCustomFps = false;
    private boolean useNativeDecoder = true;
    private int customFps = 0;
    private boolean dontCheckImageNormalSize = true;
    private boolean lockVideoSeekUpdate = false;
    private boolean lastVideoIsPlay = false;

    private void changeMode() {
        if(currentPanoMode < NativeVR720Renderer.PanoramaMode_PanoramaModeMax - 1)
            currentPanoMode++;
        else
            currentPanoMode = NativeVR720Renderer.PanoramaMode_PanoramaSphere;
        renderer.setPanoramaMode(currentPanoMode);
        viewBinder.updateModeButton(gyroEnabled, currentPanoMode);
    }
    private void deleteFile() {
        new CommonDialog(this)
            .setTitle(R.string.text_sure_delete_this_image)
            .setMessage(filePath)
            .setNegative(R.string.action_cancel)
            .setPositive(R.string.action_sure_delete)
            .setOnResult((b, dialog) -> {
                if(b == CommonDialog.BUTTON_POSITIVE) {
                    if(FileUtils.deleteFile(filePath)) {
                        ToastUtils.show(getString(R.string.text_delete_success));
                        Intent intent = new Intent();
                        intent.putExtra("isDeleteFile", true);
                        intent.putExtra("filePath", filePath);
                        setResult(0, intent);
                        finish();
                    }else
                        ToastUtils.show(getString(R.string.text_delete_failed));
                    return true;
                } else return b == CommonDialog.BUTTON_NEGATIVE;
            })
            .show();
    }
    private void openWithOtherApp() { FileUtils.openFileWithApp(this, filePath); }
    private void shareThisImage() { ShareUtils.shareFile(this, filePath); }
    private void showErr(String err) {
        viewBinder.pano_error.setVisibility(View.VISIBLE);
        viewBinder.pano_loading.setVisibility(View.GONE);
        viewBinder.text_pano_error.setText(err);
        viewBinder.titlebar.setTitle(getString(R.string.app_name));
        currentFileHasError = true;
    }
    private void clearErr() {
        viewBinder.pano_error.setVisibility(View.GONE);
        currentFileHasError = false;
    }
    private void screenShot() {
        captureLoadingDialog = new LoadingDialog(this);
        captureLoadingDialog.show();
        viewBinder.glSurfaceView.startCapture();
    }
    private void screenShotCallback(Bitmap b) {

        //保存图像到SCREENSHOT文件夹
        ImageUtils.SaveImageResult result = ImageUtils.saveImageToGalleryWithAutoName(this, b);
        //显示
        runOnUiThread(() -> {
            captureLoadingDialog.cancel();
            captureLoadingDialog = null;

            if(result.success)
                ToastUtils.show(getString(R.string.text_image_save_success));
            else
                ToastUtils.show(String.format(getString(R.string.text_image_save_failed), result.error));
        });

    }
    private void resetMode() { renderer.setPanoramaMode(currentPanoMode); }

    private boolean currentCanNext = false;
    private boolean currentCanPrev = false;
    private LoadingDialog captureLoadingDialog = null;

    private boolean toolbarOn = true;
    private boolean panoInfoShow = false;
    private boolean panoMenuShow = false;
    private boolean isLandscape = false;

    //界面相关刷新函数
    //**********************

    /**
     *  标题栏开启关闭
     */
    private void switchToolBar() {
        if(toolbarOn && viewBinder.pano_tools.getVisibility() != View.VISIBLE)
            setPanoInfoStatus(false, false);
        setToolBarStatus(!toolbarOn);
    }
    private void setToolBarStatus(boolean show) {
        toolbarOn = show;
        if (toolbarOn) {
            viewBinder.pano_tools.startAnimation(viewBinder.bottom_up);
            viewBinder.pano_tools.setVisibility(View.VISIBLE);
            viewBinder.titlebar.startAnimation(viewBinder.top_down);
            viewBinder.titlebar.setVisibility(View.VISIBLE);

            if(currentCanNext) {
                viewBinder.button_next.setVisibility(View.VISIBLE);
                viewBinder.button_next.startAnimation(viewBinder.fade_show);
            }
            if(currentCanPrev) {
                viewBinder.button_prev.setVisibility(View.VISIBLE);
                viewBinder.button_prev.startAnimation(viewBinder.fade_show);
            }

        } else {
            viewBinder.pano_tools.startAnimation(viewBinder.bottom_down);
            viewBinder.pano_tools.setVisibility(View.GONE);
            viewBinder.titlebar.startAnimation(viewBinder.top_up);
            viewBinder.titlebar.setVisibility(View.GONE);

            if(currentCanNext) {
                viewBinder.button_next.setVisibility(View.GONE);
                viewBinder.button_next.startAnimation(viewBinder.fade_hide);
            }
            if(currentCanPrev) {
                viewBinder.button_prev.startAnimation(viewBinder.fade_hide);
                viewBinder.button_prev.setVisibility(View.GONE);
            }
        }
    }
    private void setPanoMenuStatus(boolean show, boolean anim) {
        panoMenuShow = show;
        if(show) {
            if(anim)
                viewBinder.pano_menu.startAnimation(isLandscape ? viewBinder.right_to_left_in : viewBinder.bottom_up);
            viewBinder.pano_menu.setVisibility(View.VISIBLE);
        } else {
            if(anim)
                viewBinder.pano_menu.startAnimation(isLandscape ? viewBinder.left_to_right_out : viewBinder.fade_hide);
            viewBinder.pano_menu.setVisibility(View.GONE);
        }
    }
    private void setPanoInfoStatus(boolean show, boolean anim) {
        panoInfoShow = show;
        if(show) {
            setPanoMenuStatus(false, true);

            if(isLandscape && toolbarOn)
                switchToolBar();
            else {
                viewBinder.pano_tools.startAnimation(viewBinder.fade_hide);
                viewBinder.pano_tools.setVisibility(View.GONE);
            }

            if(anim)
                viewBinder.pano_info.startAnimation(viewBinder.bottom_up);
            viewBinder.pano_info.setVisibility(View.VISIBLE);

        } else {

            if(anim)
                viewBinder.pano_info.startAnimation(isLandscape ? viewBinder.left_to_right_out : viewBinder.bottom_down);
            viewBinder.pano_info.setVisibility(View.GONE);

            if(!isLandscape) {
                if(anim)
                    viewBinder.pano_tools.startAnimation(viewBinder.bottom_up);
                viewBinder.pano_tools.setVisibility(View.VISIBLE);
            }
        }
    }
    private void flushToolStatus() {
        if (toolbarOn) {
            viewBinder.pano_tools.setVisibility(View.VISIBLE);
            viewBinder.titlebar.setVisibility(View.VISIBLE);

            if(currentCanNext) viewBinder.button_next.setVisibility(View.VISIBLE);
            if(currentCanPrev) viewBinder.button_prev.setVisibility(View.VISIBLE);

        } else {
            viewBinder.pano_tools.setVisibility(View.GONE);
            viewBinder.titlebar.setVisibility(View.GONE);

            if(currentCanNext) viewBinder.button_next.setVisibility(View.GONE);
            if(currentCanPrev) viewBinder.button_prev.setVisibility(View.GONE);
        }
        viewBinder.pano_menu.setVisibility(panoMenuShow ? View.VISIBLE : View.GONE);
        viewBinder.pano_info.setVisibility(panoInfoShow ? View.VISIBLE : View.GONE);
    }
    private void updatePrevNextBtnState() {
        if(fileList == null) {
            currentCanNext = false;
            currentCanPrev = false;
            viewBinder.button_next.setVisibility(View.GONE);
            viewBinder.button_prev.setVisibility(View.GONE);
        } else {
            int currentIndex = findIndexInPathList(filePath);
            currentCanNext = currentIndex < fileList.size() - 1;
            currentCanPrev = currentIndex > 0;
            viewBinder.button_next.setVisibility(currentCanNext ? View.VISIBLE : View.GONE);
            viewBinder.button_prev.setVisibility(currentCanPrev ? View.VISIBLE : View.GONE);
        }
    }
    private void updateLikeButtonState() {
        currentImageItem = listDataService.findImageItem(filePath);
        if(currentImageItem == null) {
            TextViewCompat.setCompoundDrawableTintList(viewBinder.button_like, null);
            viewBinder.button_like.setEnabled(false);
        }else {
            viewBinder.button_like.setEnabled(true);
            TextViewCompat.setCompoundDrawableTintList(viewBinder.button_like,
                    currentImageItem.isInBelongGalleries(ListDataService.GALLERY_LIST_ID_I_LIKE) ?
                            viewBinder.likeColorStateList : null);
        }
    }
    private void updateVideoControlState() {
        if(fileLoaded) {
            viewBinder.text_video_current_pos.setText(StringUtils.getTimeString(renderer.getVideoPos()));
            if (!lockVideoSeekUpdate)
                viewBinder.seek_video.setProgress((int) ((renderer.getVideoPos() / (float) currentVideoLength) * 100));
        }
    }
    private void updateVideoControlPlayState() {
        int state = fileLoaded ? renderer.getVideoState() : NativeVR720Renderer.VideoState_NotOpen;
        currentVideoPlaying = (state == NativeVR720Renderer.VideoState_Playing);
        viewBinder.button_video_play_pause.setCompoundDrawables(currentVideoPlaying ?
                viewBinder.ic_pause : viewBinder.ic_play, null, null, null);
    }

    /**
     * 主线程handler
     */
    private static class MainHandler extends Handler {
        private final WeakReference<PanoActivity> mTarget;

        MainHandler(PanoActivity target) {
            super(Looper.myLooper());
            mTarget = new WeakReference<>(target);
        }

        @Override
        public void handleMessage(Message msg) {
            PanoActivity activity = mTarget.get();
            switch (msg.what) {
                case MainMessages.MSG_NATIVE_MESSAGE: activity.onRendererMessage((int)msg.obj); break;
                case MainMessages.MSG_LOAD_IMAGE_INFO: activity.loadImageInfoToUI(); break;
                case MainMessages.MSG_LOAD_VIDEO_INFO: activity.loadVideoInfoToUI(); break;
                case MainMessages.MSG_QUIT_LATE: activity.onQuitLate(); break;
                case MainMessages.MSG_FORCE_PAUSE: activity.onPause(); break;
                case MainMessages.MSG_TEST_VAL: activity.viewBinder.updateDebugValue(activity.debugString); break;
                case MainMessages.MSG_RENDER_TICK: activity.handleRenderTick(); break;
                default: super.handleMessage(msg);
            }
        }
    }
    private final MainHandler handler = new MainHandler(this);

    //重载相关响应函数
    //**********************

    private void onRendererIsReady() {

        if(!ImageUtils.checkSizeIs720Panorama(currentImageSize) && ImageUtils.checkSizeIs320Panorama(currentImageSize))
            currentPanoMode = NativeVR720Renderer.PanoramaMode_PanoramaFull360;

        viewBinder.titlebar.setTitle(FileUtils.getFileName(filePath));
        viewBinder.pano_loading.setVisibility(View.GONE);

        currentFileHasError = false;
        currentFileIsVideo = renderer.getCurrentFileIsVideo();
        if(currentFileIsVideo) {
            viewBinder.layout_video_control.setVisibility(View.VISIBLE);
            //写入视频初始信息到UI上
            currentVideoLength = renderer.getVideoLength();
            viewBinder.text_video_length.setText(StringUtils.getTimeString(currentVideoLength));
            viewBinder.seek_video.setProgress(0);
            renderer.updateVideoState(NativeVR720Renderer.VideoState_Playing);
        }
        else {
            viewBinder.layout_video_control.setVisibility(View.GONE);
        }

        setPanoInfoStatus(false, false);
        setPanoMenuStatus(false, false);
        setToolBarStatus(true);
    }
    private void onRendererMessage(int msg) {
        switch (msg) {
            case NativeVR720Renderer.MobileGameUIEvent_FileClosed: {
                fileLoaded = false;
                //关闭后删除缓存文件
                if (currentFileIsOpenInCache && filePath.startsWith(StorageDirUtils.getViewCachePath())) {
                    try {
                        File file = new File(filePath);
                        if (file.exists())
                            Log.i(TAG, String.format("Delete temp file : %s ,result: %b", filePath, file.delete()));
                    }catch (Exception e) {
                        Log.w(TAG, String.format("Try delete temp file : %s failed: %s", filePath, e.toString()), e.fillInStackTrace());
                    }
                }
                //如果是退出则销毁
                if (closeMarked) {
                    renderer.destroy();
                }
                else if (openNextMarked) {
                    openNextMarked = false;
                    loadFile(openNextPath, false);
                    openNextPath = null;
                }
                break;
            }
            case NativeVR720Renderer.MobileGameUIEvent_MarkLoadFailed: {
                fileLoading = false;
                viewBinder.titlebar.setVisibility(View.VISIBLE);
                String error = renderer.getLastError(this);
                Log.w(TAG, String.format("Image load failed : %s", error));
                showErr(error);
                break;
            }
            case NativeVR720Renderer.MobileGameUIEvent_MarkLoadingEnd: {
                fileLoading = false;
                fileLoaded = true;
                clearErr();
                onRendererIsReady();
                break;
            }
            case NativeVR720Renderer.MobileGameUIEvent_MarkLoadingStart: {
                fileLoading = true;
                fileLoaded = false;
                viewBinder.titlebar.setTitle(getString(R.string.text_loading_wait));
                viewBinder.pano_loading.setVisibility(View.VISIBLE);
                setPanoInfoStatus(false, false);
                setPanoMenuStatus(false, false);
                clearErr();
                break;
            }
            case NativeVR720Renderer.MobileGameUIEvent_UiInfoChanged: break;
            case NativeVR720Renderer.MobileGameUIEvent_DestroyComplete: {
                fileLoaded = false;
                fileLoading = false;
                viewBinder.glSurfaceView.onDestroyComplete();
                handler.sendEmptyMessage(MainMessages.MSG_QUIT_LATE);
            }
            case NativeVR720Renderer.MobileGameUIEvent_VideoStateChanged: {
                updateVideoControlState();
                updateVideoControlPlayState();
                int state = fileLoaded ? renderer.getVideoState() : NativeVR720Renderer.VideoState_NotOpen;
                if(state == NativeVR720Renderer.VideoState_Ended && loopPlay) {
                    renderer.setVideoPos(0);
                    renderer.updateVideoState(NativeVR720Renderer.VideoState_Playing);
                }
                break;
            }
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

        //暂停视频
        if(currentFileIsVideo && renderer.getVideoState() == NativeVR720Renderer.VideoState_Playing)
            renderer.updateVideoState(NativeVR720Renderer.VideoState_Paused);

        saveSettings();
        gameUpdateThread.stopUpdateThread();

        if(gyroEnabled)
            orientationSensor1Provider.stop();

        viewBinder.glSurfaceView.onPause();

        super.onPause();
    }
    @Override
    protected void onResume() {
        if(initialized && !closeMarked) {
            if(gyroEnabled)
                orientationSensor1Provider.start();

            viewBinder.glSurfaceView.onResume();
            gameUpdateThread.startUpdateThread();
        }

        super.onResume();
    }
    @Override
    public void onBackPressed() {
        if(!closeMarked) closeFile(true);
        else super.onBackPressed();
    }

    //屏幕旋转的处理
    //**********************

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        onUpdateScreenOrientation(newConfig);
    }

    private final ConstraintSet mConstraintSet = new ConstraintSet();

    private void onUpdateScreenOrientation(Configuration newConfig) {
        if(newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            isLandscape = false;

            viewBinder.button_like.setTextVisible(true);
            viewBinder.button_short.setTextVisible(true);
            viewBinder.button_mode.setTextVisible(true);
            viewBinder.button_more.setTextVisible(true);

            mConstraintSet.clone(viewBinder.activity_pano);
            mConstraintSet.clear(R.id.pano_info);
            mConstraintSet.clear(R.id.pano_menu);

            mConstraintSet.connect(R.id.pano_menu, ConstraintSet.RIGHT, ConstraintSet.PARENT_ID, ConstraintSet.RIGHT);
            mConstraintSet.connect(R.id.pano_info, ConstraintSet.RIGHT, ConstraintSet.PARENT_ID, ConstraintSet.RIGHT);
            mConstraintSet.connect(R.id.pano_menu, ConstraintSet.LEFT, ConstraintSet.PARENT_ID, ConstraintSet.LEFT);
            mConstraintSet.connect(R.id.pano_info, ConstraintSet.LEFT, ConstraintSet.PARENT_ID, ConstraintSet.LEFT);
            mConstraintSet.connect(R.id.pano_menu, ConstraintSet.BOTTOM, R.id.pano_tools, ConstraintSet.TOP);
            mConstraintSet.connect(R.id.pano_info, ConstraintSet.BOTTOM, R.id.pano_tools, ConstraintSet.TOP);
            mConstraintSet.applyTo(viewBinder.activity_pano);

            ViewGroup.LayoutParams layoutParams = viewBinder.pano_info.getLayoutParams();
            layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
            layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            viewBinder.pano_info.setLayoutParams(layoutParams);
            layoutParams = viewBinder.pano_menu.getLayoutParams();
            layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
            layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            viewBinder.pano_menu.setLayoutParams(layoutParams);

        }
        else if(newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            isLandscape = true;

            viewBinder.button_like.setTextVisible(false);
            viewBinder.button_short.setTextVisible(false);
            viewBinder.button_mode.setTextVisible(false);
            viewBinder.button_more.setTextVisible(false);

            mConstraintSet.clone(viewBinder.activity_pano);
            mConstraintSet.clear(R.id.pano_info);
            mConstraintSet.clear(R.id.pano_menu);
            mConstraintSet.connect(R.id.pano_info, ConstraintSet.RIGHT, ConstraintSet.PARENT_ID, ConstraintSet.RIGHT);
            mConstraintSet.connect(R.id.pano_info, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);
            mConstraintSet.connect(R.id.pano_info, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM);

            mConstraintSet.connect(R.id.pano_menu, ConstraintSet.RIGHT, ConstraintSet.PARENT_ID, ConstraintSet.RIGHT, PixelTool.dp2px(this, 36));
            mConstraintSet.connect(R.id.pano_menu, ConstraintSet.BOTTOM, R.id.pano_tools, ConstraintSet.TOP, 20);

            mConstraintSet.applyTo(viewBinder.activity_pano);

            ViewGroup.LayoutParams layoutParams = viewBinder.pano_info.getLayoutParams();
            layoutParams.width = (int)(screenSize.getHeight() / 2.4);
            layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
            viewBinder.pano_info.setLayoutParams(layoutParams);

            layoutParams = viewBinder.pano_menu.getLayoutParams();
            layoutParams.width = screenSize.getHeight() / 3;
            layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            viewBinder.pano_menu.setLayoutParams(layoutParams);
        }
        flushToolStatus();
    }

    //主图片处理逻辑
    //**********************

    private final ImageInfo imageInfo = new ImageInfo();
    private interface OnLoadImageInfoFinishedListener {
        void onLoadImageInfoFinishedListener(Size imageSize, boolean isFromInitialization);
    }

    private void loadImageInfo(boolean isFromInitialization, OnLoadImageInfoFinishedListener loadImageInfoFinishedListener) {
        final String text_un_know = getResources().getString(R.string.text_un_know);
        new Thread(() -> {

            //加载图像基础信息
            Size imageSize = new Size(0,0);
            try {
                imageSize = ImageUtils.getImageSize(filePath);
            }
            catch (IOException e) {
                Log.e(TAG, "Get image size failed : " + e.toString());
                e.printStackTrace();
            }

            if(imageSize.getHeight() == imageSize.getWidth() && imageSize.getWidth() == -1) {
                runOnUiThread(() -> showErr(getString(R.string.text_error_bad_image)));
                return;
            }

            imageInfo.imageFileSize = FileSizeUtil.getAutoFileOrFilesSize(filePath);
            String fileTime = DateUtils.format(new Date((new File(filePath)).lastModified()));
            imageInfo.imageTime = fileTime;

            //加载EXIF数据
            try {
                ExifInterface exifInterface = new ExifInterface(filePath);

                imageInfo.imageTime = exifInterface.getAttribute(ExifInterface.TAG_DATETIME);
                imageInfo.imageSize = exifInterface.getAttribute(ExifInterface.TAG_IMAGE_WIDTH) +
                        "x" + exifInterface.getAttribute(ExifInterface.TAG_IMAGE_LENGTH);
                imageInfo.imageISOSensitivity = exifInterface.getAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    imageInfo.imageShutterTime = exifInterface.getAttribute(ExifInterface.TAG_EXPOSURE_TIME);
                    imageInfo.imageExposureBiasValue = exifInterface.getAttribute(ExifInterface.TAG_EXPOSURE_BIAS_VALUE);
                }

                if(imageInfo.imageTime == null)
                    imageInfo.imageTime = fileTime;
                if(imageInfo.imageSize.equals("x"))
                    imageInfo.imageSize = imageSize.getWidth() + "x" + imageSize.getHeight();
                if(StringUtils.isNullOrEmpty(imageInfo.imageShutterTime))
                    imageInfo.imageShutterTime = text_un_know;
                if(StringUtils.isNullOrEmpty(imageInfo.imageExposureBiasValue))
                    imageInfo.imageExposureBiasValue = text_un_know;
                if(StringUtils.isNullOrEmpty(imageInfo.imageISOSensitivity))
                    imageInfo.imageISOSensitivity = text_un_know;

            }
            catch (IOException e) {
                Log.e(TAG, "Load image exif data failed : " + e.toString());
                e.printStackTrace();
            }

            //尝试首先加载最小的缩略图
            String cacheFilePath = listImageCacheService.tryGetImageThumbnailCacheFilePath(filePath);
            renderer.setProp(NativeVR720Renderer.PROP_SMALL_PANORAMA_PATH, cacheFilePath);

            //完成通知
            loadImageInfoFinishedListener.onLoadImageInfoFinishedListener(imageSize, isFromInitialization);
            handler.sendEmptyMessage(MainMessages.MSG_LOAD_IMAGE_INFO);
        }).start();
    }
    private void loadImageInfoToUI() {
        viewBinder.text_pano_info_size.setText(imageInfo.imageSize);
        viewBinder.text_pano_info_file_size.setText(imageInfo.imageFileSize);
        viewBinder.text_pano_info_image_time.setText(imageInfo.imageTime);
        viewBinder.text_pano_info_file_path.setText(filePathReal.equals("") ? filePath : filePathReal);
        viewBinder.text_pano_info_shutter_time.setText(imageInfo.imageShutterTime);
        viewBinder.text_pano_info_exposure_bias_value.setText(imageInfo.imageExposureBiasValue);
        viewBinder.text_pano_info_iso_sensitivit.setText(imageInfo.imageISOSensitivity);

        ((ViewGroup)viewBinder.text_pano_info_shutter_time.getParent()).setVisibility(View.VISIBLE);
        ((ViewGroup)viewBinder.text_pano_info_exposure_bias_value.getParent()).setVisibility(View.VISIBLE);
        ((ViewGroup)viewBinder.text_pano_info_iso_sensitivit.getParent()).setVisibility(View.VISIBLE);
    }
    private void loadVideoInfo(boolean isFromInitialization, OnLoadImageInfoFinishedListener loadImageInfoFinishedListener) {
        new Thread(() -> {

            Size imageSize = new Size(0,0);

            imageInfo.imageFileSize = FileSizeUtil.getAutoFileOrFilesSize(filePath);
            imageInfo.imageTime = DateUtils.format(new Date((new File(filePath)).lastModified()));

            MediaMetadataRetriever mmr = new MediaMetadataRetriever();
            try {

                mmr.setDataSource(filePath);

                String width = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);//宽
                String height = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);//高

                imageSize = new Size(
                        Integer.parseInt(width),
                        Integer.parseInt(height));

            }
            catch (Exception ex) {
                Log.e(TAG, "MediaMetadataRetriever exception " + ex);
            } finally {
                mmr.release();
            }

            if(imageSize.getHeight() <= 0 && imageSize.getWidth() <= 0) {
                runOnUiThread(() -> showErr(getString(R.string.text_error_video_player_av_error)));
                return;
            }

            //尝试首先加载最小的缩略图
            String cacheFilePath = listImageCacheService.tryGetImageThumbnailCacheFilePath(filePath);
            renderer.setProp(NativeVR720Renderer.PROP_SMALL_PANORAMA_PATH, cacheFilePath);

            loadImageInfoFinishedListener.onLoadImageInfoFinishedListener(imageSize, isFromInitialization);

            handler.sendEmptyMessage(MainMessages.MSG_LOAD_VIDEO_INFO);
        }).start();
    }
    private void loadVideoInfoToUI() {
        viewBinder.text_pano_info_size.setText(imageInfo.imageSize);
        viewBinder.text_pano_info_file_size.setText(imageInfo.imageFileSize);
        viewBinder.text_pano_info_image_time.setText(imageInfo.imageTime);
        viewBinder.text_pano_info_file_path.setText(filePathReal.equals("") ? filePath : filePathReal);

        ((ViewGroup)viewBinder.text_pano_info_shutter_time.getParent()).setVisibility(View.GONE);
        ((ViewGroup)viewBinder.text_pano_info_exposure_bias_value.getParent()).setVisibility(View.GONE);
        ((ViewGroup)viewBinder.text_pano_info_iso_sensitivit.getParent()).setVisibility(View.GONE);
    }
    private void loadFile(String path, boolean isFromInitialization) {

        //如果已经打开文件，那么先关闭
        if(renderer.isFileOpen()) {
            openNextPath = path;
            openNextMarked = true;
            closeFile(false);
            return;
        }

        filePath = path;
        currentImageItem = listDataService.findImageItem(filePath);

        //刷新前后按钮状态
        updatePrevNextBtnState();
        updateLikeButtonState();

        //关闭工具栏
        if(panoMenuShow)
            setPanoMenuStatus(false, false);
        if(panoInfoShow)
            setPanoInfoStatus(false, false);

        if(StringUtils.isNullOrEmpty(path))  {
            showErr(getString(R.string.text_not_provide_path));
            return;
        }
        else if(!new File(path).exists())  {
            showErr(getString(R.string.text_error_file_not_exists) + "\n" + path);
            return;
        }

        if(FileUtils.getFileIsImage(filePath)) {

            //这是图片
            //加载图片基础信息
            loadImageInfo(isFromInitialization, (size, b) -> {

                currentImageSize = size;

                runOnUiThread(() -> {
                    if (ImageUtils.checkSizeIsNormalPanorama(size) || dontCheckImageNormalSize) {
                        //加载
                        renderer.openFile(path);
                    } else {
                        //不是正常的全景图，询问用户是否加载
                        new CommonDialog(PanoActivity.this)
                                .setTitle(R.string.text_panorama_size_not_right)
                                .setMessage(R.string.text_panorama_size_not_right_warning_text)
                                .setPositive(R.string.action_cancel_load)
                                .setNegative(R.string.action_continue_load)
                                .setCheckBoxText(R.string.text_do_not_ask_me_again)
                                .setOnResult((button, dialog) -> {
                                    if(button == CommonDialog.BUTTON_POSITIVE) {
                                        if (isFromInitialization)
                                            finish();
                                        else
                                            showErr(getString(R.string.text_panorama_size_not_right));
                                        return true;
                                    } else if(button == CommonDialog.BUTTON_NEGATIVE) {
                                        if (dialog.isCheckBoxChecked())
                                            dontCheckImageNormalSize = true;
                                        //继续加载
                                        renderer.openFile(path);
                                        dialog.dismiss();
                                        return true;
                                    }
                                    return false;
                                })
                                .show();
                    }
                });

            });
        }
        else if(FileUtils.getFileIsVideo(filePath)) {
            //这是视频
            loadVideoInfo(isFromInitialization, (size, b) -> {

                currentImageSize = size;

                runOnUiThread(() -> {
                    if (ImageUtils.checkSizeIsNormalPanorama(size) || dontCheckImageNormalSize) {
                        //加载
                        renderer.openFile(path);
                    } else {
                        //不是正常的全景图，询问用户是否加载
                        new CommonDialog(PanoActivity.this)
                                .setTitle(R.string.text_panorama_video_size_not_right)
                                .setMessage(R.string.text_panorama_video_size_not_right_warning_text)
                                .setPositive(R.string.action_cancel_load)
                                .setNegative(R.string.action_continue_load)
                                .setCheckBoxText(R.string.text_do_not_ask_me_again)
                                .setOnResult((button, dialog) -> {
                                    if(button == CommonDialog.BUTTON_POSITIVE) {
                                        if (isFromInitialization)
                                            finish();
                                        else
                                            showErr(getString(R.string.text_panorama_size_not_right));
                                        return true;
                                    } else if(button == CommonDialog.BUTTON_NEGATIVE) {
                                        if (dialog.isCheckBoxChecked())
                                            dontCheckImageNormalSize = true;
                                        //继续加载
                                        renderer.openFile(path);
                                        return true;
                                    }
                                    return false;
                                })
                                .show();
                    }
                });

            });
        }
        else showErr(getString(R.string.text_image_not_support));
    }
    private void closeFile(boolean quit) {
        if (quit && currentFileHasError) {
            Intent intent = new Intent();
            intent.putExtra("filePath", filePath);
            setResult(0, intent);
            finish();
        }
        closeMarked = quit;
        renderer.closeFile();
    }
    private void nextFile() {
        if(fileLoading)
            return;
        int currentIndex = findIndexInPathList(filePath);
        if(currentIndex >= fileList.size() - 1) {
            ToastUtils.show(getString(R.string.text_this_is_last_image));
        } else {
            loadFile(fileList.get(currentIndex + 1).toString(), false);
        }
    }
    private void prevFile() {
        if(fileLoading)
            return;
        int currentIndex = findIndexInPathList(filePath);
        if(currentIndex == 0) {
            ToastUtils.show(getString(R.string.text_this_is_first_image));
            return;
        }
        loadFile(fileList.get(currentIndex - 1).toString(), false);
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
