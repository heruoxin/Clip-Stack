package com.catchingnow.tinyclipboardmanager;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.support.v4.accessibilityservice.AccessibilityServiceInfoCompat;
import android.support.v4.view.AccessibilityDelegateCompat;
import android.support.v4.view.accessibility.AccessibilityEventCompat;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.support.v4.view.accessibility.AccessibilityNodeProviderCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.List;

public class MyAccessibilityService extends AccessibilityService {

    private void startFloatingWindow() {
        this.startService(new Intent(this, FloatingWindowService.class));
    }

    private void stopFloatingWindow() {
        this.stopService(new Intent(this, FloatingWindowService.class));
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getSource() != null) {
            String packageName = String.valueOf(event.getSource().getPackageName());
            if (packageName.contains("catchingnow.tinyclipboardmanager")) {
                stopFloatingWindow();
                return;
            }
        }
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

    @Override
    public void onInterrupt() {
        Log.i("ACCESSIBILITY", "onInterrupt");
    }
}
