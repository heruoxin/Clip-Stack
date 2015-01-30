package com.catchingnow.tinyclipboards;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class LaunchServiceAtStartup extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(context);
            boolean forceShowNotification = preference.getBoolean(ActivitySetting.NOTIFICATION_ALWAYS_SHOW, false);
            CBWatcherService.startCBService(context, forceShowNotification);
        }
    }
}
