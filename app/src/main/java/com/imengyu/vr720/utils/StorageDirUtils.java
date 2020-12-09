package com.imengyu.vr720.utils;

import android.os.Environment;

import java.io.File;

public class StorageDirUtils {

    public static final String STORAGE_PATH =
            Environment.getExternalStorageState().equalsIgnoreCase(Environment.MEDIA_MOUNTED) ?
                    Environment.getExternalStorageDirectory().getAbsolutePath() : "/mnt/sdcard";//保存到SD卡

    /**
     * 检测并创建存储文件夹
     */
    public static void testAndCreateStorageDirs() {
        File file = new File(STORAGE_PATH + "/VR720");
        if (!file.exists() && !file.isDirectory())
            file.mkdir();
        file = new File(STORAGE_PATH + "/VR720/ScreenShots");
        if (!file.exists() && !file.isDirectory())
            file.mkdir();
        file = new File(STORAGE_PATH + "/VR720/Cache");
        if (!file.exists() && !file.isDirectory())
            file.mkdir();
    }

}
