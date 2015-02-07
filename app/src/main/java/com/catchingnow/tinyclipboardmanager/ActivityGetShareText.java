package com.catchingnow.tinyclipboardmanager;

import android.app.ActivityManager;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.widget.Toast;


public class ActivityGetShareText extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //receive system's text share
        Intent intent = getIntent();
        if (Intent.ACTION_SEND.equals(intent.getAction()) && "text/plain".equals(intent.getType())) {
            copyText(intent.getStringExtra(Intent.EXTRA_TEXT));
            return;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            finishAndRemoveTask();
        } else {
            finish();
        }
    }

    protected void copyText(String clip) {
        if (clip == null) {
            return;
        }
        if ("".equals(clip)) {
            return;
        }
        //copy clips to clipboard
        ClipboardManager cb = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        cb.setText(clip);

        //check service statues
        //if no service, manually add clip to db
        if (!isMyServiceRunning(CBWatcherService.class)) {
            Storage db = Storage.getInstance(this);
            db.modifyClip(null, clip, Storage.NOTIFICATION_VIEW);
        }

        if (clip.length() > 15) {
            clip = clip.substring(0, 15) + "…";
        }
        Toast.makeText(this,
                getString(R.string.toast_front_string) + clip + "\n" + getString(R.string.toast_end_string),
                Toast.LENGTH_LONG
        ).show();

    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

}
