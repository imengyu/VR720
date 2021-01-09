package com.imengyu.vr720.fragment.settings;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import com.imengyu.vr720.R;
import com.imengyu.vr720.dialog.CommonDialog;

public class PanoSettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.pano_preferences, rootKey);

        SwitchPreferenceCompat enable_custom_fps = findPreference("enable_custom_fps");
        SwitchPreferenceCompat enable_full_chunks = findPreference("enable_full_chunks");
        enable_low_fps = findPreference("enable_low_fps");
        custom_fps_value = findPreference("custom_fps_value");

        SharedPreferences sharedPreferences = getPreferenceScreen().getSharedPreferences();

        assert enable_full_chunks != null;
        assert enable_custom_fps != null;
        assert enable_low_fps != null;
        assert custom_fps_value != null;

        enable_full_chunks.setOnPreferenceChangeListener((preference, object) -> {
            if((boolean)object) {
                new CommonDialog(requireActivity())
                        .setTitle(R.string.text_tip)
                        .setMessage(R.string.text_open_full_chunks_warn)
                        .show();
            }
            return true;
        });
        enable_custom_fps.setOnPreferenceChangeListener((preference, object) -> {
            updateCustomFpsItemVisible((boolean)object);
            return true;
        });
        custom_fps_value.setValue(String.valueOf(sharedPreferences.getInt("custom_fps_value", 0)));
        custom_fps_value.setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance());
        custom_fps_value.setOnPreferenceChangeListener((preference, object) -> {
            sharedPreferences.edit().putInt("custom_fps_value", Integer.parseInt((String)object)).apply();
            return true;
        });
        updateCustomFpsItemVisible(enable_custom_fps.isChecked());

    }

    private ListPreference custom_fps_value;
    private SwitchPreferenceCompat enable_low_fps;

    private void updateCustomFpsItemVisible(boolean enable_custom_fps) {
        if(enable_custom_fps) {
            enable_low_fps.setVisible(false);
            custom_fps_value.setVisible(true);
        } else {
            enable_low_fps.setVisible(true);
            custom_fps_value.setVisible(false);
        }
    }
}
