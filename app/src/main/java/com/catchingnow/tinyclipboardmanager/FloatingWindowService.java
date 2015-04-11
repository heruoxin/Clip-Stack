package com.catchingnow.tinyclipboardmanager;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.graphics.PixelFormat;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
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

    private boolean openedSetting = false;
    private boolean longClickAble = true;
    private int onStartCommandReturn;

    private boolean checkPermission() {
        return (
                PreferenceManager.getDefaultSharedPreferences(this).getBoolean(ActivitySetting.PREF_FLOATING_BUTTON, false) &&
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

        params.x = preference.getInt(FLOATING_WINDOW_X, 300);
        params.y = preference.getInt(FLOATING_WINDOW_Y, 0);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            params.width = getResources().getDimensionPixelSize(R.dimen.abc_action_bar_default_height_material) + MyUtil.dip2px(this, 4);
            params.height = getResources().getDimensionPixelSize(R.dimen.abc_action_bar_default_height_material) + MyUtil.dip2px(this, 8);
        } else {
            params.width = getResources().getDimensionPixelSize(R.dimen.abc_action_bar_default_height_material);
            params.height = getResources().getDimensionPixelSize(R.dimen.abc_action_bar_default_height_material);
            params.alpha = (float) 0.9;
        }
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
        return onStartCommandReturn;
    }

    @Override
    public void onCreate() {

        super.onCreate();
        if (checkPermission()) {
            onStartCommandReturn = START_STICKY;
        } else {
            onStartCommandReturn = START_NOT_STICKY;
            Log.i(MyUtil.PACKAGE_NAME, "STOPPED");
            stopSelf();
        }

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        preference = MyUtil.getLocalSharedPreferences(this);
        handler = new Handler();
        LayoutInflater layoutInflater =
                (LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final Intent dialogIntent = new Intent(this, ClipObjectActionBridge.class)
                .putExtra(ClipObjectActionBridge.ACTION_CODE, ClipObjectActionBridge.ACTION_OPEN_MAIN_DIALOG);
        final Intent settingIntent = new Intent(this, ActivitySetting.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        floatingView =
                layoutInflater.inflate(R.layout.floating_window, null);
        floatingView.setScaleX(0);
        floatingView.setScaleY(0);

        FWShowAnimate();

        try {
            floatingView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (!longClickAble) return false;
                    startActivity(settingIntent);
                    floatingView.playSoundEffect(0);
                    //MyUtil.vibrator(FloatingWindowService.this);
                    openedSetting = true;
                    return true;
                }
            });
            floatingView.setOnTouchListener(new View.OnTouchListener() {
                private int initialX;
                private int initialY;
                private float initialTouchX;
                private float initialTouchY;
                private int diffX;
                private int diffY;
                private int smallLength;

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:

                            initialX = params.x;
                            initialY = params.y;
                            initialTouchX = event.getRawX();
                            initialTouchY = event.getRawY();
                            smallLength = MyUtil.dip2px(FloatingWindowService.this, 5);
                            longClickAble = true;
                            break;
                        case MotionEvent.ACTION_UP:
                            diffX = Math.abs(params.x - initialX);
                            diffY = Math.abs(params.y - initialY);
                            if (diffX < smallLength && diffY < smallLength) {
                                if (!openedSetting) {
                                    startService(dialogIntent);
                                    floatingView.playSoundEffect(0);
                                    //MyUtil.vibrator(FloatingWindowService.this);
                                }
                                openedSetting = false;
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
                            diffX = Math.abs(params.x - initialX);
                            diffY = Math.abs(params.y - initialY);
                            if (diffX > smallLength || diffY > smallLength) {
                                longClickAble = false;
                            }
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

