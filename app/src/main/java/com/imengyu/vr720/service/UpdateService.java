package com.imengyu.vr720.service;

import com.alibaba.fastjson.JSONObject;
import com.imengyu.vr720.BuildConfig;
import com.imengyu.vr720.config.Constants;
import com.imengyu.vr720.utils.Base64Utils;
import com.imengyu.vr720.utils.HttpUtils;

import java.util.Locale;

public class UpdateService {

    private boolean checkingUpdate = false;

    public interface OnCheckUpdateCallback {
        void onCheckUpdateSuccess(boolean hasUpdate, String newVer, int verCode, String newText, String md5, String downUrl);
        void onCheckUpdateFailed(String err);
    }

    public void checkUpdate(OnCheckUpdateCallback callback) {

        if(checkingUpdate)
            return;

        checkingUpdate = true;

        new Thread(() -> {
            boolean success = false;
            String err = null;
            boolean hasUpdate = false;
            String newText = null;
            String newVer = null;
            int newVerCode = 0;
            String url = null;
            String md5 = null;

            try {
                Thread.sleep(600);
                JSONObject object = HttpUtils.httpGetJson(
                        String.format(Locale.getDefault(),
                                Constants.CHECK_UPDATE_URL, BuildConfig.APPLICATION_ID, BuildConfig.VERSION_CODE));
                if(object != null) {
                    if (object.getInteger("code") == 200) {
                        if (object.getBoolean("update")){
                            hasUpdate = true;
                            newVer = object.getString("ver");
                            newText = Base64Utils.decode(object.getString("text"));
                            newVerCode = object.getInteger("vercode");
                            url = object.getString("url");
                            md5 = object.getString("md5");
                        }
                        success = true;
                    } else err = object.getString("message");
                } else err = "未知错误";
            } catch (Exception e) {
                e.printStackTrace();
                err = e.getMessage();
            }

            if(success) {
                if(callback != null)
                    callback.onCheckUpdateSuccess(hasUpdate, newVer, newVerCode, newText, md5, url);
            }
            else {
                if(callback != null)
                    callback.onCheckUpdateFailed(err);
            }

            checkingUpdate = false;
        }).start();
    }
}
