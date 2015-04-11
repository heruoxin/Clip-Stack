package com.catchingnow.tinyclipboardmanager;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.internal.widget.TintImageView;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;

import java.util.ArrayList;

/**
 * Created by heruoxin on 15/2/28.
 */
public class MyActionBarActivity extends ActionBarActivity {
    public static final String ACTIVITY_OPENED = "activity_opened";
    public static final String ACTIVITY_CLOSED = "activity_closed";

    protected SharedPreferences preference;

    //Fix LG support V7 bug:
    //https://code.google.com/p/android/issues/detail?id=78154
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU && "LGE".equalsIgnoreCase(Build.BRAND)) {
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU && "LGE".equalsIgnoreCase(Build.BRAND)) {
            openOptionsMenu();
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    //check softKeyboard show or hide
    //http://stackoverflow.com/questions/25216749/softkeyboard-open-and-close-listener-in-an-activity-in-android
    private boolean isKeyboardShow = false;

    private ViewTreeObserver.OnGlobalLayoutListener keyboardLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
            int heightDiff = rootLayout.getRootView().getHeight() - rootLayout.getHeight();
            int contentViewTop = getWindow().findViewById(Window.ID_ANDROID_CONTENT).getHeight();

            if (heightDiff <= contentViewTop) {
                if (!isKeyboardShow) return;
                onHideKeyboard();
                isKeyboardShow = false;

            } else {
                if (isKeyboardShow) return;
                int keyboardHeight = heightDiff - contentViewTop;
                onShowKeyboard(keyboardHeight);
                isKeyboardShow = true;

            }
        }
    };

    private boolean keyboardListenersAttached = false;
    private ViewGroup rootLayout;

    protected void onShowKeyboard(int keyboardHeight) {
    }

    protected void onHideKeyboard() {
    }

    protected void attachKeyboardListeners() {
        if (keyboardListenersAttached) {
            return;
        }

        rootLayout = (ViewGroup) findViewById(android.R.id.content);
        rootLayout.getViewTreeObserver().addOnGlobalLayoutListener(keyboardLayoutListener);

        keyboardListenersAttached = true;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (intent == null) {
            intent = new Intent();
        }
        super.onNewIntent(intent);
    }

    @Override
    public Intent getIntent() {
        Intent intent = super.getIntent();
        if (intent == null) {
            return new Intent();
        } else {
            return intent;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preference = PreferenceManager.getDefaultSharedPreferences(this);
        Toolbar mToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(mToolbar);

        //set toolbar shadow for phone.
        if (getString(R.string.screen_type).contains("phone")) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mToolbar.setElevation(MyUtil.dip2px(this, 4));
//            } else {
//                View mToolbarShadow = findViewById(R.id.my_toolbar_shadow);
//                if (mToolbarShadow != null) {
//                    mToolbarShadow.setVisibility(View.VISIBLE);
//                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (keyboardListenersAttached) {
            rootLayout.getViewTreeObserver().removeGlobalOnLayoutListener(keyboardLayoutListener);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(MyUtil.PACKAGE_NAME, "sendBroadcast ACTIVITY_CLOSED");
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(ACTIVITY_CLOSED));
        CBWatcherService.startCBService(this, -1);
        if (preference.getBoolean(ActivitySetting.PREF_FLOATING_BUTTON, false) &&
                preference.getString(ActivitySetting.PREF_FLOATING_BUTTON_ALWAYS_SHOW, "always").equals("always")
                ) {
            this.startService(new Intent(this, FloatingWindowService.class));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(ACTIVITY_OPENED));
        CBWatcherService.startCBService(this, true, true, 1);
        if (preference.getBoolean(ActivitySetting.PREF_FLOATING_BUTTON, false) &&
                preference.getString(ActivitySetting.PREF_FLOATING_BUTTON_ALWAYS_SHOW, "always").equals("always")
                ) {
            this.stopService(new Intent(this, FloatingWindowService.class));
        }
    }

    public int getScreenWidthPixels() {
        DisplayMetrics displaymetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        return displaymetrics.widthPixels;
    }

    public int getScreenHeightPixels() {
        DisplayMetrics displaymetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        return displaymetrics.heightPixels;
    }

    public int getScreenOrientation() {
        int width = getScreenWidthPixels();
        int height = getScreenHeightPixels();
        if (width > height) {
            Log.v(MyUtil.PACKAGE_NAME, "ORIENTATION_LANDSCAPE");
            return Configuration.ORIENTATION_LANDSCAPE;
        }
        if (width < height) {
            Log.v(MyUtil.PACKAGE_NAME, "ORIENTATION_PORTRAIT");
            return Configuration.ORIENTATION_PORTRAIT;
        }
        Log.v(MyUtil.PACKAGE_NAME, "ORIENTATION_SQUARE");
        return Configuration.ORIENTATION_SQUARE;
    }

    public static void setOverflowButtonColor(final Activity activity, final int imageID) {
        final String overflowDescription = activity.getString(R.string.abc_action_menu_overflow_description);
        final ViewGroup decorView = (ViewGroup) activity.getWindow().getDecorView();
        final ViewTreeObserver viewTreeObserver = decorView.getViewTreeObserver();
        viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                final ArrayList<View> outViews = new ArrayList<View>();
                decorView.findViewsWithText(outViews, overflowDescription,
                        View.FIND_VIEWS_WITH_CONTENT_DESCRIPTION);
                if (outViews.isEmpty()) {
                    return;
                }
                TintImageView overflow=(TintImageView) outViews.get(0);
                //overflow.setColorFilter(Color.CYAN);
                overflow.setImageResource(imageID);
                removeOnGlobalLayoutListener(decorView, this);
            }
        });
    }

    public static void removeOnGlobalLayoutListener(View v, ViewTreeObserver.OnGlobalLayoutListener listener) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            v.getViewTreeObserver().removeGlobalOnLayoutListener(listener);
        }
        else {
            v.getViewTreeObserver().removeOnGlobalLayoutListener(listener);
        }
    }

}
