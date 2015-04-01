package com.catchingnow.tinyclipboardmanager;

import android.animation.Animator;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.graphics.PixelFormat;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

//https://github.com/EatHeat/FloatingExample

public class FloatingWindowService extends Service {

    public static final String FLOATING_WINDOW_X = "floating_window_x";
    public static final String FLOATING_WINDOW_Y = "floating_window_y";

    private final static int DURATION_TIME = 400;

    private SharedPreferences preference;
    private WindowManager windowManager;
    private View floatingView;
    private WindowManager.LayoutParams params;
    private Handler handler;

    private boolean checkPermission() {
        return (
                PreferenceManager.getDefaultSharedPreferences(this).getBoolean(ActivitySetting.PREF_FLOATING_BUTTON, false)
                &&
                PreferenceManager.getDefaultSharedPreferences(this).getBoolean(ActivitySetting.PREF_START_SERVICE, true)
        );
    }

    private void FWHideAnimate() {
        Log.i(MyUtil.PACKAGE_NAME, "FWHideAnimate");
        floatingView.animate().scaleX(0).scaleY(0).setDuration(DURATION_TIME);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                windowManager.removeView(floatingView);
            }
        }, DURATION_TIME);
    }

    private void FWShowAnimate() {
        Log.i(MyUtil.PACKAGE_NAME, "FWShowAnimate");
        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        params.x = preference.getInt(FLOATING_WINDOW_X, 120);
        params.y = preference.getInt(FLOATING_WINDOW_Y, 120);
        params.width = getResources().getDimensionPixelSize(R.dimen.abc_action_bar_default_height_material);
        params.height = getResources().getDimensionPixelSize(R.dimen.abc_action_bar_default_height_material);
        windowManager.addView(floatingView, params);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                floatingView.animate().scaleX(1).scaleY(1).setDuration(DURATION_TIME);
            }
        }, DURATION_TIME);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!checkPermission()) {
            Log.i(MyUtil.PACKAGE_NAME, "STOPPED");
            stopSelf();
            return START_NOT_STICKY;
        }
        return START_STICKY;
    }

    @Override
    public void onCreate() {

        super.onCreate();

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        preference = MyUtil.getLocalSharedPreferences(this);
        handler = new Handler();
        LayoutInflater layoutInflater =
                (LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final Intent i = new Intent(this, ClipObjectActionBridge.class)
                .putExtra(ClipObjectActionBridge.ACTION_CODE, ClipObjectActionBridge.ACTION_OPEN_MAIN_DIALOG);

        floatingView =
                layoutInflater.inflate(R.layout.floating_window, null);
        floatingView.setScaleX(0);
        floatingView.setScaleY(0);
        FWShowAnimate();

        try {
            floatingView.setOnTouchListener(new View.OnTouchListener() {
                private int initialX;
                private int initialY;
                private float initialTouchX;
                private float initialTouchY;

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:

                            initialX = params.x;
                            initialY = params.y;
                            initialTouchX = event.getRawX();
                            initialTouchY = event.getRawY();
                            break;
                        case MotionEvent.ACTION_UP:
                            int diffX = Math.abs(params.x - preference.getInt(FLOATING_WINDOW_X, 120));
                            int diffY = Math.abs(params.y - preference.getInt(FLOATING_WINDOW_Y, 120));
                            int smallLength = MyUtil.dip2px(FloatingWindowService.this, 5);
                            if (diffX < smallLength && diffY < smallLength) {
                                startService(i);
                            } else {
                                preference.edit()
                                        .putInt(FLOATING_WINDOW_X, params.x)
                                        .putInt(FLOATING_WINDOW_Y, params.y)
                                        .apply();
                            }
                            break;
                        case MotionEvent.ACTION_MOVE:
                            params.x = initialX + (int) (event.getRawX() - initialTouchX);
                            params.y = initialY + (int) (event.getRawY() - initialTouchY);
                            windowManager.updateViewLayout(floatingView, params);
                            break;
                    }
                    return false;
                }
            });
        } catch (Exception e) {
            Log.e(MyUtil.PACKAGE_NAME, e.toString());
        }

    }

    @Override
    public void onDestroy() {
        Log.i(MyUtil.PACKAGE_NAME, "onDestroy");
        FWHideAnimate();
        super.onDestroy();
    }

}

