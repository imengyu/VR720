package com.imengyu.vr720.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;

import androidx.core.content.FileProvider;

import com.imengyu.vr720.BuildConfig;
import com.imengyu.vr720.R;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

public class ShareUtils {

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
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
        } else {
            Uri uri = Uri.fromFile(new File(file));
            shareIntent.setDataAndType(uri, MapTable.getMIMEType(file));
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        }
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.setType("image/*");
        shareIntent = Intent.createChooser(shareIntent, context.getString(R.string.text_share_image_title));
        context.startActivity(shareIntent);
    }


    /**
     * Share the given files as stream with given mime-type
     *
     * @param files    The files to share
     */
    public static void shareStreamMultiple(Context context, final Collection<File> files) {
        ArrayList<Uri> rawUriItems = new ArrayList<>();
        for (File file : files) {
            Uri uri = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".fileProvider", file);
            rawUriItems.add(uri);
        }

        Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        intent.putExtra(Intent.EXTRA_TEXT, "VR720 Share files");
        intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, rawUriItems);
        intent.setType("image/*");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setAction(Intent.ACTION_SEND_MULTIPLE);
        intent = Intent.createChooser(intent, String.format(context.getString(R.string.text_share_files_title), files.size()));
        context.startActivity(intent);
    }
}
