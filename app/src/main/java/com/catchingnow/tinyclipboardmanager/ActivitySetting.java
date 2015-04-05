package com.catchingnow.tinyclipboardmanager;

import android.app.AlertDialog;
import android.app.backup.BackupManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.LinearLayout;

public class ActivitySetting extends MyPreferenceActivity {

    public final static String PREF_NOTIFICATION_SHOW = "pref_notification_show";
    public final static String PREF_NOTIFICATION_PIN = "pref_notification_pin";
    public final static String PREF_NOTIFICATION_PRIORITY = "pref_notification_priority";
    public final static String PREF_START_SERVICE = "pref_start_service";
    public final static String PREF_LONG_CLICK_BEHAVIOR = "pref_long_click_behavior";
    public final static String PREF_SAVE_DATES = "pref_save_dates";
    public static final String PREF_FLOATING_BUTTON = "pref_floating_button_switch";
    public static final String PREF_FLOATING_BUTTON_ALWAYS_SHOW = "pref_floating_button_always_show";
    //    public final static String PREF_LAST_ACTIVE_THIS = "pref_last_active_this";
    private Toolbar mActionBar;
    private SharedPreferences.OnSharedPreferenceChangeListener myPrefChangeListener;
    private SharedPreferences preferences;
    private Context context;

    public ActivitySetting() {
        myPrefChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                                  String key) {
                switch (key) {
                    case PREF_NOTIFICATION_SHOW:
                    case PREF_NOTIFICATION_PRIORITY:
                    case PREF_NOTIFICATION_PIN:
                        CBWatcherService.startCBService(context, true);
                        break;
                    case PREF_START_SERVICE:
                        CBWatcherService.startCBService(context, true);
                        if (!sharedPreferences.getBoolean(PREF_START_SERVICE, true)) {
                            context.stopService(new Intent(context, FloatingWindowService.class));
                            break;
                        }
                    case PREF_FLOATING_BUTTON:
                    case PREF_FLOATING_BUTTON_ALWAYS_SHOW:
                        if (sharedPreferences.getBoolean(PREF_FLOATING_BUTTON, false)) {
                            if (sharedPreferences.getString(PREF_FLOATING_BUTTON_ALWAYS_SHOW, "always").equals("always")) {
                                context.startService(new Intent(context, FloatingWindowService.class));
                            } else {
                                checkAccessibilityPermission();
                                context.stopService(new Intent(context, FloatingWindowService.class));
                            }
                        } else {
                            context.stopService(new Intent(context, FloatingWindowService.class));
                        }
                        break;
                    case PREF_SAVE_DATES:
                        int i = Integer.parseInt(sharedPreferences.getString(key, "7"));
                        if (i > 9998) {
                            findPreference(key).setSummary(getString(R.string.pref_storage_summary_infinite));
                        } else {
                            findPreference(key).setSummary(String.format(getString(R.string.pref_storage_summary_days), i));
                        }
                        break;
                    case PREF_LONG_CLICK_BEHAVIOR:
                        break;
                }
            }
        };
    }

    public void initSharedPrefListener() {
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(myPrefChangeListener);
//        SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(this);
//        preference.edit().putLong(PREF_LAST_ACTIVE_THIS, new Date().getTime()).commit();
    }

    private boolean checkAccessibilityPermission() {
        if (MyUtil.isAccessibilityEnabled(context, MyUtil.PACKAGE_NAME)) {
            Log.i(MyUtil.PACKAGE_NAME, "checkAccessibilityPermission: true");
            return true;
        }
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.accessibility_dialog_title))
                .setMessage(getString(R.string.accessibility_dialog_summary))
                .setPositiveButton(getString(R.string.accessibility_dialog_ok), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                                startActivityForResult(intent, 0);
                            }
                        }
                )
                .setNegativeButton(getString(R.string.dialog_cancel), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                PreferenceManager.getDefaultSharedPreferences(context)
                                        .edit()
                                        .putString(PREF_FLOATING_BUTTON_ALWAYS_SHOW, "always")
                                        .apply();
                            }
                        }
                )
                .setCancelable(false)
                .create()
                .show();
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = this.getBaseContext();
        addPreferencesFromResource(R.xml.preference);
        mActionBar.setTitle(getTitle());

        preferences = PreferenceManager.getDefaultSharedPreferences(context);

    }

    @Override
    protected void onResume() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mActionBar.setElevation(MyUtil.dip2px(context, 4));
        } else {
            View mToolbarShadow = findViewById(R.id.my_toolbar_shadow);
            if (mToolbarShadow != null) {
                mToolbarShadow.setVisibility(View.VISIBLE);
            }
        }

        super.onResume();

        if (!preferences.getString(PREF_FLOATING_BUTTON_ALWAYS_SHOW, "always").equals("always")) {
            Log.i(MyUtil.PACKAGE_NAME, "" + preferences.getString(PREF_FLOATING_BUTTON_ALWAYS_SHOW, "always"));
            checkAccessibilityPermission();
        }

        initSharedPrefListener();
    }

    @Override
    protected void onPause() {
        super.onPause();

    }

    @Override
    public void setContentView(int layoutResID) {
        ViewGroup contentView = (ViewGroup) LayoutInflater.from(this).inflate(
                R.layout.activity_setting, new LinearLayout(this), false);

        mActionBar = (Toolbar) contentView.findViewById(R.id.my_toolbar);
        mActionBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        ViewGroup contentWrapper = (ViewGroup) contentView.findViewById(R.id.content_wrapper);
        LayoutInflater.from(this).inflate(layoutResID, contentWrapper, true);

        getWindow().setContentView(contentView);
    }

}
