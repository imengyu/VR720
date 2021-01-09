package com.imengyu.vr720.service;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.imengyu.vr720.config.Constants;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ErrorUploadService {

    private static final String TAG = ErrorUploadService.class.getSimpleName();
    private boolean checking = false;

    public interface OnCheckErrorCallback {
        void onCheckError(boolean lastHasError, String path);
    }
    public interface OnUploadErrorCallback {
        void onCheckError(boolean success, String error);
    }
    private interface OnUploadCallback {
        void onUpdateResult(boolean success, String error);
    }

    public void check(Context context, OnCheckErrorCallback callback) {

        if(checking)
            return;

        checking = true;

        new Thread(() -> {

            String path = context.getCacheDir() + "/errorLogs";

            File fileTarget = null;
            File[] files = new File(path).listFiles();
            if(files!=null) {
                for (File file : files) {
                    if (file.getName().endsWith(".trace.txt")) {
                        fileTarget = file;
                        break;
                    }
                }
            }
            if(callback != null)
                callback.onCheckError(fileTarget != null,
                        fileTarget != null ? fileTarget.getAbsolutePath() : null);

            checking = false;
        }).start();
    }

    private int currentIndex = 0;

    public void doUpload(Context context, OnUploadErrorCallback callback) {
        String path = context.getCacheDir() + "/errorLogs";
        File[] files = new File(path).listFiles();
        currentIndex = 0;
        doUploadLoop(files, callback);
    }
    private void doUploadClear(File[] files) {
        for(File f : files) {
            try {
                if (!f.delete())
                    Log.w(TAG, "Delete file failed : " + f.getPath());
            }catch (Exception e) {
                Log.w(TAG, "Delete file failed : " + f.getPath() + " Error: " + e.toString());
            }
        }
    }
    private void doUploadLoop(File[] files, OnUploadErrorCallback callback) {
        uploadFile(files[currentIndex], (success,err) -> {
            if(success) {
                if(currentIndex < files.length - 1) {
                    currentIndex++;
                    doUploadLoop(files, callback);
                } else {
                    callback.onCheckError(true, null);
                }
            } else {
                doUploadClear(files);
                callback.onCheckError(false, err);
            }
        });
    }
    private void uploadFile(File file, OnUploadCallback uploadCallback) {
        RequestBody fileBody = RequestBody.create(MediaType.parse("application/octet-stream"), file);
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("application/octet-stream", file.getName(), fileBody)
                .build();
        Request request = new Request.Builder()
                .url(Constants.ERROR_FEED_BACK_URL)
                .post(requestBody)
                .build();

        final okhttp3.OkHttpClient.Builder httpBuilder = new OkHttpClient.Builder();
        OkHttpClient okHttpClient  = httpBuilder
                //设置超时
                .connectTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(50, TimeUnit.SECONDS)
                .build();
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try {
                    JSONObject jsonObject = JSON.parseObject(response.body().string());
                    if (jsonObject.getBoolean("success"))
                        uploadCallback.onUpdateResult(true, null);
                    else
                        uploadCallback.onUpdateResult(false, jsonObject.getString("message"));
                }catch (Exception e) {
                    uploadCallback.onUpdateResult(false, e.toString());
                }
            }
            @Override
            public void onFailure(@NonNull Call arg0, @NonNull IOException e) {
                uploadCallback.onUpdateResult(false, e.toString());
            }
        });

    }

}
