package com.imengyu.vr720;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.hardware.SensorManager;
import android.net.Uri;
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
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.widget.TextViewCompat;
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
import com.imengyu.vr720.model.ImageItem;
import com.imengyu.vr720.service.ListDataService;
import com.imengyu.vr720.utils.AlertDialogTool;
import com.imengyu.vr720.utils.DateUtils;
import com.imengyu.vr720.utils.FileSizeUtil;
import com.imengyu.vr720.utils.FileUtils;
import com.imengyu.vr720.utils.ImageUtils;
import com.imengyu.vr720.utils.PixelTool;
import com.imengyu.vr720.utils.ScreenUtils;
import com.imengyu.vr720.utils.StatusBarUtils;
import com.imengyu.vr720.utils.StorageDirUtils;
import com.imengyu.vr720.widget.MyTitleBar;
import com.imengyu.vr720.widget.ToolbarButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PanoActivity extends AppCompatActivity {

    /** Tag for logging. */
    public static final String TAG = PanoActivity.class.getSimpleName();

    private Context mContext;
    private Resources resources;

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
    private TextView text_sensor_cant_use_in_mode;
    private TextView text_debug;
    private ImageButton button_prev;
    private ImageButton button_next;
    private SeekBar seek_x;
    private SeekBar seek_y;
    private SeekBar seek_z;
    private MyTitleBar titlebar;
    private NativeVR720GLSurfaceView glSurfaceView;
    private ToolbarButton button_like;
    private ToolbarButton button_short;
    private ToolbarButton button_mode;
    private ToolbarButton button_more;
    private ConstraintLayout activity_pano;

    private Drawable iconModeBall;
    private Drawable iconModeLittlePlanet;
    private Drawable iconModeRectiliner;
    private Drawable iconModeSource;
    private Drawable iconModeMercator;
    private Drawable iconModeVideoBall;

    private Animation bottom_up, bottom_down;
    private Animation fade_hide, fade_show;
    private Animation top_down, top_up;
    private Animation left_to_right_out, right_to_left_in;
    private ColorStateList likeColorStateList;

    private Size screenSize;

    private String openNextPath;
    private boolean openNextMarked = false;
    private boolean closeMarked = false;
    private boolean initialized = false;
    private boolean lastTouchMoved = false;

    private ListDataService listDataService;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        //设置全屏
        ScreenUtils.setFullScreen(this, true);

        setContentView(R.layout.activity_pano);

        StatusBarUtils.setDarkMode(this);
        StatusBarUtils.setStatusBarColor(this, getColor(R.color.colorPrimary));

        listDataService = ((VR720Application)getApplication()).getListDataService();
        resources = getResources();
        mContext = getApplicationContext();
        screenSize = ScreenUtils.getScreenSize(this);

        ((VR720Application)getApplication()).checkAndInit();

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

        onUpdateScreenOrientation(getResources().getConfiguration());

        initialized = true;
        onResume();

        //加载图片
        readArgAndLoadImage();
    }

    private void readArgAndLoadImage() {
        //读取输入路径
        Intent intent = getIntent();
        if(Intent.ACTION_VIEW.equals(intent.getAction()))
            loadImageFromAction(intent);
        else
            loadImageFromArg(intent);
    }
    private void loadImageFromAction(Intent intent) {
        Uri uri = intent.getData();
        String uriScheme = uri.getScheme();
        if (uriScheme.equalsIgnoreCase("file")) {
            filePath = uri.getPath();
            filePathReal = uri.toString();

            loadImage(filePath, true);
        }
        else if (uriScheme.equalsIgnoreCase("content")) {
            filePath = StorageDirUtils.getViewCachePath() +
                    FileUtils.getFileNameWithExt(uri.getPath());
            filePathReal = uri.toString();

            new Thread(() -> {
                try {
                    InputStream inputStream = getContentResolver().openInputStream(uri);
                    FileOutputStream fileOutputStream = new FileOutputStream(new File(filePath));
                    int index;
                    byte[] bytes = new byte[1024];
                    while ((index = inputStream.read(bytes)) != -1) {
                        fileOutputStream.write(bytes, 0, index);
                        fileOutputStream.flush();
                    }
                    inputStream.close();
                    fileOutputStream.close();

                    runOnUiThread(() -> loadImage(filePath, true));
                } catch (IOException e) {
                    runOnUiThread(() -> showErr(e.toString()));
                }
            }).start();
        }
        else {
            showErr("Bad uri scheme : " + uriScheme);
        }
    }
    private void loadImageFromArg(Intent intent) {

        filePath = intent.getStringExtra("filePath");
        filePathReal = filePath;
        fileList = intent.getCharSequenceArrayListExtra("fileList");

        loadImage(filePath, true);
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

    private int updateFpsTick = 0;
    private final StringBuilder debugString = new StringBuilder();

    private void initRenderer() {

        renderer = new NativeVR720Renderer(handler);
        renderer.setEnableFullChunks(panoEnableFull);
        renderer.setOnRequestGyroValueCallback(quaternion -> orientationSensor1Provider.getQuaternion(quaternion));
        renderer.setCachePath(StorageDirUtils.getViewCachePath());
        renderer.setEnableCache(panoEnableCache);
        glSurfaceView = findViewById(R.id.pano_view);
        glSurfaceView.setNativeRenderer(renderer);
        glSurfaceView.setCaptureCallback(this::screenShotCallback);
        glSurfaceView.setRendererCallback((gl10) -> {
            if(debugEnabled) {
               if(updateFpsTick < 0xffff) {
                   updateFpsTick++;
                   if(updateFpsTick % 20 == 0) {

                       //FPS
                       debugString.append("FPS: ");
                       debugString.append(glSurfaceView.getFps());

                       handler.sendEmptyMessage(MainMessages.MSG_TEST_VAL);
                   }
               } else updateFpsTick = 0;
            }
        });
        glSurfaceView.setDragVelocityEnabled(dragVelocityEnabled);

        startUpdateThread();
    }
    private void initControls() {
        titlebar = findViewById(R.id.titlebar);
        titlebar.setLeftIconOnClickListener((v) -> onBackPressed());
        titlebar.setRightIconOnClickListener((v) -> shareThisImage());

        activity_pano = findViewById(R.id.activity_pano);

        View layout_debug = findViewById(R.id.layout_debug);
        text_debug = findViewById(R.id.text_debug);
        if(!debugEnabled) {
            text_debug.setVisibility(View.GONE);
        }else {
            layout_debug.setVisibility(View.VISIBLE);
        }

        pano_error = findViewById(R.id.pano_error_view);
        pano_error.setVisibility(View.GONE);
        pano_loading = findViewById(R.id.pano_loading_view);
        pano_menu = findViewById(R.id.pano_menu);
        pano_info = findViewById(R.id.pano_info);
        pano_tools = findViewById(R.id.pano_tools);
        pano_info.setVisibility(View.GONE);
        pano_menu.setVisibility(View.GONE);
        pano_tools.setVisibility(View.VISIBLE);
        pano_info.setOnTouchListener((view, motionEvent) -> {
            if (motionEvent.getAction() == MotionEvent.ACTION_UP)
                view.performClick();
            return true;
        });

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
        right_to_left_in = AnimationUtils.loadAnimation(PanoActivity.this, R.anim.right_to_left_in);
        left_to_right_out = AnimationUtils.loadAnimation(PanoActivity.this, R.anim.left_to_right_out);
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
        panoEnableCache = sharedPreferences.getBoolean("enable_view_cache", true);
        panoEnableFull = sharedPreferences.getBoolean("enable_full_chunks", false);
        currentPanoMode = sharedPreferences.getInt("pano_mode", 0);
        vrEnabled = sharedPreferences.getBoolean("pano_enable_vr", false);
        gyroEnabled = sharedPreferences.getBoolean("pano_enable_gyro", false);
        dragVelocityEnabled = sharedPreferences.getBoolean("enable_drag_velocity", true);
        debugEnabled = sharedPreferences.getBoolean("show_debug_tool", false);
        dontCheckImageNormalSize = sharedPreferences.getBoolean("do_not_check_pano_normal_size", false);
        if(sharedPreferences.getBoolean("enable_non_fullscreen", false))
            ScreenUtils.setFullScreen(this, false);
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
                    if(panoMenuShow)
                        setPanoMenuStatus(false, true);
                    if(panoInfoShow)
                        setPanoInfoStatus(false, false);
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

        likeColorStateList = ColorStateList.valueOf(resources.getColor(R.color.colorImageLike, null));

        button_short = findViewById(R.id.button_short);
        button_more = findViewById(R.id.button_more);
        button_like =  findViewById(R.id.button_like);

        button_short.setOnClickListener(v -> {
            if(!fileLoading && fileLoaded)
                screenShot();
        });
        button_more.setOnClickListener(v -> {
            if(!fileLoading && fileLoaded)
                setPanoMenuStatus(!panoMenuShow, true);
        });
        button_like.setOnClickListener(v -> {
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

        findViewById(R.id.button_close_pano_info).setOnClickListener(v -> {
            if(!fileLoading && fileLoaded)
                setPanoInfoStatus(false, true);
        });
        findViewById(R.id.action_view_file_info).setOnClickListener(v -> setPanoInfoStatus(true, true));
        findViewById(R.id.action_delete_file).setOnClickListener(v -> deleteFile());
        findViewById(R.id.action_openwith_text).setOnClickListener(v -> openWithOtherApp());
        findViewById(R.id.button_back).setOnClickListener(v -> closeImage(true));

        findViewById(R.id.layout_info_path).setOnClickListener((v) -> {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData mClipData = ClipData.newPlainText(filePath, filePath);
            cm.setPrimaryClip(mClipData);
            ToastUtils.show(getString(R.string.text_path_copied_to_clipboard));
        });

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
        editor.putBoolean("do_not_check_pano_normal_size", dontCheckImageNormalSize);

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
        text_debug.setText(debugString.toString());
        debugString.delete(0, debugString.length());
    }

    //界面控制
    //**********************

    private Size currentImageSize = new Size(0,0);
    private ImageItem currentImageItem = null;
    private int currentPanoMode = 0;
    private boolean panoEnableCache = true;
    private boolean panoEnableFull = false;
    private boolean vrEnabled = false;
    private boolean gyroEnabled = false;
    private boolean dragVelocityEnabled = true;
    private boolean debugEnabled = true;
    private boolean dontCheckImageNormalSize = true;

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
                button_mode.setCompoundDrawables(null, iconModeRectiliner, null, null);
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
                case NativeVR720Renderer.PanoramaMode_PanoramaOuterBall:
                case NativeVR720Renderer.PanoramaMode_PanoramaCylinder:
                case NativeVR720Renderer.PanoramaMode_PanoramaAsteroid:
                    text_sensor_cant_use_in_mode.setVisibility(View.GONE);
                    break;
                case NativeVR720Renderer.PanoramaMode_PanoramaFull360:
                case NativeVR720Renderer.PanoramaMode_PanoramaFullOrginal:
                case NativeVR720Renderer.PanoramaMode_PanoramaMercator:
                    text_sensor_cant_use_in_mode.setVisibility(View.VISIBLE);
                    break;
            }
        }else {
            text_sensor_cant_use_in_mode.setVisibility(View.GONE);
        }
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
        FileUtils.openFileWithApp(this, filePath);
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
    private void updatePrevNextBtnState() {
        if(fileList == null) {
            currentCanNext = false;
            currentCanPrev = false;
            button_next.setVisibility(View.GONE);
            button_prev.setVisibility(View.GONE);
        } else {
            int currentIndex = findIndexInPathList(filePath);
            currentCanNext = currentIndex < fileList.size() - 1;
            currentCanPrev = currentIndex > 0;
            button_next.setVisibility(currentCanNext ? View.VISIBLE : View.GONE);
            button_prev.setVisibility(currentCanPrev ? View.VISIBLE : View.GONE);
        }
    }
    private void updateLikeButtonState() {
        currentImageItem = listDataService.findImageItem(filePath);
       if(currentImageItem == null) {
           TextViewCompat.setCompoundDrawableTintList(button_like, null);
           button_like.setEnabled(false);
       }else {
           button_like.setEnabled(true);
           TextViewCompat.setCompoundDrawableTintList(button_like,
                   currentImageItem.isInBelongGalleries(ListDataService.GALLERY_LIST_ID_I_LIKE) ?
                           likeColorStateList : null);
       }
    }

    private boolean currentCanNext = false;
    private boolean currentCanPrev = false;
    private LoadingDialog captureLoadingDialog = null;

    private boolean toolbarOn = true;
    private boolean panoInfoShow = false;
    private boolean panoMenuShow = false;
    private boolean isLandscape = false;

    /*
     *  标题栏开启关闭
     */
    private void switchToolBar() {
        if(toolbarOn && pano_tools.getVisibility() != View.VISIBLE)
            setPanoInfoStatus(false, false);
        setToolBarStatus(!toolbarOn);
    }
    private void setToolBarStatus(boolean show) {
        toolbarOn = show;
        if (toolbarOn) {
            pano_tools.startAnimation(bottom_up);
            pano_tools.setVisibility(View.VISIBLE);
            titlebar.startAnimation(top_down);
            titlebar.setVisibility(View.VISIBLE);

            if(currentCanNext) {
                button_next.setVisibility(View.VISIBLE);
                button_next.startAnimation(fade_show);
            }
            if(currentCanPrev) {
                button_prev.setVisibility(View.VISIBLE);
                button_prev.startAnimation(fade_show);
            }

        } else {
            pano_tools.startAnimation(bottom_down);
            pano_tools.setVisibility(View.GONE);
            titlebar.startAnimation(top_up);
            titlebar.setVisibility(View.GONE);

            if(currentCanNext) {
                button_next.setVisibility(View.GONE);
                button_next.startAnimation(fade_hide);
            }
            if(currentCanPrev) {
                button_prev.startAnimation(fade_hide);
                button_prev.setVisibility(View.GONE);
            }
        }
    }
    private void setPanoMenuStatus(boolean show, boolean anim) {
        panoMenuShow = show;
        if(show) {
            if(anim)
                pano_menu.startAnimation(isLandscape ? right_to_left_in : bottom_up);
            pano_menu.setVisibility(View.VISIBLE);
        } else {
            if(anim)
                pano_menu.startAnimation(isLandscape ? left_to_right_out : fade_hide);
            pano_menu.setVisibility(View.GONE);
        }
    }
    private void setPanoInfoStatus(boolean show, boolean anim) {
        panoInfoShow = show;
        if(show) {
            setPanoMenuStatus(false, true);

            if(isLandscape && toolbarOn)
                switchToolBar();
            else {
                pano_tools.startAnimation(fade_hide);
                pano_tools.setVisibility(View.GONE);
            }

            if(anim)
                pano_info.startAnimation(bottom_up);
            pano_info.setVisibility(View.VISIBLE);

        } else {

            if(anim)
                pano_info.startAnimation(isLandscape ? left_to_right_out : bottom_down);
            pano_info.setVisibility(View.GONE);

            if(!isLandscape) {
                if(anim)
                    pano_tools.startAnimation(isLandscape ? right_to_left_in : bottom_up);
                pano_tools.setVisibility(View.VISIBLE);
            }
        }
    }
    private void flushToolStatus() {
        if (toolbarOn) {
            pano_tools.setVisibility(View.VISIBLE);
            titlebar.setVisibility(View.VISIBLE);

            if(currentCanNext) button_next.setVisibility(View.VISIBLE);
            if(currentCanPrev) button_prev.setVisibility(View.VISIBLE);

        } else {
            pano_tools.setVisibility(View.GONE);
            titlebar.setVisibility(View.GONE);

            if(currentCanNext) button_next.setVisibility(View.GONE);
            if(currentCanPrev) button_prev.setVisibility(View.GONE);
        }
        pano_menu.setVisibility(panoMenuShow ? View.VISIBLE : View.GONE);
        pano_info.setVisibility(panoInfoShow ? View.VISIBLE : View.GONE);
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

        if(!ImageUtils.checkSizeIs720Panorama(currentImageSize) && ImageUtils.checkSizeIs320Panorama(currentImageSize))
            currentPanoMode = NativeVR720Renderer.PanoramaMode_PanoramaFull360;

        //设置上次模式
        updateModeButton();
        renderer.setPanoramaMode(currentPanoMode);

        titlebar.setTitle(FileUtils.getFileName(filePath));
        pano_loading.setVisibility(View.GONE);

        setPanoInfoStatus(false, false);
        setPanoMenuStatus(false, false);
        setToolBarStatus(true);
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
                    loadImage(openNextPath, false);
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
                setPanoInfoStatus(false, false);
                setPanoMenuStatus(false, false);
                pano_error.setVisibility(View.GONE);
                break;
            }
            case NativeVR720Renderer.MobileGameUIEvent_UiInfoChanged: break;
            case NativeVR720Renderer.MobileGameUIEvent_DestroyComplete: {
                Log.i(TAG, "DestroyComplete");

                glSurfaceView.forceStop();
                fileLoaded = false;
                fileLoading = false;
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

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        onUpdateScreenOrientation(newConfig);
        AlertDialogTool.notifyConfigurationChangedForDialog(this);
    }

    ConstraintSet mConstraintSet = new ConstraintSet();

    private void onUpdateScreenOrientation(Configuration newConfig) {
        if(newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            isLandscape = false;

            button_like.setTextVisible(true);
            button_short.setTextVisible(true);
            button_mode.setTextVisible(true);
            button_more.setTextVisible(true);

            mConstraintSet.clone(activity_pano);
            mConstraintSet.clear(R.id.pano_info);
            mConstraintSet.clear(R.id.pano_menu);

            mConstraintSet.connect(R.id.pano_menu, ConstraintSet.RIGHT, ConstraintSet.PARENT_ID, ConstraintSet.RIGHT);
            mConstraintSet.connect(R.id.pano_info, ConstraintSet.RIGHT, ConstraintSet.PARENT_ID, ConstraintSet.RIGHT);
            mConstraintSet.connect(R.id.pano_menu, ConstraintSet.LEFT, ConstraintSet.PARENT_ID, ConstraintSet.LEFT);
            mConstraintSet.connect(R.id.pano_info, ConstraintSet.LEFT, ConstraintSet.PARENT_ID, ConstraintSet.LEFT);
            mConstraintSet.connect(R.id.pano_menu, ConstraintSet.BOTTOM, R.id.pano_tools, ConstraintSet.TOP);
            mConstraintSet.connect(R.id.pano_info, ConstraintSet.BOTTOM, R.id.pano_tools, ConstraintSet.TOP);
            mConstraintSet.applyTo(activity_pano);

            ViewGroup.LayoutParams layoutParams = pano_info.getLayoutParams();
            layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
            layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            pano_info.setLayoutParams(layoutParams);
            layoutParams = pano_menu.getLayoutParams();
            layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
            layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            pano_menu.setLayoutParams(layoutParams);

        }
        else if(newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            isLandscape = true;

            button_like.setTextVisible(false);
            button_short.setTextVisible(false);
            button_mode.setTextVisible(false);
            button_more.setTextVisible(false);

            mConstraintSet.clone(activity_pano);
            mConstraintSet.clear(R.id.pano_info);
            mConstraintSet.clear(R.id.pano_menu);
            mConstraintSet.connect(R.id.pano_info, ConstraintSet.RIGHT, ConstraintSet.PARENT_ID, ConstraintSet.RIGHT);
            mConstraintSet.connect(R.id.pano_info, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);
            mConstraintSet.connect(R.id.pano_info, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM);

            mConstraintSet.connect(R.id.pano_menu, ConstraintSet.RIGHT, ConstraintSet.PARENT_ID, ConstraintSet.RIGHT, PixelTool.dp2px(this, 36));
            mConstraintSet.connect(R.id.pano_menu, ConstraintSet.BOTTOM, R.id.pano_tools, ConstraintSet.TOP, 20);

            mConstraintSet.applyTo(activity_pano);

            ViewGroup.LayoutParams layoutParams = pano_info.getLayoutParams();
            layoutParams.width = (int)(screenSize.getHeight() / 2.4);
            layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
            pano_info.setLayoutParams(layoutParams);

            layoutParams = pano_menu.getLayoutParams();
            layoutParams.width = screenSize.getHeight() / 3;
            layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            pano_menu.setLayoutParams(layoutParams);
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

            Size imageSize = new Size(0,0);
            try {
                imageSize = ImageUtils.getImageSize(filePath);
            } catch (IOException e) {
                e.printStackTrace();
            }

            imageInfo.imageFileSize = FileSizeUtil.getAutoFileOrFilesSize(filePath);
            imageInfo.imageTime = DateUtils.format(new Date((new File(filePath)).lastModified()));

            try {
                ExifInterface exifInterface = new ExifInterface(filePath);
                imageInfo.imageTime = exifInterface.getAttribute(ExifInterface.TAG_DATETIME);
                imageInfo.imageSize = exifInterface.getAttribute(ExifInterface.TAG_IMAGE_WIDTH) +
                        "x" + exifInterface.getAttribute(ExifInterface.TAG_IMAGE_LENGTH);

                if(imageInfo.imageSize.equals("x"))
                    imageInfo.imageSize = imageSize.getWidth() + "x" + imageSize.getHeight();

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

            loadImageInfoFinishedListener.onLoadImageInfoFinishedListener(imageSize, isFromInitialization);

            handler.sendEmptyMessage(MainMessages.MSG_LOAD_IMAGE_INFO);
        }).start();
    }
    private void loadImageInfoToUI() {
        text_pano_info_size.setText(imageInfo.imageSize);
        text_pano_info_file_size.setText(imageInfo.imageFileSize);
        text_pano_info_image_time.setText(imageInfo.imageTime);
        text_pano_info_file_path.setText(filePathReal.equals("") ? filePath : filePathReal);
        text_pano_info_shutter_time.setText(imageInfo.imageShutterTime);
        text_pano_info_exposure_bias_value.setText(imageInfo.imageExposureBiasValue);
        text_pano_info_iso_sensitivit.setText(imageInfo.imageISOSensitivity);
    }
    private void loadImage(String path, boolean isFromInitialization) {

        //如果已经打开文件，那么先关闭
        if(renderer.isFileOpen()) {
            openNextPath = path;
            openNextMarked = true;
            closeImage(false);
            return;
        }

        filePath = path;
        currentImageItem = listDataService.findImageItem(filePath);

        //刷新前后按钮状态
        updatePrevNextBtnState();
        updateLikeButtonState();

        if(path == null || path.isEmpty())  {
            showErr(getString(R.string.text_not_provide_path));
            return;
        }

        //加载图片基础信息
        loadImageInfo(isFromInitialization, (size, b) -> {

            currentImageSize = size;

            runOnUiThread(() -> {
                if(ImageUtils.checkSizeIsNormalPanorama(size) || dontCheckImageNormalSize) {
                    //加载
                    renderer.openFile(path);
                } else {
                    //不是正常的全景图，询问用户是否加载
                    new CommonDialog(PanoActivity.this)
                            .setTitle(getString(R.string.text_tip))
                            .setMessage(getString(R.string.text_panorama_size_not_right_warning_text))
                            .setPositive(getString(R.string.action_cancel_load))
                            .setNegative(getString(R.string.action_continue_load))
                            .setCheckText(getString(R.string.text_do_not_ask_me_again))
                            .setOnClickBottomListener(new CommonDialog.OnClickBottomListener() {
                                @Override
                                public void onPositiveClick(CommonDialog dialog) {
                                    dialog.dismiss();
                                    if(isFromInitialization)
                                        finish();
                                    else
                                        showErr(getString(R.string.text_panorama_size_not_right));
                                }
                                @Override
                                public void onNegativeClick(CommonDialog dialog) {
                                    if(dialog.isCheckBoxChecked())
                                        dontCheckImageNormalSize = true;
                                    //继续加载
                                    renderer.openFile(path);
                                    dialog.dismiss();
                                }
                            })
                            .show();
                }
            });

        });
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
            loadImage(fileList.get(currentIndex + 1).toString(), false);
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
        loadImage(fileList.get(currentIndex - 1).toString(), false);
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
