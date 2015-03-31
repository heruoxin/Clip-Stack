package com.catchingnow.tinyclipboardmanager;

import android.animation.Animator;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.graphics.PixelFormat;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;

//https://github.com/EatHeat/FloatingExample

public class FloatingWindowService extends Service {

    public static final String FLOATING_WINDOW_X = "floating_window_x";
    public static final String FLOATING_WINDOW_Y = "floating_window_y";

    private SharedPreferences preference;
    private WindowManager windowManager;
    private View floatingView;
    private WindowManager.LayoutParams params;

    private boolean isFloatingWindowShow = false;

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
            Log.v(MyUtil.PACKAGE_NAME, "checkPermission stopped");
            stopSelf();
        } else {
            Log.v(MyUtil.PACKAGE_NAME, "checkPermission OK");
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {

        super.onCreate();

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        preference = MyUtil.getLocalSharedPreferences(this);
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
        isFloatingWindowShow = true;

        LocalBroadcastManager.getInstance(this)
                .registerReceiver(new BroadcastReceiver() {
                                      @Override
                                      public void onReceive(final Context context, Intent intent) {
                                          if (!isFloatingWindowShow) {
                                              floatingView.animate().scaleX(1).scaleY(1)
                                              .setListener(new Animator.AnimatorListener() {
                                                  @Override
                                                  public void onAnimationStart(Animator animation) {

                                                  }

                                                  @Override
                                                  public void onAnimationEnd(Animator animation) {
                                                      isFloatingWindowShow = true;
                                                  }

                                                  @Override
                                                  public void onAnimationCancel(Animator animation) {

                                                  }

                                                  @Override
                                                  public void onAnimationRepeat(Animator animation) {

                                                  }
                                              });
                                          }
                                      }
                                  },
                        new IntentFilter(MyActionBarActivity.DIALOG_CLOSED));

        LocalBroadcastManager.getInstance(this)
                .registerReceiver(new BroadcastReceiver() {
                                      @Override
                                      public void onReceive(final Context context, Intent intent) {
                                          if (isFloatingWindowShow) {
                                              floatingView.animate().scaleX(0).scaleY(0)
                                                      .setListener(new Animator.AnimatorListener() {
                                                          @Override
                                                          public void onAnimationStart(Animator animation) {

                                                          }

                                                          @Override
                                                          public void onAnimationEnd(Animator animation) {
                                                              isFloatingWindowShow = false;
                                                          }

                                                          @Override
                                                          public void onAnimationCancel(Animator animation) {

                                                          }

                                                          @Override
                                                          public void onAnimationRepeat(Animator animation) {

                                                          }
                                                      });
                                          }
                                      }
                                  },
                        new IntentFilter(MyActionBarActivity.DIALOG_OPENED));

        try {
            floatingView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(i);
                }
            });
            floatingView.setOnTouchListener(new View.OnTouchListener() {
                private WindowManager.LayoutParams paramsF = params;
                private int initialX;
                private int initialY;
                private float initialTouchX;
                private float initialTouchY;

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:

                            // Get current time in nano seconds.

                            initialX = paramsF.x;
                            initialY = paramsF.y;
                            initialTouchX = event.getRawX();
                            initialTouchY = event.getRawY();
                            break;
                        case MotionEvent.ACTION_UP:
                            preference.edit()
                                    .putInt(FLOATING_WINDOW_X, paramsF.x)
                                    .putInt(FLOATING_WINDOW_Y, paramsF.y)
                                    .apply();
                            break;
                        case MotionEvent.ACTION_MOVE:
                            paramsF.x = initialX + (int) (event.getRawX() - initialTouchX);
                            paramsF.y = initialY + (int) (event.getRawY() - initialTouchY);
                            windowManager.updateViewLayout(floatingView, paramsF);
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
        if (floatingView != null) windowManager.removeView(floatingView);
        super.onDestroy();
    }

}

