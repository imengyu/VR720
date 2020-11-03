package com.dreamfish.com.vr720.utils;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.widget.Toast;
import androidx.core.content.FileProvider;

import com.dreamfish.com.vr720.BuildConfig;
import com.dreamfish.com.vr720.R;

import java.io.File;

public class FileUtils {

    public static String getFileName(String pathandname) {
        int start = pathandname.lastIndexOf("/");
        int end = pathandname.lastIndexOf(".");
        if (start != -1 && end != -1) {
            return pathandname.substring(start + 1, end);
        } else {
            return null;
        }
    }

    public static boolean deleteFile(String path) {
        File file = new File(path);
        if (file.exists()) {
            return file.delete();
        }
        return false;
    }

    public static void openFile(Context context, String file) {
        try {
            Intent intent = new Intent();
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setAction(Intent.ACTION_VIEW);
            //解决 Android N 7.0 上 报错：android.os.FileUriExposedException
            //判断是否是AndroidN以及更高的版本
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
}
