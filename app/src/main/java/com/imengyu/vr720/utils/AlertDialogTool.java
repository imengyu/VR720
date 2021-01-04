package com.imengyu.vr720.utils;

import android.app.Dialog;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Point;
import android.util.Size;
import android.view.ViewGroup;

import com.imengyu.vr720.R;

/**
 * 弹出框工具类
 */
public class AlertDialogTool {

    public static void bottomDialogSizeAutoSet(Dialog dialog, int width, int height) {
        Context context = dialog.getContext();
        ViewGroup viewGroup = dialog.findViewById(R.id.layout_dialog);

        if (viewGroup != null) {
            ViewGroup.LayoutParams layoutParams = viewGroup.getLayoutParams();
            layoutParams.width = width;

            //设置对话框在宽屏模式下的最宽宽度
            if (width == ViewGroup.LayoutParams.MATCH_PARENT) {
                Point point = new Point();
                context.getDisplay().getRealSize(point);
                Size screenSize = new Size(point.x, point.y);

                Configuration mConfiguration = context.getResources().getConfiguration(); //获取设置的配置信息
                if (mConfiguration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    layoutParams.width = screenSize.getWidth() > PixelTool.dp2px(context, 500) ?
                            PixelTool.dp2px(context, 500) : width;
                } else if (mConfiguration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                    layoutParams.width = width;
                }
            }

            layoutParams.height = height;
            viewGroup.setLayoutParams(layoutParams);
        }
    }

}
