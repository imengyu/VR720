package com.imengyu.vr720.utils;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;
import androidx.core.content.FileProvider;

import com.imengyu.vr720.BuildConfig;
import com.imengyu.vr720.R;

import java.io.File;
import java.text.DecimalFormat;

/**
 * 文件工具类
 */
public class FileUtils {

    private static final String TAG = "FileUtils";

    /**
     * 从文件路径获取文件名（不包括扩展名）
     * @param path 文件路径
     * @return 文件名（不包括扩展名）
     */
    public static String getFileName(String path) {
        int start = path.lastIndexOf("/");
        int end = path.lastIndexOf(".");
        if (start != -1 && end != -1) {
            return path.substring(start + 1, end);
        } else {
            return path;
        }
    }

    /**
     * 从文件路径获取文件名（包括扩展名）
     * @param path 文件路径
     * @return 文件名（包括扩展名）
     */
    public static String getFileNameWithExt(String path) {
        int start = path.lastIndexOf("/");
        if (start != -1)
            return path.substring(start + 1);
        else
            return path;
    }

    /**
     * 删除文件
     * @param path 文件路径
     * @return 返回是否成功
     */
    public static boolean deleteFile(String path) {
        File file = new File(path);
        if (file.exists()) {
            try {
                return file.delete();
            }catch (Exception e) {
                Log.e(TAG, String.format("Delete file %s failed : %s", path, e.toString()));
            }
        }
        return false;
    }

    /**
     * 获取文件夹大小
     * @param fileDir 文件夹路径
     * @return 返回文件夹大小（包括子文件夹）字节
     */
    public static long getDirSize(File fileDir){
        File[] files = fileDir.listFiles();
        long fileSize = 0;
        if(files == null) return fileSize;
        for (File file : files) {
            if (file.isFile())
                fileSize += file.length();
            else
                fileSize += getDirSize(file);
        }
        return fileSize;
    }

    /**
     * 将文件大小转为可读的字符串
     * @param fileSize 文件大小（字节）
     * @return 可读的字符串
     */
    public static String getReadableFileSize(long fileSize){
        double fileSizeDouble;
        DecimalFormat decimalFormat = new DecimalFormat("0.00");
        String sizeStr;
        if(fileSize >= 1073741824){
            fileSizeDouble = Math.round(fileSize / 1073741824.0 * 100)/100.0;
            sizeStr = decimalFormat.format(fileSizeDouble) + "GB";
        }else if(fileSize >= 1048576) {
            fileSizeDouble = Math.round(fileSize / 1048576.0 * 100) / 100.0;
            sizeStr = decimalFormat.format(fileSizeDouble) + "MB";
        }else{
            fileSizeDouble = Math.round(fileSize / 1024.0 * 100) / 100.0;
            sizeStr = decimalFormat.format(fileSizeDouble) + "KB";
        }
        return sizeStr;
    }

    /**
     * 使用系统选择文件打开方式
     * @param context 上下文
     * @param file 文件路径
     */
    public static void openFileWithApp(Context context, String file) {
        try {
            Intent intent = new Intent();
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setAction(Intent.ACTION_VIEW);
            //解决 Android N 7.0 上 使用 fileProvider 共享文件
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                Uri contentUri = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".fileProvider", new File(file));
                intent.setDataAndType(contentUri, MapTable.getMIMEType(file));
            } else {
                intent.setDataAndType(Uri.fromFile(new File(file)), MapTable.getMIMEType(file));
            }
            context.startActivity(Intent.createChooser(intent, context.getString(R.string.text_choose_program_to_open)));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, context.getString(R.string.text_image_cannot_open), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 分享文件
     * @param context 上下文
     * @param file 文件路径
     */
    public static void shareFile(Context context, String file) {

        Intent shareIntent = new Intent();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            shareIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Uri contentUri = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".fileProvider", new File(file));
            shareIntent.setDataAndType(contentUri, MapTable.getMIMEType(file));
        } else {
            shareIntent.setDataAndType(Uri.fromFile(new File(file)), MapTable.getMIMEType(file));
        }
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.setType("image/*");
        shareIntent = Intent.createChooser(shareIntent, context.getString(R.string.text_share_image_title));
        context.startActivity(shareIntent);
    }

    private static final String[] videoExtensions = new String[] {
            "wmv","rm","rmvb","mpg","mpeg","mpe","3gp","mov","mp4","m4v","avi","mkv","flv","vob"
    };

    /**
     * 获取文件路径的扩展名是不是支持的图像
     * @param path 文件路径
     * @return 返回是不是支持
     */
    public static boolean getFileIsImage(String path) {
        String ext = path.substring(path.lastIndexOf('.') + 1);
        return ext.equalsIgnoreCase("jpg")
                || ext.equalsIgnoreCase("jpeg")
                || ext.equalsIgnoreCase("jfif")
                || ext.equalsIgnoreCase("pmg")
                || ext.equalsIgnoreCase("bmp");
    }

    /**
     * 获取文件路径的扩展名是不是支持的视频
     * @param path 文件路径
     * @return 返回是不是支持
     */
    public static boolean getFileIsVideo(String path) {
        String ext = path.substring(path.lastIndexOf('.') + 1);
        for(String extVideo : videoExtensions)
            if(ext.equalsIgnoreCase(extVideo))
                return true;
        return false;
    }
}
