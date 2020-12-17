package com.imengyu.vr720.utils;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import com.imengyu.vr720.PanoActivity;

import java.io.File;

public class StorageDirUtils {

    public static final String TAG = StorageDirUtils.class.getSimpleName();

    private static String storagePath = "";
    private static String cachePath = "";

    private static final String[] storageDirs = new String[6];

    public static String getStoragePath() {
        return storagePath;
    }
    public static String getCachePath() { return cachePath; }
    public static String getViewCachePath() { return storageDirs[4]; }
    public static String getGalleryCachePath() { return storageDirs[5]; }
    public static String getFileStoragePath() { return storageDirs[0]; }

    /**
     * 检测并创建存储文件夹
     */
    public static void testAndCreateStorageDirs(Context context) {

        if(Environment.getExternalStorageState().equalsIgnoreCase(Environment.MEDIA_MOUNTED)) {
            cachePath = context.getExternalCacheDir().getAbsolutePath();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                storagePath = context.getExternalFilesDir(null).getAbsolutePath();
            else
                storagePath = Environment.getExternalStorageDirectory().getAbsolutePath();

        }else {
            cachePath = context.getCacheDir().getAbsolutePath();
            storagePath = "/mnt/sdcard";
        }

        storageDirs[0] = storagePath + "/VR720/";
        storageDirs[1] = "";
        storageDirs[2] = "";
        storageDirs[3] = "";
        storageDirs[4] = cachePath + "/viewCache/";
        storageDirs[5] = cachePath + "/galleryCache/";


        File file = null;
        for(String path : storageDirs)  {
            file = new File(path);
            checkOrCreateDir(file);
        }
    }

    private static void checkOrCreateDir(File file) {
        if (!file.exists() && !file.isDirectory())
            if(!file.mkdir()) Log.w(TAG, "Create directory failed : " + file.getPath());
    }
}
