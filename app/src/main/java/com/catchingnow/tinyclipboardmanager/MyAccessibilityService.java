package com.catchingnow.tinyclipboardmanager;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.support.v4.view.accessibility.AccessibilityEventCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.List;

public class MyAccessibilityService extends AccessibilityService {
    private boolean floatingWindowShow = false;

    private void startFloatingWindow() {
        floatingWindowShow = true;
        this.startService(new Intent(this, FloatingWindowService.class));
    }

    private void stopFloatingWindow() {
        floatingWindowShow = false;
        this.stopService(new Intent(this, FloatingWindowService.class));
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
    }

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        Log.i("onKeyEvent", "");
        Log.i("onKeyEvent", event.getKeyCode() + "");
        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_BACK:
                boolean preFloatingWindowShow = floatingWindowShow;
                stopFloatingWindow();
                return preFloatingWindowShow;
            case KeyEvent.KEYCODE_HOME:
            case KeyEvent.KEYCODE_APP_SWITCH:
            case KeyEvent.KEYCODE_SEARCH:
            case KeyEvent.KEYCODE_POWER:
            case KeyEvent.KEYCODE_NOTIFICATION:
                stopFloatingWindow();
                break;
        }
        return super.onKeyEvent(event);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        switch (event.getEventType()) {
            case AccessibilityEvent.TYPE_VIEW_FOCUSED:
                Log.i("AccessibilityEvent", "TYPE_VIEW_FOCUSED");
                if (event.getSource() != null &&
                        event.getSource().isEditable()) {
                    startFloatingWindow();
                } else {
                    stopFloatingWindow();
                }
                break;
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                Log.i("AccessibilityEvent", "TYPE_WINDOW_STATE_CHANGED");
                stopFloatingWindow();
                break;
        }
    }

    @Override
    public void onInterrupt() {
        Log.i("ACCESSIBILITY", "onInterrupt");
    }
}
