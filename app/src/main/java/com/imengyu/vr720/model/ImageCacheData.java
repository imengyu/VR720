package com.imengyu.vr720.model;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

public class ImageCacheData {
    public String filePath;
    public boolean isFastCache;
    public boolean isFileCache;
    public int cacheUseCount = 0;
    public Drawable fastCache;
    public String cacheImagePath;
}
