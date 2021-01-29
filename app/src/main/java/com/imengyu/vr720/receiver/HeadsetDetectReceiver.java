package com.imengyu.vr720.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.imengyu.vr720.activity.PanoActivity;

public class HeadsetDetectReceiver extends BroadcastReceiver {

    public HeadsetDetectReceiver(PanoActivity panoActivity) {
        this.panoActivity = panoActivity;
    }

    private final PanoActivity panoActivity;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (Intent.ACTION_HEADSET_PLUG.equals(action)) {
            if (intent.hasExtra("state")) {
                int state = intent.getIntExtra("state", 0);
                panoActivity.onHeadsetStateChanged (state == 1);
            }
        }
    }
}
