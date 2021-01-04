package com.imengyu.vr720;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.hjq.toast.ToastUtils;
import com.imengyu.vr720.dialog.CommonDialog;
import com.imengyu.vr720.utils.FileUtils;
import com.imengyu.vr720.utils.StorageDirUtils;
import com.imengyu.vr720.utils.StringUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class OpenFileActivity extends AppCompatActivity {

    private static final String TAG = OpenFileActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_file);

        VR720Application application = ((VR720Application)getApplication());

        readArg((path, argPath, isInCache) -> {

            Intent intent;
            if(!application.isInitFinish()) {
                //app 没有完全启动，现在返回 LunchActivity 进行初始化
                intent = new Intent(this, LunchActivity.class);
            } else {
                //app 启动了，跳转到MainActivity
                intent = new Intent(this, MainActivity.class);
            }

            intent.putExtra("openFilePath", path);
            intent.putExtra("openFileArgPath", argPath);
            intent.putExtra("openFileIsInCache", isInCache);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }

    private interface ReadArgCallback {
        void onReadArg(String path, String argPath, boolean isInCache);
    }

    private void readArg(ReadArgCallback readArgCallback) {
        //读取参数
        Intent intent = getIntent();
        //读取输入路径
        Uri uri = intent.getData();
        if(uri == null) {
            ToastUtils.show("Bad Uri");
            finish();
            return;
        }
        String uriScheme = uri.getScheme();
        if (uriScheme.equalsIgnoreCase("file")) {
            readArgCallback.onReadArg(uri.getPath(), uri.toString(), false);
        } else if (uriScheme.equalsIgnoreCase("content")) {

            //尝试获取真实路径
            String realPath = FileUtils.getContentFilePath(this, uri);
            if(!StringUtils.isNullOrEmpty(realPath) && new File(realPath).exists()) {
                readArgCallback.onReadArg(realPath, realPath, false);
                return;
            }

            //如果获取失败，则保存至缓存再打开查看
            StorageDirUtils.testAndCreateStorageDirs(this);
            String filePath = StorageDirUtils.getViewCachePath() +
                    FileUtils.getFileNameWithExt(uri.getPath());
            String filePathReal = uri.toString();

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
                    readArgCallback.onReadArg(filePath, filePathReal, true);
                } catch (IOException e) {
                    runOnUiThread(() -> {
                        Log.e(TAG, "Load Content failed! ", e.fillInStackTrace());
                        new CommonDialog(OpenFileActivity.this)
                                .setTitle(R.string.text_failed)
                                .setImageResource(R.drawable.ic_warning)
                                .setMessage(e.toString())
                                .show();
                    });
                }
            }).start();
        } else {
            finish();
        }
    }
}