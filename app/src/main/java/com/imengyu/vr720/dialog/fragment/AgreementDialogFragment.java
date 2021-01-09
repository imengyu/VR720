package com.imengyu.vr720.dialog.fragment;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.imengyu.vr720.R;
import com.imengyu.vr720.config.Constants;
import com.imengyu.vr720.utils.AlertDialogTool;
import com.imengyu.vr720.utils.text.CustomUrlSpan;
import com.imengyu.vr720.utils.PixelTool;

public class AgreementDialogFragment extends DialogFragment {

    public AgreementDialogFragment() {
        super();
        setStyle(DialogFragment.STYLE_NORMAL, R.style.CustomDialog);
    }

    public interface OnAgreementCloseListener {
        void onAgreementClose(boolean allowed);
    }

    private OnAgreementCloseListener onAgreementCloseListener = null;

    public void setOnAgreementCloseListener(OnAgreementCloseListener onAgreementCloseListener) {
        this.onAgreementCloseListener = onAgreementCloseListener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.dialog_argeement, container, false);
        context = requireContext();

        scroll_main = v.findViewById(R.id.scroll_main);
        Button btn_ok = v.findViewById(R.id.btn_ok);
        Button btn_close = v.findViewById(R.id.btn_close);
        Button btn_cancel = v.findViewById(R.id.btn_cancel);
        TextView text_base = v.findViewById(R.id.text_base);
        TextView text_notice = v.findViewById(R.id.text_notice);
        TextView text_title = v.findViewById(R.id.text_title);
        TextView text_agreement_urls = v.findViewById(R.id.text_agreement_urls);
        CheckBox check_agreement = v.findViewById(R.id.check_agreement);

        //构建协议可点字符串
        SpannableStringBuilder urlSpannableStringBuilder = new SpannableStringBuilder();
        if(onAgreementCloseListener!=null)
            urlSpannableStringBuilder.append(getString(R.string.text_i_agree_agreement));
        urlSpannableStringBuilder.append(getString(R.string.text_agreement),
                new CustomUrlSpan(context, Constants.ARGEEMENT_URL), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        urlSpannableStringBuilder.append(getString(R.string.text_and));
        urlSpannableStringBuilder.append(getString(R.string.text_privacy_policy),
                new CustomUrlSpan(context, Constants.PRIVACY_POLICY_URL), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);

        //事件
        if(onAgreementCloseListener!=null) {
            btn_ok.setEnabled(false);
            btn_ok.setOnClickListener(view -> { dismiss(); onAgreementCloseListener.onAgreementClose(true); });
            btn_cancel.setOnClickListener(view -> { dismiss(); onAgreementCloseListener.onAgreementClose(false); });
            text_base.setText(R.string.text_agreement_base_welcome);
            check_agreement.setOnCheckedChangeListener((buttonView, isChecked) -> {
                btn_ok.setEnabled(isChecked);
            });
            text_agreement_urls.setMovementMethod(LinkMovementMethod.getInstance());
            text_agreement_urls.setText(urlSpannableStringBuilder);
        }
        else {
            btn_ok.setVisibility(View.GONE);
            btn_cancel.setVisibility(View.GONE);
            btn_close.setVisibility(View.VISIBLE);
            check_agreement.setVisibility(View.GONE);
            text_notice.setVisibility(View.GONE);
            text_agreement_urls.setVisibility(View.GONE);
            btn_close.setOnClickListener(view -> dismiss());
            urlSpannableStringBuilder.append('\n');
            urlSpannableStringBuilder.append(getString(R.string.text_agreement_base));
            text_base.setMovementMethod(LinkMovementMethod.getInstance());
            text_base.setText(urlSpannableStringBuilder);
            text_title.setText(R.string.settings_key_privacy_policy);
        }

        return v;
    }

    private Context context;
    private ScrollView scroll_main;

    @Override
    public void onResume() {
        Window window = requireDialog().getWindow();
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
        AlertDialogTool.bottomDialogSizeAutoSet(requireDialog(),
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            ViewGroup.LayoutParams layoutParams = scroll_main.getLayoutParams();
            layoutParams.height = PixelTool.dp2px(context, 200);
            scroll_main.setLayoutParams(layoutParams);
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            ViewGroup.LayoutParams layoutParams = scroll_main.getLayoutParams();
            layoutParams.height = PixelTool.dp2px(context, 400);
            scroll_main.setLayoutParams(layoutParams);
        }
        super.onConfigurationChanged(newConfig);
    }
}
