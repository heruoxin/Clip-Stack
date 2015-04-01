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

    private SharedPreferences preference;
    private WindowManager windowManager;
    private View floatingView;
    private WindowManager.LayoutParams params;
    private Handler handler;

    private final static int DURATION_TIME = 400;

    private int foregroundActivityCount = 0;
    private boolean onAnimate = false;
    private boolean isAttached = false;

    private boolean checkPermission() {
        return (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(ActivitySetting.PREF_FLOATING_BUTTON, false));
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!checkPermission()) {
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
        final Intent i = new Intent(this, ActivityMainDialog.class)
                .putExtra(ActivityMain.EXTRA_IS_FROM_NOTIFICATION, true)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                .addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);

        floatingView =
                layoutInflater.inflate(R.layout.floating_window, null);

        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.TOP | Gravity.LEFT;
        params.x = preference.getInt(FLOATING_WINDOW_X, 120);
        params.y = preference.getInt(FLOATING_WINDOW_Y, 120);

        windowManager.addView(floatingView, params);
        isAttached = true;


        BroadcastReceiver onClosedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(final Context context, Intent intent) {
                foregroundActivityCount -= 1;
                if (foregroundActivityCount < 0) foregroundActivityCount = 0;
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (foregroundActivityCount != 0) return;
                        if (!isAttached) return;
                        if (onAnimate) return;
                        Log.i(MyUtil.PACKAGE_NAME, "onClosedReceiver");
                        onAnimate = true;
                        params.x = preference.getInt(FLOATING_WINDOW_X, 120);
                        params.y = preference.getInt(FLOATING_WINDOW_Y, 120);
                        params.width = getResources().getDimensionPixelSize(R.dimen.abc_action_bar_default_height_material);
                        params.height = getResources().getDimensionPixelSize(R.dimen.abc_action_bar_default_height_material);
                        windowManager.updateViewLayout(floatingView, params);
                        Log.i(MyUtil.PACKAGE_NAME, "onClosedReceiver updateViewLayout");
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                floatingView.animate().scaleX(1).scaleY(1).setDuration(DURATION_TIME)
                                        .setListener(new Animator.AnimatorListener() {
                                            @Override
                                            public void onAnimationStart(Animator animation) {

                                            }

                                            @Override
                                            public void onAnimationEnd(Animator animation) {
                                                onAnimate = false;
                                                Log.i(MyUtil.PACKAGE_NAME, "onClosedReceiver onAnimate false");
                                            }

                                            @Override
                                            public void onAnimationCancel(Animator animation) {
                                                onAnimate = false;
                                                Log.i(MyUtil.PACKAGE_NAME, "onClosedReceiver onAnimate false");
                                            }

                                            @Override
                                            public void onAnimationRepeat(Animator animation) {

                                            }
                                        });
                                Log.i(MyUtil.PACKAGE_NAME, "onClosedReceiver scaleX");
                            }
                        }, DURATION_TIME);
                    }
                }, 100);
            }
        };

        BroadcastReceiver onOpenedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(final Context context, Intent intent) {
                foregroundActivityCount += 1;
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (foregroundActivityCount <= 0) return;
                        if (!isAttached) return;
                        if (onAnimate) return;
                        Log.i(MyUtil.PACKAGE_NAME, "onOpenedReceiver");
                        onAnimate = true;
                        floatingView.animate().scaleX(0).scaleY(0).setDuration(DURATION_TIME)
                                .setListener(new Animator.AnimatorListener() {
                                    @Override
                                    public void onAnimationStart(Animator animation) {

                                    }

                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        if (!isAttached) return;
                                        params.x = 0;
                                        params.y = 0;
                                        params.width = 0;
                                        params.height = 0;
                                        windowManager.updateViewLayout(floatingView, params);
                                        Log.i(MyUtil.PACKAGE_NAME, "onOpenedReceiver updateViewLayout");
                                        onAnimate = false;
                                    }

                                    @Override
                                    public void onAnimationCancel(Animator animation) {
                                        if (!isAttached) return;
                                        params.x = 0;
                                        params.y = 0;
                                        params.width = 0;
                                        params.height = 0;
                                        windowManager.updateViewLayout(floatingView, params);
                                        Log.i(MyUtil.PACKAGE_NAME, "onOpenedReceiver updateViewLayout");
                                        onAnimate = false;
                                    }

                                    @Override
                                    public void onAnimationRepeat(Animator animation) {

                                    }
                                });
                        Log.i(MyUtil.PACKAGE_NAME, "onOpenedReceiver scaleX");
                    }
                }, 100);
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(onOpenedReceiver, new IntentFilter(MyActionBarActivity.ACTIVITY_OPENED));
        LocalBroadcastManager.getInstance(this).registerReceiver(onClosedReceiver, new IntentFilter(MyActionBarActivity.ACTIVITY_CLOSED));


        try {
            floatingView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(i);
                }
            });
            floatingView.setOnTouchListener(new View.OnTouchListener() {
                private int initialX;
                private int initialY;
                private float initialTouchX;
                private float initialTouchY;

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:

                            // Get current time in nano seconds.

                            initialX = params.x;
                            initialY = params.y;
                            initialTouchX = event.getRawX();
                            initialTouchY = event.getRawY();
                            break;
                        case MotionEvent.ACTION_UP:
                            preference.edit()
                                    .putInt(FLOATING_WINDOW_X, params.x)
                                    .putInt(FLOATING_WINDOW_Y, params.y)
                                    .apply();
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
        if (!isAttached) windowManager.removeView(floatingView);
        super.onDestroy();
    }

}

