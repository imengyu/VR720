package com.imengyu.vr720.dialog.fragment;

import android.app.Dialog;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.imengyu.vr720.R;
import com.imengyu.vr720.dialog.CommonDialogInternal;
import com.imengyu.vr720.utils.AlertDialogTool;
import com.imengyu.vr720.utils.PixelTool;

public class CommonDialogFragment extends DialogFragment {

    public CommonDialogFragment() { setStyle(DialogFragment.STYLE_NORMAL, R.style.CustomDialog); }

    private Context context;
    private CommonDialogInternal commonDialogInternal;

    public CommonDialogInternal getCommonDialogInternal() { return commonDialogInternal; }
    public CommonDialogInternal createCommonDialogInternal(Context context) {
        commonDialogInternal = new CommonDialogInternal(context);
        return commonDialogInternal;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        context = requireContext();
        return commonDialogInternal;
    }

    @Override
    public void onResume() {

        commonDialogInternal.refreshViews();

        Window window = commonDialogInternal.getWindow();
        window.setWindowAnimations(R.style.DialogBottomPopup);

        WindowManager.LayoutParams params = window.getAttributes();
        params.width = WindowManager.LayoutParams.MATCH_PARENT;
        params.height = WindowManager.LayoutParams.MATCH_PARENT;
        window.setAttributes(params);

        onConfigurationChanged(getResources().getConfiguration());
        super.onResume();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        AlertDialogTool.bottomDialogSizeAutoSet(commonDialogInternal,
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {

            commonDialogInternal.layout_dialog.setPadding(commonDialogInternal.layout_dialog.getPaddingLeft(),
                    PixelTool.dp2px(context, 10),
                    commonDialogInternal.layout_dialog.getPaddingRight(),
                    PixelTool.dp2px(context, 10));

        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {

            commonDialogInternal.layout_dialog.setPadding(commonDialogInternal.layout_dialog.getPaddingLeft(),
                    PixelTool.dp2px(context, 35),
                    commonDialogInternal.layout_dialog.getPaddingRight(),
                    PixelTool.dp2px(context, 30));

        }

        super.onConfigurationChanged(newConfig);
    }
}
