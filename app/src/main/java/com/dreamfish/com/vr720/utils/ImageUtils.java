package com.dreamfish.com.vr720.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Size;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class ImageUtils
{
    /**
     * 根据指定的图像路径和大小来获取缩略图
     *
     * @param path      图像的路径
     * @param maxWidth  指定输出图像的宽度
     * @param maxHeight 指定输出图像的高度
     * @return 生成的缩略图
     */
    public static Bitmap revitionImageSize(String path, int maxWidth, int maxHeight) throws IOException {
        Bitmap bitmap = null;
        try {
            BufferedInputStream in = new BufferedInputStream(new FileInputStream(
                    new File(path)));
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(in, null, options);
            in.close();
            int i = 0;
            while (true) {
                if ((options.outWidth >> i <= maxWidth)
                        && (options.outHeight >> i <= maxHeight)) {
                    in = new BufferedInputStream(
                            new FileInputStream(new File(path)));
                    options.inSampleSize = (int) Math.pow(2.0D, i);
                    options.inJustDecodeBounds = false;
                    bitmap = BitmapFactory.decodeStream(in, null, options);
                    break;
                }
                i += 1;
            }
        } catch (Exception e) {
            return null;
        }
        return bitmap;
    }

    public static Size getImageSize(String imagePath) throws IOException {
        FileInputStream fis;
        fis = new FileInputStream(imagePath);
        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inJustDecodeBounds = true;

        BitmapFactory.decodeFile(imagePath, opt);
        Bitmap bitmap = BitmapFactory.decodeStream(fis,null, opt);

        fis.close();
        return new Size(bitmap.getWidth(), bitmap.getHeight());
    }
}
