package com.imengyu.vr720.fragment;

import android.content.Intent;
import android.os.Message;

import com.imengyu.vr720.model.TitleSelectionChangedCallback;

public interface IMainFragment {

    void onActivityResult(int requestCode, int resultCode, Intent data);
    void setTitleSelectionChangedCallback(TitleSelectionChangedCallback callback);
    void setTitleSelectionCheckAllSwitch();
    void setTitleSelectionQuit();

    boolean onBackPressed();
    void showMore();
    void handleMessage(Message msg);
}
