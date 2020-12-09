package com.imengyu.vr720.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.Size;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

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

    /**
     * 获取图像大小
     * @param imagePath 图像路径
     * @return 图像大小
     * @throws IOException 抛出异常
     */
    public static Size getImageSize(String imagePath) throws IOException {
        FileInputStream fis;
        fis = new FileInputStream(imagePath);
        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inJustDecodeBounds = true;

        BitmapFactory.decodeFile(imagePath, opt);
        Bitmap bitmap = BitmapFactory.decodeStream(fis,null, opt);

        fis.close();
        if(bitmap == null)
           throw new IOException("打开图像失败");

        return new Size(bitmap.getWidth(), bitmap.getHeight());
    }

    public static class SaveImageResult {
        public boolean success;
        public String path;
        public String error;
    }

    /**
     * 按自动名称保存图像至存储中
     * @param bitmap 图像
     * @return 返回是否成功
     */
    public static SaveImageResult saveImageToStorageWithAutoName(Bitmap bitmap) {
        SimpleDateFormat simpleDate = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault());
        String fileName = String.format("%s.jpg", simpleDate.format(new Date()));
        String filePath = StorageDirUtils.STORAGE_PATH + "/VR720/ScreenShots/ScreenShot" + fileName;

        SaveImageResult result = new SaveImageResult();
        result.path = filePath;
        try {
            File file = new File(filePath);
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();
            result.success = true;
        } catch (Exception e) {
            e.printStackTrace();
            result.error = e.toString();
            result.success = false;
        }
        return result;
    }
}
