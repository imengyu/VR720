package com.imengyu.vr720.fragment;

import android.os.Bundle;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.imengyu.vr720.R;
import com.imengyu.vr720.model.TitleSelectionChangedCallback;

public class GalleryFragment extends Fragment implements IMainFragment {

    public static GalleryFragment newInstance() {
        return new GalleryFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.layout_gallery, null);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {




    }

    @Override
    public void setTitleSelectionChangedCallback(TitleSelectionChangedCallback callback) {

    }
    @Override
    public void setTitleSelectionCheckAllSwitch() {

    }
    @Override
    public void setTitleSelectionQuit() {

    }
    @Override
    public boolean onBackPressed() {
        return false;
    }

    @Override
    public void showMore() {

    }

    @Override
    public void handleMessage(Message msg) {

    }

}