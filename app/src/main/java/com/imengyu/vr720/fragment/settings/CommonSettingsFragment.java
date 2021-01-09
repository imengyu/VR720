package com.imengyu.vr720.fragment.settings;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.azhon.appupdate.manager.DownloadManager;
import com.hjq.toast.ToastUtils;
import com.imengyu.vr720.BuildConfig;
import com.imengyu.vr720.R;
import com.imengyu.vr720.VR720Application;
import com.imengyu.vr720.activity.SettingsActivity;
import com.imengyu.vr720.dialog.CommonDialog;
import com.imengyu.vr720.dialog.LoadingDialog;
import com.imengyu.vr720.service.CacheServices;
import com.imengyu.vr720.service.UpdateService;
import com.imengyu.vr720.utils.AppUtils;
import com.imengyu.vr720.utils.NetworkUtils;

public class CommonSettingsFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.common_preferences, rootKey);

        Preference app_go_app_store_check_update = findPreference("app_go_app_store_check_update");
        Preference app_check_update = findPreference("app_check_update");
        Preference settings_key_clear_cache = findPreference("settings_key_clear_cache");
        ListPreference native_log_level = findPreference("native_log_level");

        SettingsActivity activity = (SettingsActivity)getActivity();
        SharedPreferences sharedPreferences = getPreferenceScreen().getSharedPreferences();

        assert activity != null;
        assert native_log_level != null;
        assert app_check_update != null;
        assert app_go_app_store_check_update != null;
        assert settings_key_clear_cache != null;

        app_check_update.setOnPreferenceClickListener(preference -> {
            if(NetworkUtils.isNetworkConnected(activity)) {
                if(NetworkUtils.isNetworkWifi()) checkUpdate();
                else {
                    new CommonDialog(activity)
                            .setMessage(R.string.text_cell_network_warn)
                            .setNegative(R.string.action_yes)
                            .setPositive(R.string.action_no)
                            .setOnResult((button, dialog) -> {
                                if(button == CommonDialog.BUTTON_POSITIVE) {
                                    checkUpdate();
                                    return true;
                                } else return button == CommonDialog.BUTTON_NEGATIVE;
                            })
                            .show();
                }
            } else ToastUtils.show(getString(R.string.text_network_not_connect));
            return true;
        });
        app_check_update.setSummary(String.format("%s%s", getString(R.string.text_soft_version), BuildConfig.VERSION_NAME));
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
                    .setPositive(R.string.action_ok)
                    .setNegative(R.string.action_cancel)
                    .setOnResult((b, dialog) -> {
                        if(b == CommonDialog.BUTTON_POSITIVE) {
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
                            return true;
                        } else return b == CommonDialog.BUTTON_NEGATIVE;
                    })
                    .show();
            return true;
        });

        native_log_level.setValue(String.valueOf(sharedPreferences.getInt("native_log_level", 0)));
        native_log_level.setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance());
        native_log_level.setOnPreferenceChangeListener((preference, object) -> {
            sharedPreferences.edit().putInt("native_log_level", Integer.parseInt((String)object)).apply();
            return true;
        });
    }

    private void checkUpdate() {
        SettingsActivity activity = (SettingsActivity)requireActivity();
        UpdateService updateService = ((VR720Application)activity.getApplication()).getUpdateService();

        LoadingDialog loadingDialog = new LoadingDialog(activity);
        loadingDialog.show();

        updateService.checkUpdate(new UpdateService.OnCheckUpdateCallback() {
            @Override
            public void onCheckUpdateSuccess(boolean hasUpdate, String newVer, int newVerCode,
                                             String newText, String md5, String downUrl) {
                activity.runOnUiThread(() -> {
                    loadingDialog.dismiss();
                    if (hasUpdate) {
                        DownloadManager manager = DownloadManager.getInstance(activity);
                        manager.setApkName("vr720-update.apk")
                                .setApkUrl(downUrl)
                                .setApkVersionCode(newVerCode)
                                .setApkVersionName(newVer)
                                .setApkDescription(newText)
                                .setApkMD5(md5)
                                .setSmallIcon(R.mipmap.ic_launcher)
                                .download();
                    } else {
                        ToastUtils.show(getString(R.string.text_update_latest));
                    }
                });
            }
            @Override
            public void onCheckUpdateFailed(String err) {
                activity.runOnUiThread(() -> {
                    loadingDialog.dismiss();
                    new CommonDialog(activity)
                            .setTitle(R.string.text_update_failed)
                            .setMessage(err)
                            .setImageResource(R.drawable.ic_warning)
                            .show();
                });
            }
        });
    }
}
