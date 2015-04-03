package com.catchingnow.tinyclipboardmanager;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.accessibilityservice.AccessibilityServiceInfoCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.AccessibilityDelegateCompat;
import android.support.v4.view.accessibility.AccessibilityEventCompat;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.support.v4.view.accessibility.AccessibilityNodeProviderCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.List;

public class ExperienceEnhanceService extends AccessibilityService {

    private Context context;
    private SharedPreferences preferences;
    private Handler handler;

    private boolean isFWRunning = false;
    private boolean wakedUpCBService = false;

    private void startFloatingWindow() {
        if (isFWRunning) return;
        if (preferences.getString(ActivitySetting.PREF_FLOATING_BUTTON_ALWAYS_SHOW, "always").equals("always")) {
            isFWRunning = preferences.getBoolean(ActivitySetting.PREF_FLOATING_BUTTON, true);
            return;
        }
        isFWRunning = true;
        this.startService(new Intent(this, FloatingWindowService.class));
    }

    private void stopFloatingWindow() {
        if (!isFWRunning) return;
        if (preferences.getString(ActivitySetting.PREF_FLOATING_BUTTON_ALWAYS_SHOW, "always").equals("always")) {
            isFWRunning = preferences.getBoolean(ActivitySetting.PREF_FLOATING_BUTTON, true);
            return;
        }
        isFWRunning = false;
        this.stopService(new Intent(this, FloatingWindowService.class));
    }

    @Override
    protected void onServiceConnected() {
        context = this;
        handler = new Handler();
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        BroadcastReceiver restartCBWatcherService = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (wakedUpCBService) return;
                CBWatcherService.startCBService(context, false);
                wakedUpCBService = true;
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        wakedUpCBService = false;
                    }
                }, 3*1000);
            }
        };
        LocalBroadcastManager.getInstance(context).registerReceiver(
                restartCBWatcherService,
                new IntentFilter(CBWatcherService.ON_DESTROY)
        );
        super.onServiceConnected();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {

        //Check package name
        AccessibilityNodeInfo nodeInfo = event.getSource();
        if (nodeInfo != null) {
            String packageName = String.valueOf(nodeInfo.getPackageName());
            if (packageName.contains("catchingnow.tinyclipboardmanager")) {
                stopFloatingWindow();
                return;
            }
        }

        //FW
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AccessibilityNodeInfo findFocus = findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
            if (findFocus != null &&
                    findFocus.isEditable()) {
                Log.i("AccessibilityNodeInfo", "true");
                startFloatingWindow();
            } else {
                Log.i("AccessibilityNodeInfo", "false");
                stopFloatingWindow();
            }
        }
    }

    @Override
    public void onInterrupt() {
        Log.i("ACCESSIBILITY", "onInterrupt");
    }
}
