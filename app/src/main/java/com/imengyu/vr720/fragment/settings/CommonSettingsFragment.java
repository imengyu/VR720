package com.imengyu.vr720.fragment.settings;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import com.hjq.toast.ToastUtils;
import com.imengyu.vr720.BuildConfig;
import com.imengyu.vr720.R;
import com.imengyu.vr720.SettingsActivity;
import com.imengyu.vr720.VR720Application;
import com.imengyu.vr720.dialog.AppDialogs;
import com.imengyu.vr720.dialog.CommonDialog;
import com.imengyu.vr720.dialog.LoadingDialog;
import com.imengyu.vr720.service.CacheServices;
import com.imengyu.vr720.utils.AppUtils;

public class CommonSettingsFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.common_preferences, rootKey);

        Preference app_go_app_store_check_update = findPreference("app_go_app_store_check_update");
        Preference app_check_update = findPreference("app_check_update");
        Preference settings_key_clear_cache = findPreference("settings_key_clear_cache");
        native_log_level = findPreference("native_log_level");

        SettingsActivity activity = (SettingsActivity)getActivity();

        SharedPreferences sharedPreferences = getPreferenceScreen().getSharedPreferences();

        assert activity != null;
        assert native_log_level != null;
        assert app_check_update != null;
        assert app_go_app_store_check_update != null;
        assert settings_key_clear_cache != null;

        app_check_update.setOnPreferenceClickListener(preference -> {
            Log.i("Settings", "Do update");
            return true;
        });
        app_check_update.setSummary(String.format("%s%s", getString(R.string.text_soft_version), getString(R.string.version_name)));
        app_go_app_store_check_update.setOnPreferenceClickListener(preference -> {
            AppUtils.goToAppStore(activity, BuildConfig.APPLICATION_ID);
            return true;
        });

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

        native_log_level.setValue(String.valueOf(sharedPreferences.getInt("native_log_level", 0)));
        native_log_level.setOnPreferenceChangeListener((preference, object) -> {
            updateNativeLogLevelItemValue((String)object);
            sharedPreferences.edit().putInt("native_log_level", Integer.parseInt((String)object)).apply();
            return true;
        });

        updateNativeLogLevelItemValue(native_log_level.getValue());
    }

    private ListPreference native_log_level;

    private void updateNativeLogLevelItemValue(String value) {
        int index = native_log_level.findIndexOfValue(value);
        CharSequence[] entries = native_log_level.getEntries();
        if(index >= 0 && index < entries.length)
            native_log_level.setSummary(entries[index]);
        else
            native_log_level.setSummary("");
    }
}
