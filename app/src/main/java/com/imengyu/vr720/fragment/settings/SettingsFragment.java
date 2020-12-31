package com.imengyu.vr720.fragment.settings;

import android.os.Bundle;
import android.util.Log;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.hjq.toast.ToastUtils;
import com.imengyu.vr720.R;
import com.imengyu.vr720.SettingsActivity;
import com.imengyu.vr720.VR720Application;
import com.imengyu.vr720.dialog.AppDialogs;
import com.imengyu.vr720.dialog.CommonDialog;
import com.imengyu.vr720.dialog.LoadingDialog;
import com.imengyu.vr720.service.CacheServices;

public class SettingsFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey);

        Preference app_privacy_policy = findPreference("app_privacy_policy");
        Preference app_about = findPreference("app_about");
        Preference settings_key_go_panorama = findPreference("settings_key_go_panorama");
        Preference settings_key_go_common = findPreference("settings_key_go_common");

        SettingsActivity activity = (SettingsActivity)getActivity();

        assert activity != null;
        assert app_privacy_policy != null;
        assert app_about != null;
        assert settings_key_go_panorama != null;
        assert settings_key_go_common != null;

        app_privacy_policy.setOnPreferenceClickListener(preference -> {
            AppDialogs.showPrivacyPolicyAndAgreement(this.getActivity(), null);
            return true;
        });
        app_about.setOnPreferenceClickListener(preference -> {
            AppDialogs.showAbout(activity);
            return true;
        });

        settings_key_go_panorama.setOnPreferenceClickListener((preference) -> {
            activity.switchFragment(activity.getPanoSettingsFragment());
            return true;
        });
        settings_key_go_common.setOnPreferenceClickListener((preference) -> {
            activity.switchFragment(activity.getCommonSettingsFragment());
            return true;
        });
    }
}
