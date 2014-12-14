package com.catchingnow.tinyclipboards;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

public class LaunchServiceAtStartup extends BroadcastReceiver {
    public LaunchServiceAtStartup() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent i = new Intent(context, CBWatcherService.class);
        context.startService(i);
    }
}
