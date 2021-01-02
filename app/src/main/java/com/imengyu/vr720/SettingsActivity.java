package com.imengyu.vr720;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.imengyu.vr720.fragment.settings.CommonSettingsFragment;
import com.imengyu.vr720.fragment.settings.PanoSettingsFragment;
import com.imengyu.vr720.fragment.settings.SettingsFragment;
import com.imengyu.vr720.fragment.settings.VideoSettingsFragment;
import com.imengyu.vr720.utils.StatusBarUtils;
import com.imengyu.vr720.widget.MyTitleBar;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        SettingsFragment settingsFragment = new SettingsFragment();
        panoSettingsFragment = new PanoSettingsFragment();
        commonSettingsFragment = new CommonSettingsFragment();
        videoSettingsFragment = new VideoSettingsFragment();
        switchFragment(settingsFragment);

        MyTitleBar titleBar = findViewById(R.id.titlebar);
        titleBar.setLeftIconOnClickListener(v -> onBackPressed());

        StatusBarUtils.setLightMode(this);
    }

    @Override
    public void onBackPressed() {
        if(getSupportFragmentManager().getBackStackEntryCount() <= 1)
            finish();
        else
            getSupportFragmentManager().popBackStack();
    }

    private VideoSettingsFragment videoSettingsFragment = null;
    private CommonSettingsFragment commonSettingsFragment = null;
    private PanoSettingsFragment panoSettingsFragment = null;

    public CommonSettingsFragment getCommonSettingsFragment() {
        return commonSettingsFragment;
    }
    public PanoSettingsFragment getPanoSettingsFragment() {
        return panoSettingsFragment;
    }
    public VideoSettingsFragment getVideoSettingsFragment() {
        return videoSettingsFragment;
    }

    public void switchFragment(Fragment targetFragment) {
        FragmentTransaction transaction = getSupportFragmentManager()
                .beginTransaction();
        transaction
                .replace(R.id.settings, targetFragment)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .addToBackStack(null)
                .commit();
    }

}