package com.imengyu.vr720.service;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ThumbnailUtils;
import android.os.Build;
import android.util.Log;
import android.util.Size;

import com.imengyu.vr720.model.ImageCacheData;
import com.imengyu.vr720.utils.FileUtils;
import com.imengyu.vr720.utils.ImageUtils;
import com.imengyu.vr720.utils.MD5Utils;
import com.imengyu.vr720.utils.StorageDirUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ListImageCacheService {

    public static final String TAG = ListImageCacheService.class.getSimpleName();

    private final Context context;

    public ListImageCacheService(Context context) {
        this.context = context;
    }

    private int cacheThumbnailMaxSize = 128;
    private final Map<String, ImageCacheData> cacheThumbnailDataMap = new HashMap<>();
    private final List<ImageCacheData> cacheThumbnailDataLive = new ArrayList<>();

    public int getCacheThumbnailMaxSize() {
        return cacheThumbnailMaxSize;
    }
    public void setCacheThumbnailMaxSize(int cacheThumbnailMaxSize) {
        this.cacheThumbnailMaxSize = cacheThumbnailMaxSize;
    }

    private String makeCacheThumbnailPath(String path) {
        return StorageDirUtils.getGalleryCachePath() + MD5Utils.md5(path);
    }

    /**
     * 尝试读取图片已经缓存的缓存文件路径
     * @param path 图片路径
     * @return 缓存文件路径，如果没有，则返回null
     */
    public String tryGetImageThumbnailCacheFilePath(String path) {
        String cacheThumbnailPath = StorageDirUtils.getGalleryCachePath() + MD5Utils.md5(path);
        if(new File(cacheThumbnailPath).exists())
            return cacheThumbnailPath;
        return null;
    }

    /**
     * 加载图片缩略图并存入缓存系统
     * @param path 图片路径
     * @return 返回图片缩略图
     */
    public synchronized Drawable loadImageThumbnailCache(String path) {

        if(path == null || path.isEmpty())
            return null;

        if(!new File(path).exists())
            return null;

        if(cacheThumbnailDataMap.containsKey(path)) {
            ImageCacheData cacheData = cacheThumbnailDataMap.get(path);
            if(cacheData != null) {
                cacheData.cacheUseCount++;
                if (cacheData.isFastCache)
                    return cacheData.fastCache;
                else if (cacheData.isFileCache) {
                    BitmapDrawable drawable = new BitmapDrawable(context.getResources(), ImageUtils.loadBitmap(cacheData.cacheImagePath));
                    if (cacheData.cacheUseCount > 10) {
                        cacheData.fastCache = drawable;
                        cacheData.isFastCache = true;
                    }
                    return drawable;
                }
            }
        }

        cutThumbnailCache();

        if(FileUtils.getFileIsVideo(path)) {

            String cacheThumbnailPath = makeCacheThumbnailPath(path);
            File cacheThumbnailFile = new File(cacheThumbnailPath);
            if(cacheThumbnailFile.exists())
                return fastLoadCacheToThumbnail(path, cacheThumbnailPath, false, true);

            Bitmap videoThumbnail;
            //保存缩略图至缓存文件
            FileOutputStream saveImgOut;
            try {
                saveImgOut = new FileOutputStream(cacheThumbnailFile);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    videoThumbnail = ThumbnailUtils.createVideoThumbnail(new File(path), new Size(300, 150), null);
                    if(videoThumbnail.getHeight() == videoThumbnail.getWidth() && videoThumbnail.getWidth() == -1)
                        return null;
                } else
                    return null;

                videoThumbnail.compress(Bitmap.CompressFormat.JPEG, 80, saveImgOut);
                saveImgOut.flush();
                saveImgOut.close();
            } catch (Exception e) {
                Log.e(TAG, String.format("Save video thumbnail cache [%s] failed ! %s", cacheThumbnailPath, e.toString()));
                return null;
            }

            ImageCacheData cacheData = new ImageCacheData();
            cacheData.isFastCache = false;
            cacheData.filePath = path;
            cacheData.cacheImagePath = cacheThumbnailPath;
            cacheData.cacheUseCount = 1;
            cacheData.isFileCache = true;
            cacheThumbnailDataLive.add(cacheData);
            cacheThumbnailDataMap.put(path, cacheData);
            return new BitmapDrawable(context.getResources(), videoThumbnail);
        }

        //获取图像大小
        Size imSize;
        try {
            imSize = ImageUtils.getImageSize(path);
            if(imSize.getWidth() == -1 && imSize.getHeight() == -1) {
                Log.e(TAG, String.format("Try get image size for thumbnail [%s] failed !", path));
                return null;
            }
        }
        catch (IOException e) {
            Log.e(TAG, String.format("Try get image size for thumbnail [%s] failed ! %s", path, e.toString()));
            return null;
        }

        //图像太小，直接使用原图
        if(imSize.getWidth() < 400 && imSize.getHeight() < 800)
            return fastLoadCacheToThumbnail(path, path, true, false);

        //如果图像非常大，那么会保存缩略图至文件缓存中，方便下次读取
        if(imSize.getWidth() >= 4096 || imSize.getHeight() > 2048) {

            //已经有文件缓存，直接读取
            String cacheThumbnailPath = makeCacheThumbnailPath(path);
            File cacheThumbnailFile = new File(cacheThumbnailPath);
            if(cacheThumbnailFile.exists())
                return fastLoadCacheToThumbnail(path, cacheThumbnailPath, false, true);

            Bitmap thumbnail = ImageUtils.revitionImageSize(path, 600, 300);
            if(thumbnail == null) return null;

            //保存缩略图至缓存文件
            FileOutputStream saveImgOut;
            try {
                saveImgOut = new FileOutputStream(cacheThumbnailFile);
                thumbnail.compress(Bitmap.CompressFormat.JPEG, 80, saveImgOut);
                saveImgOut.flush();
                saveImgOut.close();
            } catch (Exception e) {
                Log.e(TAG, String.format("Save thumbnail cache [%s] failed ! %s", cacheThumbnailPath, e.toString()));
                return null;
            }

            ImageCacheData cacheData = new ImageCacheData();
            cacheData.isFastCache = false;
            cacheData.filePath = path;
            cacheData.cacheImagePath = cacheThumbnailPath;
            cacheData.cacheUseCount = 1;
            cacheData.isFileCache = true;
            cacheThumbnailDataLive.add(cacheData);
            cacheThumbnailDataMap.put(path, cacheData);
            return new BitmapDrawable(context.getResources(), thumbnail);
        }

        //普通加载模式
        {
            Bitmap thumbnail = ImageUtils.revitionImageSize(path, 600, 300);
            if (thumbnail == null)
                return null;

            ImageCacheData cacheData = new ImageCacheData();
            cacheData.isFastCache = false;
            cacheData.isFileCache = false;
            cacheData.cacheUseCount = 0;
            cacheData.filePath = path;
            cacheThumbnailDataLive.add(cacheData);
            cacheThumbnailDataMap.put(path, cacheData);
            return new BitmapDrawable(context.getResources(), thumbnail);
        }
    }

    /**
     * 快速加载缩略图并设置缓存
     * @param path 文件路径
     * @param cachePath 缩略图文件路径
     * @return 返回缩略图
     */
    private Drawable fastLoadCacheToThumbnail(String path, String cachePath, boolean fast, boolean file) {
        Bitmap bitmap = ImageUtils.loadBitmap(cachePath);
        if(bitmap == null || bitmap.getWidth() <= 0 || bitmap.getHeight() <= 0)
            return  null;

        BitmapDrawable drawable = new BitmapDrawable(context.getResources(), bitmap);
        ImageCacheData cacheData = new ImageCacheData();
        cacheData.isFastCache = fast;
        cacheData.isFileCache = file;
        cacheData.fastCache = drawable;
        cacheData.filePath = path;
        cacheData.cacheImagePath = cachePath;
        cacheThumbnailDataLive.add(cacheData);
        cacheThumbnailDataMap.put(path, cacheData);
        return drawable;
    }
    /**
     * 缓存大小超过了最大限制，开始清理
     */
    private synchronized void cutThumbnailCache() {
        for (int i = cacheThumbnailDataLive.size() - 1; i >= 0; i--) {
            cacheThumbnailDataLive.get(i).cacheUseCount--;
        }
        int removeCount = cacheThumbnailDataLive.size() - cacheThumbnailMaxSize;
        if(removeCount > 0) {
            Collections.sort(cacheThumbnailDataLive, (o1, o2) -> -Integer.compare(o1.cacheUseCount, o2.cacheUseCount));
            for (int i = removeCount - 1; i >= 0; i--) {
                cacheThumbnailDataMap.remove(cacheThumbnailDataLive.get(i).filePath);
                cacheThumbnailDataLive.remove(i);
            }
        }
    }

    /**
     * 清除所有图像缓存
     */
    public void clearAllImageCache() {
        ImageCacheData data;
        for (int i = cacheThumbnailDataLive.size() - 1; i >= 0; i--) {
            data = cacheThumbnailDataLive.get(i);
            if(data.isFileCache) {
                try {
                    File file = new File(data.cacheImagePath);
                    if (file.exists() && file.canWrite() && !file.delete())
                        Log.w(TAG, String.format("Delete image cache %s failed !", data.cacheImagePath));
                }catch (Exception e) {
                    Log.w(TAG, String.format("Delete image cache %s failed ! Error : %s", data.cacheImagePath, e.toString()));
                }
            }
        }
    }

    public void releaseImageCache() {
        cacheThumbnailDataLive.clear();
        cacheThumbnailDataMap.clear();
    }

    /**
     * 释放保存在内存中的缓存
     */
    public void releaseAllMemoryCache() {
        ImageCacheData data;
        for (int i = cacheThumbnailDataLive.size() - 1; i >= 0; i--) {
            data = cacheThumbnailDataLive.get(i);
            //清理掉较少使用的缓存
            if (data.cacheUseCount <= 0) {
                cacheThumbnailDataMap.remove(data.filePath);
                cacheThumbnailDataLive.remove(i);
            } else {
                if(data.isFastCache) {
                    data.isFastCache = false;
                    data.fastCache = null;
                }
            }
        }
    }

}
