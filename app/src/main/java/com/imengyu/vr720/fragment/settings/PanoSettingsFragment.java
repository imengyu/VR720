package com.imengyu.vr720.fragment.settings;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import com.imengyu.vr720.R;

public class PanoSettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.pano_preferences, rootKey);

        SwitchPreferenceCompat enable_custom_fps = findPreference("enable_custom_fps");
        enable_low_fps = findPreference("enable_low_fps");
        custom_fps_value = findPreference("custom_fps_value");

        SharedPreferences sharedPreferences = getPreferenceScreen().getSharedPreferences();

        assert enable_custom_fps != null;
        assert enable_low_fps != null;
        assert custom_fps_value != null;

        enable_custom_fps.setOnPreferenceChangeListener((preference, object) -> {
            updateCustomFpsItemVisible((boolean)object);
            return true;
        });
        custom_fps_value.setValue(String.valueOf(sharedPreferences.getInt("custom_fps_value", 0)));
        custom_fps_value.setOnPreferenceChangeListener((preference, object) -> {
            updateCustomFpsItemValue((String)object);
            sharedPreferences.edit().putInt("custom_fps_value", Integer.parseInt((String)object)).apply();
            return true;
        });

        updateCustomFpsItemValue(custom_fps_value.getValue());
        updateCustomFpsItemVisible(enable_custom_fps.isChecked());

    }

    private ListPreference custom_fps_value;
    private SwitchPreferenceCompat enable_low_fps;

    private void updateCustomFpsItemValue(String value) {
        int index = custom_fps_value.findIndexOfValue(value);
        CharSequence[] entries = custom_fps_value.getEntries();
        if(index >= 0 && index < entries.length)
            custom_fps_value.setSummary(entries[index]);
        else
            custom_fps_value.setSummary("");
    }
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
