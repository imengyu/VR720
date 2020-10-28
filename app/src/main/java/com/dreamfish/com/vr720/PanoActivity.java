package com.dreamfish.com.vr720;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.dreamfish.com.vr720.config.MainMessages;
import com.dreamfish.com.vr720.core.NativeVR720Renderer;
import com.dreamfish.com.vr720.core.RendererWrapper;
import com.dreamfish.com.vr720.dialog.CommonDialog;
import com.dreamfish.com.vr720.core.NativeVR720;
import com.dreamfish.com.vr720.utils.DateUtils;
import com.dreamfish.com.vr720.utils.FileSizeUtil;
import com.dreamfish.com.vr720.utils.FileUtils;
import com.dreamfish.com.vr720.utils.StatusBarUtils;
import com.dreamfish.com.vr720.widget.MyTitleBar;
import com.dreamfish.com.vr720.widget.ToolbarButton;


import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Date;


public class PanoActivity extends AppCompatActivity {

    static {
        System.loadLibrary("vr720");
    }

    /** Tag for logging. */
    public static final String TAG = PanoActivity.class.getSimpleName();

    private Context mContext;
    private Resources resources;

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

    private MyTitleBar myTitleBar;

    private Button button_mode;
    private ToolbarButton button_vr;
    private ToolbarButton button_gryo;

    private String filePath;

    private boolean rendererSet = false;
    private GLSurfaceView glSurfaceView;

    private Drawable iconModeBall;
    private Drawable iconModeLittlePlanet;
    private Drawable iconModeRectiliner;
    private Drawable iconModeSource;
    private Drawable iconModeVideoBall;

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
        initControls();
        initButtons();

        //初始化内核
        if(!NativeVR720Renderer.checkSupportsEs2(this)) {
            showErr(getString(R.string.text_your_device_dosnot_support_es20));
            return;
        }
        if(!NativeVR720.initNative()) {
            showErr(getString(R.string.text_core_init_failed));
            return;
        }

        //加载图片基础信息
        loadImageInfo();
        //加载图片
        loadImage();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        NativeVR720.releaseNative();
    }

    private String imageSize = "";
    private String imageTime = "";
    private String imageFileSize = "";
    private String imageShutterTime = "0s";
    private String imageExposureBiasValue = "0";
    private String imageISOSensitivity = "0";

    private NativeVR720Renderer renderer;

    private void initRenderer() {
        renderer = new NativeVR720Renderer();
        glSurfaceView = findViewById(R.id.pano_view);
        glSurfaceView.setRenderer(new RendererWrapper(renderer));
    }
    private void initControls() {
        myTitleBar = findViewById(R.id.myTitleBar);

        pano_bar = findViewById(R.id.pano_toolbar_view);
        pano_error = findViewById(R.id.pano_error_view);
        pano_error.setVisibility(View.GONE);
        pano_loading = findViewById(R.id.pano_loading_view);
        pano_menu = findViewById(R.id.pano_menu);
        pano_info = findViewById(R.id.pano_info);
        pano_tools = findViewById(R.id.pano_tools);
        pano_info.setVisibility(View.GONE);
        pano_menu.setVisibility(View.GONE);

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
        iconModeBall = resources.getDrawable(R.drawable.ic_bar_projection_ball, null);
        iconModeLittlePlanet = resources.getDrawable(R.drawable.ic_bar_projection_little_planet, null);
        iconModeRectiliner = resources.getDrawable(R.drawable.ic_bar_projection_rectilinear, null);
        iconModeSource = resources.getDrawable(R.drawable.ic_bar_projection_source, null);
        iconModeVideoBall = resources.getDrawable(R.drawable.ic_bar_projection_video_ball, null);
        iconModeBall.setBounds(0,0,iconModeBall.getMinimumWidth(),iconModeBall.getMinimumHeight());
        iconModeLittlePlanet.setBounds(0,0,iconModeLittlePlanet.getMinimumWidth(),iconModeLittlePlanet.getMinimumHeight());
        iconModeRectiliner.setBounds(0,0,iconModeRectiliner.getMinimumWidth(),iconModeRectiliner.getMinimumHeight());
        iconModeSource.setBounds(0,0,iconModeSource.getMinimumWidth(),iconModeSource.getMinimumHeight());
        iconModeVideoBall.setBounds(0,0,iconModeVideoBall.getMinimumWidth(),iconModeVideoBall.getMinimumHeight());
    }
    private void initButtons() {

        button_gryo = findViewById(R.id.button_gryo);
        button_vr = findViewById(R.id.button_vr);
        button_mode = findViewById(R.id.button_mode);
        button_mode.setOnClickListener(v -> changeMode());

        glSurfaceView.setOnClickListener(v -> switchToolBar());
        myTitleBar.setLeftIconOnClickListener(v -> finish());

        findViewById(R.id.button_more).setOnClickListener(v -> showMore());
        findViewById(R.id.button_close_pano_info).setOnClickListener(v -> showTools());
        findViewById(R.id.action_view_file_info).setOnClickListener(v -> showFileInfo());
        findViewById(R.id.action_delete_file).setOnClickListener(v -> deleteFile());
        findViewById(R.id.action_openwith_text).setOnClickListener(v -> openWithOtherApp());
        findViewById(R.id.action_back_main).setOnClickListener(v -> finish());
    }

    //界面控制
    //**********************

    private void changeMode() {

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
            pano_menu.startAnimation(AnimationUtils.loadAnimation(PanoActivity.this, R.anim.buttom_down));
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
                        Toast.makeText(PanoActivity.this, getString(R.string.text_delete_failed), Toast.LENGTH_SHORT).show();
                        finish();
                    }else Toast.makeText(PanoActivity.this, getString(R.string.text_delete_success), Toast.LENGTH_SHORT).show();
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
        pano_bar.setVisibility(View.GONE);
        text_pano_error.setText(err);
    }

    //标题栏开启关闭与全屏
    //**********************

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
        disableToolbar=false;
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
                myTitleBar.startAnimation(AnimationUtils.loadAnimation(PanoActivity.this, R.anim.top_down));
                pano_bar.setVisibility(View.VISIBLE);
                myTitleBar.setVisibility(View.VISIBLE);
            } else {
                pano_bar.startAnimation(AnimationUtils.loadAnimation(PanoActivity.this, R.anim.buttom_down));
                myTitleBar.startAnimation(AnimationUtils.loadAnimation(PanoActivity.this, R.anim.top_up));
                pano_bar.setVisibility(View.GONE);
                myTitleBar.setVisibility(View.GONE);
            }
        }
    }


    //主线程handler
    //**********************

    private static class MainHandler extends Handler {
        private final WeakReference<PanoActivity> mTarget;

        MainHandler(PanoActivity target) {
            mTarget = new WeakReference<>(target);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MainMessages.MSG_TEST_LIST:

                    break;
                case MainMessages.MSG_LOAD_IMAGE_INFO:
                    mTarget.get().loadFileInfo();
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
    public void onRendererIsReady() {
        Log.d(TAG, "Renderer is ok");
        Log.d(TAG, "Load image file : " + filePath);

        myTitleBar.setTitle(FileUtils.getFileName(filePath));
        pano_loading.setVisibility(View.GONE);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (rendererSet) {
            glSurfaceView.onPause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (rendererSet) {
            glSurfaceView.onResume();
        }
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
                imageISOSensitivity = exifInterface.getAttribute(ExifInterface.TAG_ISO);
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
    private void loadImage() {

    }
}
