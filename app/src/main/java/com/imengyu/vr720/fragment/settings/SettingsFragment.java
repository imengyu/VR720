package com.imengyu.vr720.fragment.settings;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.hjq.toast.ToastUtils;
import com.imengyu.vr720.BuildConfig;
import com.imengyu.vr720.ChooseLanguageActivity;
import com.imengyu.vr720.R;
import com.imengyu.vr720.SettingsActivity;
import com.imengyu.vr720.VR720Application;
import com.imengyu.vr720.config.Codes;
import com.imengyu.vr720.dialog.AppDialogs;
import com.imengyu.vr720.dialog.CommonDialog;
import com.imengyu.vr720.model.GalleryItem;
import com.imengyu.vr720.model.ImageItem;
import com.imengyu.vr720.service.ListDataService;
import com.imengyu.vr720.utils.FileUtils;
import com.alibaba.fastjson.JSONObject;
import com.imengyu.vr720.utils.StringUtils;
import java.util.Set;

public class SettingsFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey);

        Preference app_privacy_policy = findPreference("app_privacy_policy");
        Preference app_about = findPreference("app_about");
        Preference settings_key_go_panorama = findPreference("settings_key_go_panorama");
        Preference settings_key_go_common = findPreference("settings_key_go_common");
        Preference data_backup = findPreference("data_backup");
        Preference data_restore_backup = findPreference("data_restore_backup");
        Preference app_choose_language = findPreference("app_choose_language");

        SettingsActivity activity = (SettingsActivity)getActivity();

        assert activity != null;
        assert app_privacy_policy != null;
        assert app_about != null;
        assert settings_key_go_panorama != null;
        assert settings_key_go_common != null;
        assert data_backup != null;
        assert data_restore_backup != null;
        assert app_choose_language != null;

        app_privacy_policy.setOnPreferenceClickListener(preference -> {
            AppDialogs.showPrivacyPolicyAndAgreement(this.getActivity(), null);
            return true;
        });
        app_about.setOnPreferenceClickListener(preference -> {
            AppDialogs.showAbout(activity);
            return true;
        });
        app_choose_language.setOnPreferenceClickListener((preference) -> {
            startActivityForResult(new Intent(activity, ChooseLanguageActivity.class), Codes.REQUEST_CODE_CHOOSE_LANGUAGE);
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

        data_backup.setOnPreferenceClickListener((preference) -> {
            new CommonDialog(activity)
                    .setTitle(getString(R.string.settings_key_data_backup))
                    .setMessage(getString(R.string.text_we_will_backup))
                    .setOnClickBottomListener(new CommonDialog.OnClickBottomListener() {
                        @Override
                        public void onPositiveClick(CommonDialog dialog) {
                            dialog.dismiss();

                            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                            intent.setType("application/json");
                            intent.addCategory(Intent.CATEGORY_OPENABLE);
                            startActivityForResult(intent, Codes.REQUEST_CODE_CHOOSE_EXPORT_FILE);
                        }
                        @Override
                        public void onNegativeClick(CommonDialog dialog) { dialog.dismiss(); }
                    })
                    .show();
            return true;
        });
        data_restore_backup.setOnPreferenceClickListener((preference) -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("application/json");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
            startActivityForResult(intent, Codes.REQUEST_CODE_CHOOSE_IMPORT_FILE);
            return true;
        });
    }

    private void doExportSettings(String path) {

        Log.i("Settings", "doExportSettings: " + path);

        Activity activity = getActivity();
        VR720Application application = (VR720Application)activity.getApplication();

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("version", BuildConfig.VERSION_CODE);
        jsonObject.put("settings", PreferenceManager.getDefaultSharedPreferences(activity).getAll());
        jsonObject.put("imageList", application.getListDataService().getImageList());
        jsonObject.put("galleryList", application.getListDataService().getGalleryList());

        if(FileUtils.writeToTextFile(path, jsonObject.toJSONString()))
            ToastUtils.show(getString(R.string.text_success));
        else
            ToastUtils.show(getString(R.string.text_failed));
    }
    private void doImportSettings(String path) {
        Activity activity = getActivity();
        VR720Application application = (VR720Application)activity.getApplication();
        ListDataService listDataService = application.getListDataService();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity);

        new CommonDialog(getActivity())
                .setTitle(getString(R.string.settings_key_data_restore_backup))
                .setMessage(getString(R.string.text_we_will_restore))
                .setCheckText(getString(R.string.text_merge_import_data))
                .setOnClickBottomListener(new CommonDialog.OnClickBottomListener() {
                    @Override
                    public void onPositiveClick(CommonDialog dialog) {
                        dialog.dismiss();

                        String json = FileUtils.readToTextFile(path);
                        if(StringUtils.isNullOrEmpty(json))  {
                            ToastUtils.show(getString(R.string.text_failed));
                            return;
                        }

                        boolean merge = dialog.isCheckBoxChecked();
                        try {
                            JSONObject jsonObject = JSON.parseObject(json);
                            JSONObject settings = jsonObject.getJSONObject("settings");
                            JSONArray imageList = jsonObject.getJSONArray("imageList");
                            JSONArray galleryList = jsonObject.getJSONArray("galleryList");

                            //数据
                            listDataService.importImageItems(imageList.toJavaList(ImageItem.class), merge);
                            listDataService.importGalleryItem(galleryList.toJavaList(GalleryItem.class), merge);
                            listDataService.setDataDirty(true);

                            //设置
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.clear();

                            Set<String> keys = settings.keySet();
                            for (String key : keys) {
                                Object object = settings.get(key);
                                if (object instanceof Integer)
                                    editor.putInt(key, (Integer) object);
                                else if (object instanceof String)
                                    editor.putString(key, (String) object);
                                else if (object instanceof Boolean)
                                    editor.putBoolean(key, (Boolean) object);
                                else if (object instanceof Float)
                                    editor.putFloat(key, (Float) object);
                            }

                            editor.apply();

                            ToastUtils.show(getString(R.string.text_success));
                        } catch (Exception e) {
                            e.printStackTrace();
                            ToastUtils.show(getString(R.string.text_failed) + "\n" + e.toString());
                        }

                    }
                    @Override
                    public void onNegativeClick(CommonDialog dialog) { dialog.dismiss(); }
                })
                .show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(resultCode == Activity.RESULT_OK && data != null) {
            Uri uri = data.getData();
            if(requestCode == Codes.REQUEST_CODE_CHOOSE_EXPORT_FILE) {
                doExportSettings(FileUtils.getContentFilePath(this.getContext(), uri));
            } else if(requestCode == Codes.REQUEST_CODE_CHOOSE_IMPORT_FILE) {
                doImportSettings(FileUtils.getContentFilePath(this.getContext(), uri));
            } else if(requestCode == Codes.REQUEST_CODE_CHOOSE_LANGUAGE) {
                if(data.getBooleanExtra("needRestart", false)) {
                    Activity activity = getActivity();
                    activity.setResult(0, new Intent().putExtra("needRestart", true));
                    activity.finish();
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
