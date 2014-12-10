package com.catchingnow.clippingnow;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

public class LaunchServiceAtStartup extends BroadcastReceiver {
    public LaunchServiceAtStartup() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            ComponentName service = context.startService(new Intent(context, CBWatcherService.class));
        }
    }
}
