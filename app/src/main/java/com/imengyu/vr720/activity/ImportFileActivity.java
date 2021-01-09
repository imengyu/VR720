package com.imengyu.vr720.activity;

import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.hjq.toast.ToastUtils;
import com.imengyu.vr720.R;
import com.imengyu.vr720.VR720Application;
import com.imengyu.vr720.utils.FileUtils;
import com.imengyu.vr720.utils.StringUtils;

import java.io.File;
import java.util.ArrayList;

public class ImportFileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import_file);
        intent = getIntent();

        new Thread(() -> {
            try { Thread.sleep(400); } catch (InterruptedException e) { e.printStackTrace(); }
            runOnUiThread(this::doImport);
        }).start();
    }

    private Intent intent = null;

    private void doImport() {
        if(Intent.ACTION_SEND.equals(intent.getAction())) {
            Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            if(uri != null) {
                Uri[] arr = new Uri[1];
                arr[0] = uri;
                goImport(arr);
            } else ToastUtils.show("Intent.EXTRA_STREAM not set");
        } else if(Intent.ACTION_SEND_MULTIPLE.equals(intent.getAction())) {
            ClipData clipData = intent.getClipData();
            if(clipData != null && clipData.getItemCount() > 0) {
                Uri[] arr = new Uri[clipData.getItemCount()];
                for (int i = 0; i < clipData.getItemCount(); i++)
                    arr[i] = clipData.getItemAt(i).getUri();
                goImport(arr);
            } else ToastUtils.show("No file was provided");
        } else ToastUtils.show("Bad Action");
        finish();
    }

    private void goImport(Uri[] arr) {

        VR720Application application = ((VR720Application)getApplication());

        Intent intent;
        if(application.isNotInit()) {
            //app 没有完全启动，现在返回 LunchActivity 进行初始化
            intent = new Intent(this, LunchActivity.class);
        } else {
            //app 启动了，跳转到MainActivity
            intent = new Intent(this, MainActivity.class);
        }

        ArrayList<CharSequence> pathArr = new ArrayList<>();
        for(Uri uri : arr) {
            //尝试获取真实路径
            String realPath = FileUtils.getContentFilePath(this, uri);
            if(!StringUtils.isNullOrEmpty(realPath) && new File(realPath).exists())
                pathArr.add(realPath);
        }

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("importCount", arr.length);
        intent.putCharSequenceArrayListExtra("importList", pathArr);

        startActivity(intent);
    }

}