package com.imengyu.vr720.utils;

import android.content.Context;
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

    public static String getCachePath() { return cachePath; }
    public static String getViewCachePath() { return storageDirs[4]; }
    public static String getGalleryCachePath() { return storageDirs[5]; }
    public static String getScreenShotsStoragePath() { return storageDirs[2]; }
    public static String getCacheStoragePath() { return storageDirs[3]; }

    /**
     * 检测并创建存储文件夹
     */
    public static void testAndCreateStorageDirs(Context context) {

        if(Environment.getExternalStorageState().equalsIgnoreCase(Environment.MEDIA_MOUNTED)) {
            cachePath = context.getExternalCacheDir().getAbsolutePath();
            storagePath = context.getExternalFilesDir(null).getAbsolutePath();
        }else {
            cachePath = context.getCacheDir().getAbsolutePath();
            storagePath = "/mnt/sdcard";
        }

        storageDirs[0] = storagePath + "/VR720/";
        storageDirs[1] = storagePath + "/VR720/";
        storageDirs[2] = storagePath + "/VR720/ScreenShots/";
        storageDirs[3] = storagePath + "/VR720/Cache/";
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
