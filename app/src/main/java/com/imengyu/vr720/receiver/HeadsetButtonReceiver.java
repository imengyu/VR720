package com.imengyu.vr720.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;

import com.imengyu.vr720.activity.PanoActivity;

public class HeadsetButtonReceiver  extends BroadcastReceiver {

    public HeadsetButtonReceiver(PanoActivity panoActivity) {
        this.panoActivity = panoActivity;
    }

    private final PanoActivity panoActivity;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (Intent.ACTION_MEDIA_BUTTON.equals(action)) {
            KeyEvent keyEvent = (KeyEvent) intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            if(KeyEvent.KEYCODE_MEDIA_PLAY == keyEvent.getKeyCode())
                panoActivity.onHeadsetPlayButton();
            else if(KeyEvent.KEYCODE_MEDIA_NEXT == keyEvent.getKeyCode())
                panoActivity.onHeadsetNextButton();
            else if(KeyEvent.KEYCODE_MEDIA_PREVIOUS == keyEvent.getKeyCode())
                panoActivity.onHeadsetPrevButton();
        }
    }
}