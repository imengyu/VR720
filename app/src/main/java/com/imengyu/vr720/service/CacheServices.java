package com.imengyu.vr720.service;

import android.content.Context;
import android.util.Log;

import com.imengyu.vr720.utils.FileUtils;
import com.imengyu.vr720.utils.StorageDirUtils;

import java.io.File;

public class CacheServices {

    public static final String TAG = CacheServices.class.getSimpleName();

    private final Context context;

    public CacheServices(Context context) {
        this.context = context;
    }

    private String lastCacheDirSize = null;

    /**
     * 统计缓存目录的大小
     * @return 缓存目录的大小
     */
    public String getCacheDirSize() {
        if(lastCacheDirSize == null) {
            String path = StorageDirUtils.getCachePath();
            long size = FileUtils.getDirSize(new File(path));
            lastCacheDirSize = FileUtils.getReadableFileSize(size);
        }
        return lastCacheDirSize;
    }

    /**
     * 清理缓存目录
     */
    public void clearCacheDir() {

        int succesCount = 0;
        int allCount = 0;

        File fileDir = new File(StorageDirUtils.getCachePath());
        File[] files = fileDir.listFiles();
        for (File file : files) {
            if (file.isFile()) {
                allCount++;
                try {
                    if (file.delete())
                        succesCount++;
                } catch (Exception e) {
                    Log.e(TAG, String.format("Delete failed in CacheDir, path : %s, err : %s", file.getPath(), e.toString()));
                }
            }
            else {
                File[] innerFiles = fileDir.listFiles();
                for (File innerFile : innerFiles) {
                    allCount++;
                    try {
                        if (innerFile.delete())
                            succesCount++;
                    } catch (Exception e) {
                        Log.e(TAG, String.format("Delete failed in CacheDir, path : %s, err : %s", innerFile.getPath(), e.toString()));
                    }
                }
            }
        }

        lastCacheDirSize = null;

        Log.i(TAG, String.format("Do clearCacheDir, collected : %d, deleted : %d", allCount, succesCount));
    }

}
