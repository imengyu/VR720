package com.imengyu.vr720;

import android.app.Activity;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.hjq.toast.ToastUtils;
import com.imengyu.vr720.dialog.AppDialogs;
import com.imengyu.vr720.dialog.CommonDialog;
import com.imengyu.vr720.dialog.LoadingDialog;
import com.imengyu.vr720.service.CacheServices;
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
            Preference settings_key_clear_cache = findPreference("settings_key_clear_cache");

            Activity activity = getActivity();

            assert activity != null;
            assert app_privacy_policy != null;
            assert app_check_update != null;
            assert app_about != null;

            app_privacy_policy.setOnPreferenceClickListener(preference -> {
                AppDialogs.showPrivacyPolicyAndAgreement(this.getActivity(), null);
                return true;
            });
            app_check_update.setOnPreferenceClickListener(preference -> {

                return true;
            });
            app_about.setOnPreferenceClickListener(preference -> {
                AppDialogs.showAbout(activity);
                return true;
            });
            app_check_update.setSummary(String.format("%s%s",
                    getString(R.string.text_soft_version),
                    getString(R.string.version_name)));

            CacheServices cacheServices = ((VR720Application)activity.getApplication()).getCacheServices();

            settings_key_clear_cache.setSummary(String.format(getString(R.string.text_now_cache_size), cacheServices.getCacheDirSize()));
            settings_key_clear_cache.setOnPreferenceClickListener(preference -> {
                new CommonDialog(activity)
                        .setTitle(getString(R.string.text_warning))
                        .setMessage(getString(R.string.text_sure_to_clear_the_cache))
                        .setOnClickBottomListener(new CommonDialog.OnClickBottomListener() {
                            @Override
                            public void onPositiveClick(CommonDialog dialog) {

                                LoadingDialog loadingDialog = new LoadingDialog(activity);
                                loadingDialog.show();

                                new Thread(() -> {
                                    cacheServices.clearCacheDir();

                                    try {
                                        Thread.sleep(300);
                                    } catch (InterruptedException e) { e.printStackTrace(); }

                                    activity.runOnUiThread(() -> {
                                        loadingDialog.dismiss();
                                        ToastUtils.show(getString(R.string.text_cache_clear_finish));
                                    });
                                }).start();

                                dialog.dismiss();
                            }
                            @Override
                            public void onNegativeClick(CommonDialog dialog) { dialog.dismiss(); }
                        })
                        .show();
                return true;
            });
        }
    }
}