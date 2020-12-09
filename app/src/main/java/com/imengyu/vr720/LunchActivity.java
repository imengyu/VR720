package com.imengyu.vr720;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.imengyu.vr720.dialog.CommonDialog;
import com.imengyu.vr720.dialog.CommonDialogs;
import com.imengyu.vr720.utils.StorageDirUtils;

public class LunchActivity extends AppCompatActivity {

    private static final int REQUEST_CODE = 179;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState);
        setContentView(R.layout.activity_lunch);
        new Thread(() -> {
            try {
                Thread.sleep(600);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //耗时任务，比如加载网络数据
            StorageDirUtils.testAndCreateStorageDirs();
            //转回UI线程
            runOnUiThread(() -> {
                //检查是否同意许可以及请求权限
                testAgreementAllowed((b) -> {
                    if(checkPermission())
                        runMainActivity();
                });
            });
        }).start();
    }

    private interface TestAgreementAllowedCallback {
        void testAgreementAllowedCallback(boolean b);
    }
    private void testAgreementAllowed(TestAgreementAllowedCallback callback) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if(!prefs.getBoolean("app_agreement_allowed", false)) {
            CommonDialogs.showPrivacyPolicyAndAgreement(this, (allowed) -> {
                if(allowed) {
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putBoolean("app_agreement_allowed", true);
                    editor.apply();
                    callback.testAgreementAllowedCallback(false);
                }else finish();
            });
        }else callback.testAgreementAllowedCallback(true);
    }


    //权限申请
    //=================

    private String[] permissions = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private boolean checkPermission(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int i = checkSelfPermission(permissions[0]);
            // 权限是否已经 授权 GRANTED---授权  DINIED---拒绝
            if (i != PackageManager.PERMISSION_GRANTED) {
                // 如果没有授予该权限，就去提示用户请求
                showDialogTipUserRequestPermission();
                return false;
            }
        }
        return true;
    }

    // 提示用户该请求权限的弹出框
    private void showDialogTipUserRequestPermission() {
        new CommonDialog(this)
                .setTitle(getString(R.string.text_no_storage_permission))
                .setMessage(getString(R.string.text_storage_permission_useage))
                .setPositive(getString(R.string.action_open_now))
                .setNegative(getString(R.string.action_cancel))
                .setOnClickBottomListener(new CommonDialog.OnClickBottomListener() {
                    @Override
                    public void onPositiveClick(CommonDialog dialog) {
                        startRequestPermission();
                        dialog.dismiss();
                    }
                    @Override
                    public void onNegativeClick(CommonDialog dialog) {
                        finish();
                        dialog.dismiss();
                    }
                })
                .setDialogCancelable(false)
                .show();
    }
    // 开始提交请求权限
    @TargetApi(Build.VERSION_CODES.M)
    private void startRequestPermission() {
        requestPermissions(this.permissions, REQUEST_CODE);
    }
    // 提示用户去应用设置界面手动开启权限
    private void showDialogTipUserGoToAppSettting() {

        // 跳转到应用设置界面
        new CommonDialog(this)
                .setDialogCancelable(false)
                .setTitle(getString(R.string.text_no_storage_permission))
                .setMessage(getString(R.string.text_storage_permission_open_intro))
                .setPositive(getString(R.string.action_open_now))
                .setNegative(getString(R.string.action_cancel))
                .setOnClickBottomListener(new CommonDialog.OnClickBottomListener() {
                    @Override
                    public void onPositiveClick(CommonDialog dialog) {
                        // 跳转到应用设置界面
                        goToAppSetting();
                        dialog.dismiss();
                    }
                    @Override
                    public void onNegativeClick(CommonDialog dialog) {
                        finish();
                        dialog.dismiss();
                    }
                })
                .show();
    }
    // 跳转到当前应用的设置界面
    private void goToAppSetting() {
        Intent intent = new Intent();

        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);

        startActivityForResult(intent, 123);
    }
    // 跳转到主界面
    private void runMainActivity() {
        Intent intent = new Intent(LunchActivity.this, MainActivity.class);
        startActivity(intent);
        LunchActivity.this.finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    // 判断用户是否 点击了不再提醒。(检测该权限是否还可以申请)
                    boolean b = shouldShowRequestPermissionRationale(permissions[0]);
                    if (!b) {
                        // 用户还是想用我的 APP 的
                        // 提示用户去应用设置界面手动开启权限
                        showDialogTipUserGoToAppSettting();
                    } else
                        finish();
                }else runMainActivity();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

}
