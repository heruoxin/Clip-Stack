package com.catchingnow.tinyclipboardmanager;

import android.content.BroadcastReceiver;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.List;

public class LaunchServiceAtStartup extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
//            SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(context);
//            boolean forceShowNotification = preference.getBoolean(ActivitySetting.NOTIFICATION_ALWAYS_SHOW, false);
//            CBWatcherService.startCBService(context, forceShowNotification);
            List<ClipObject> clipObjects = db.getClipHistory(0);
            CBWatcherService.startCBService(context, true);
            Storage db = Storage.getInstance(context);
            if (clipObjects.size() > 0) {
                String clips = clipObjects.get(0).getText();
                ClipboardManager cb = (ClipboardManager) context.getSystemService(context.CLIPBOARD_SERVICE);
                cb.setText(clips);
            }
        }
    }
}
