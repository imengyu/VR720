package com.dreamfish.com.vr720;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.dreamfish.com.vr720.utils.FileSizeUtil;
import com.dreamfish.com.vr720.utils.FileUtils;
import com.dreamfish.com.vr720.widget.ToolbarButton;


import java.io.File;
import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public class PanoActivity extends AppCompatActivity {

    /** Tag for logging. */
    public static final String TAG = PanoActivity.class.getSimpleName();

    private Context mContext;
    private Resources resources;

    private View pano_view;
    private View pano_title;
    private View pano_bar;
    private View pano_error;
    private View pano_loading;
    private TextView textview_pano_error;
    private TextView textview_pano_title;

    private Button button_mode;
    private ToolbarButton button_vr;
    private ToolbarButton button_gryo;

    private String filePath;


    private Drawable iconModeBall;
    private Drawable iconModeLittlePlanet;
    private Drawable iconModeRectiliner;
    private Drawable iconModeSource;
    private Drawable iconModeVideoBall;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null)
            getSupportActionBar().hide();
        setContentView(R.layout.activity_pano);

        resources = getResources();
        mContext = getApplicationContext();
        filePath = getIntent().getStringExtra("filePath");

        pano_view= findViewById(R.id.pano_view);
        pano_bar = findViewById(R.id.pano_toolbar_view);
        pano_title = findViewById(R.id.pano_title_view);
        pano_error = findViewById(R.id.pano_error_view);
        pano_error.setVisibility(View.GONE);
        pano_loading = findViewById(R.id.pano_loading_view);
        textview_pano_error = findViewById(R.id.textview_pano_error);
        textview_pano_title= findViewById(R.id.textview_pano_title);

        pano_view.setOnClickListener(v -> SwitchToolBar());
        findViewById(R.id.button_pano_back).setOnClickListener(v -> finish());
        findViewById(R.id.button_more).setOnClickListener(v -> showMore());

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


        //初始化PanoSDK

        button_gryo = findViewById(R.id.button_gryo);
        button_vr = findViewById(R.id.button_vr);
        button_mode = findViewById(R.id.button_mode);
        button_mode.setOnClickListener(v -> changeMode());

        //加载图片基础信息
        new Thread(() -> {
            try {
                imageFileSize = FileSizeUtil.getAutoFileOrFilesSize(filePath);

                FileInputStream fis;
                fis = new FileInputStream(filePath);
                BitmapFactory.Options opt = new BitmapFactory.Options();
                opt.inJustDecodeBounds = true;

                BitmapFactory.decodeFile(filePath, opt);
                Bitmap bitmap = BitmapFactory.decodeStream(fis,null, opt);

                if(bitmap!=null) {
                    imageSize = bitmap.getWidth() + "x" + bitmap.getHeight();
                    long fileTime = (new File(filePath)).lastModified();
                    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss", Locale.CHINA);
                    imageTime = format.format(new Date(fileTime));
                }

                fis.close();

            } catch (Exception e) {
                e.printStackTrace();

            }
        }).start();
    }

    private String imageSize = "";
    private String imageTime = "";
    private String imageFileSize = "";

    //按钮事件
    //**********************


    private void changeMode(){

    }

    private void showMore(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(resources.getString(R.string.text_more_actions));
        builder.setItems(new CharSequence[]{
                getString(R.string.action_view_file_info),
                getString(R.string.action_openwith_text),
                getString(R.string.action_delete_file),
                getString(R.string.action_back_main),
        }, (dialog, which) -> {
            switch (which){
                case 0: showFileInfo(); break;
                case 1: openWithOtherApp(); break;
                case 2: deleteFile(); break;
                case 3: finish(); break;
            }
        });
        builder.setNegativeButton(resources.getString(R.string.action_cancel),null);
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }
    private void deleteFile() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(R.mipmap.ic_launcher_round);
        builder.setTitle(getString(R.string.text_sure_delete_pano));
        builder.setMessage(filePath);
        builder.setPositiveButton(resources.getString(R.string.action_sure_delete), (dialog, which) -> {
            if(FileUtils.deleteFile(filePath)) {
                Toast.makeText(PanoActivity.this, getString(R.string.text_delete_failed), Toast.LENGTH_SHORT).show();
                finish();
            }else Toast.makeText(PanoActivity.this, getString(R.string.text_delete_success), Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton(resources.getString(R.string.action_cancel),null);
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }
    private void openWithOtherApp() {
        FileUtils.openFile(this, filePath);
    }
    private void showFileInfo() {

        CharSequence[] fileInfo = new CharSequence[4];
        fileInfo[0] = getString(R.string.text_file_path) + filePath;
        fileInfo[1] = getString(R.string.text_file_size) + imageFileSize;
        fileInfo[2] = getString(R.string.text_image_size) + imageSize;
        fileInfo[3] = getString(R.string.text_image_date) + imageTime;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.text_file_info));
        builder.setItems(fileInfo, (dialog, which) -> {});
        builder.setPositiveButton(resources.getString(R.string.action_ok),null);
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    //标题栏开启关闭与全屏
    //**********************

    private boolean disableToolbar = false;
    private boolean toolbarOn = true;
    private void SwitchFullScreen(boolean full){
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
    private void DisableToolBar() {
        pano_bar.setVisibility(View.GONE);
        disableToolbar=false;
    }
    private void SwitchToolBar(){
        if(!disableToolbar) {
            toolbarOn = !toolbarOn;
            SwitchFullScreen(!toolbarOn);
            if (toolbarOn) {
                pano_bar.startAnimation(AnimationUtils.loadAnimation(PanoActivity.this, R.anim.bottom_up));
                pano_title.startAnimation(AnimationUtils.loadAnimation(PanoActivity.this, R.anim.top_down));
                pano_bar.setVisibility(View.VISIBLE);
                pano_title.setVisibility(View.VISIBLE);
            } else {
                pano_bar.startAnimation(AnimationUtils.loadAnimation(PanoActivity.this, R.anim.buttom_down));
                pano_title.startAnimation(AnimationUtils.loadAnimation(PanoActivity.this, R.anim.top_up));
                pano_bar.setVisibility(View.GONE);
                pano_title.setVisibility(View.GONE);
            }
        }
    }


    //重载相关响应函数
    //**********************



    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {
        super.onPointerCaptureChanged(hasCapture);
    }
    public void onSDKIsReady() {
        Log.d(TAG, "SDK is ok");
        Log.d(TAG, "Load image file : " + filePath);

        textview_pano_title.setText(FileUtils.getFileName(filePath));
        pano_loading.setVisibility(View.GONE);
    }

}
