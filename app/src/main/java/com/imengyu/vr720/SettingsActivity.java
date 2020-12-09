package com.imengyu.vr720;

import android.app.Activity;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.imengyu.vr720.dialog.CommonDialogs;
import com.imengyu.vr720.utils.StatusBarUtils;
import com.imengyu.vr720.widget.MyTitleBar;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_settings);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, new SettingsFragment())
                .commit();

        MyTitleBar titleBar = findViewById(R.id.titlebar);
        titleBar.setLeftIconOnClickListener(v -> finish());

        StatusBarUtils.setLightMode(this);
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            Preference app_check_update = findPreference("app_check_update");
            Preference app_privacy_policy = findPreference("app_privacy_policy");
            Preference app_about = findPreference("app_about");

            Activity activity = getActivity();

            assert activity != null;
            assert app_privacy_policy != null;
            assert app_check_update != null;
            assert app_about != null;

            app_privacy_policy.setOnPreferenceClickListener(preference -> {
                CommonDialogs.showPrivacyPolicyAndAgreement(this.getActivity(), null);
                return true;
            });
            app_check_update.setOnPreferenceClickListener(preference -> {

                return true;
            });
            app_about.setOnPreferenceClickListener(preference -> {
                CommonDialogs.showAbout(activity);
                return true;
            });
            app_check_update.setSummary(String.format("%s%s",
                    getString(R.string.text_soft_version),
                    getString(R.string.version_name)));

        }
    }
}