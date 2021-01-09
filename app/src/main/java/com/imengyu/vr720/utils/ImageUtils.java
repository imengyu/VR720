package com.imengyu.vr720.utils;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Size;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ImageUtils {
    /**
     * 根据指定的图像路径和大小来获取缩略图
     *
     * @param path      图像的路径
     * @param maxWidth  指定输出图像的宽度
     * @param maxHeight 指定输出图像的高度
     * @return 生成的缩略图
     */
    public static Bitmap revitionImageSize(String path, int maxWidth, int maxHeight) {
        Bitmap bitmap;
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
     * 加载图片为 Bitmap
     *
     * @param path 图像的路径
     * @return 返回Bitmap
     */
    public static Bitmap loadBitmap(String path) {
        Bitmap bitmap = null;
        try {
            BufferedInputStream in = new BufferedInputStream(new FileInputStream(
                    new File(path)));
            BitmapFactory.Options options = new BitmapFactory.Options();
            bitmap = BitmapFactory.decodeStream(in, null, options);
            in.close();
        } catch (Exception e) {
            return null;
        }
        return bitmap;
    }


    /**
     * 获取图像大小
     *
     * @param imagePath 图像路径
     * @return 图像大小
     * @throws IOException 抛出异常
     */
    public static Size getImageSize(String imagePath) throws IOException {
        FileInputStream fileInputStream = new FileInputStream(imagePath);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;

        BitmapFactory.decodeFile(imagePath, options);
        BitmapFactory.decodeStream(fileInputStream, null, options);

        fileInputStream.close();
        return new Size(options.outWidth, options.outHeight);
    }

    public static boolean checkSizeIsNormalPanorama(Size imageSize) {
        return checkSizeIs320Panorama(imageSize) || checkSizeIs720Panorama(imageSize);
    }

    public static boolean checkSizeIs720Panorama(Size imageSize) {
        int w = imageSize.getWidth(), h = imageSize.getHeight();
        return Math.abs(2.0 - (double) w / h) < 0.15;
    }

    public static boolean checkSizeIs320Panorama(Size imageSize) {
        int w = imageSize.getWidth(), h = imageSize.getHeight();
        return h > 0 && (double) w / h > 5;
    }

    public static class SaveImageResult {
        public boolean success;
        public String path;
        public String error;
    }

    /**
     * 按自动名称保存图像至存储中
     *
     * @param bitmap 图像
     * @return 返回是否成功
     */
    public static SaveImageResult saveImageToGalleryWithAutoName(Context context, Bitmap bitmap) {

        SimpleDateFormat simpleDate = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault());
        String fileName = String.format("%s.jpg", simpleDate.format(new Date()));
        return saveImageToGallery(context, bitmap, fileName);
    }

    /**
     * 保存图像至存储中
     *
     * @param context  上下文
     * @param bitmap   图像
     * @param fileName 文件名
     * @return 返回是否成功
     */
    public static SaveImageResult saveImageToGallery(Context context, Bitmap bitmap, String fileName) {

        SaveImageResult result = new SaveImageResult();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
            values.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg");
            values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/VR720");
            values.put(MediaStore.Images.Media.IS_PENDING, true);

            Uri uri = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (uri != null) {
                try {
                    OutputStream out = context.getContentResolver().openOutputStream(uri);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                    out.flush();
                    out.close();
                    values.put(MediaStore.Images.Media.IS_PENDING, false);
                    context.getContentResolver().update(uri, values, null, null);

                    result.success = true;
                } catch (IOException e) {
                    e.printStackTrace();
                    result.error = e.toString();
                    result.success = false;
                }
            } else {
                result.error = "uri is null";
            }
        } else {

            String filePath = StorageDirUtils.getFileStoragePath() + fileName;
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

        }

        return result;
    }


    public static SaveImageResult saveImageToLocalFolder(Context context, Bitmap bitmap, String fileName) {

        SaveImageResult result = new SaveImageResult();

        String filePath = StorageDirUtils.getFileStoragePath() + fileName;
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
