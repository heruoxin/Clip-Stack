package com.catchingnow.tinyclipboards;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class LaunchServiceAtStartup extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Intent i = new Intent(context, CBWatcherService.class);
            SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(context);
            boolean forceShowNotification = preference.getBoolean(ActivitySetting.NOTIFICATION_ALWAYS_SHOW, false);
            i.putExtra(CBWatcherService.INTENT_EXTRA_FORCE_SHOW_NOTIFICATION, forceShowNotification);
            context.startService(i);
        }
    }
}
