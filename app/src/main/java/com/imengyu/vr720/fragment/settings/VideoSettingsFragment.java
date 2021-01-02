package com.imengyu.vr720.fragment.settings;

import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

import com.imengyu.vr720.R;

public class VideoSettingsFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.video_preferences, rootKey);


    }
}
