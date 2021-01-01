package com.imengyu.vr720.utils;

import android.content.ActivityNotFoundException;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;
import androidx.core.content.FileProvider;

import com.imengyu.vr720.BuildConfig;
import com.imengyu.vr720.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
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
                || ext.equalsIgnoreCase("png")
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

    /**
     * 写入txt文件
     * @param path 文件路径
     * @param content 内容
     * @return 返回是否成功
     */
    public static boolean writeToTextFile(String path, String content) {
        File file = new File(path);
        try {
            if(!file.exists()) {
                if(file.createNewFile()) Log.i(TAG, "Create file " + path);
                else Log.e(TAG, "Create file " + path + " failed!");
            }

            FileWriter fileWritter = new FileWriter(file, false);
            fileWritter.write(content);
            fileWritter.close();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Write file " + path + " failed : " + e.toString());
            return false;
        }
        return true;
    }

    /**
     * 读取txt文件
     * @param path 文件路径
     * @return 返回是否成功
     */
    public static String readToTextFile(String path) {
        File file = new File(path);
        if(file.isFile() && file.exists()){
            try {
                FileInputStream fileInputStream = new FileInputStream(file);
                InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

                StringBuilder sb = new StringBuilder();
                String text;
                while((text = bufferedReader.readLine()) != null){
                    sb.append(text);
                }
                return sb.toString();
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "Read file " + path + " failed : " + e.toString());
            }
        }
        return null;
    }

    /**
     * 获取Content文件真实路径
     * @param context 上下文
     * @param uri URI
     * @return 返回Content
     */
    public static String getContentFilePath(Context context, Uri uri) {
        String chooseFilePath;
        if ("file".equalsIgnoreCase(uri.getScheme())) {//使用第三方应用打开
            chooseFilePath = uri.getPath();
            Toast.makeText(context, chooseFilePath, Toast.LENGTH_SHORT).show();
            return chooseFilePath;
        }
        //4.4以后
        chooseFilePath = getPath(context, uri);
        return chooseFilePath;
    }

    private static String getPath(final Context context, final Uri uri) {

        // DocumentProvider
        if (DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];

                }
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {
                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.parseLong(id));
                return getDataColumn(context, contentUri, null, null);

            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;

                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{split[1]};

                return getDataColumn(context, contentUri, selection, selectionArgs);

            }

        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);

        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            uri.getPath();
        }
        return null;
    }

    private static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {column};
        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    private static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    private static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    private static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }
}
