package com.catchingnow.tinyclipboardmanager;

import android.app.backup.BackupManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

public class ActivitySetting extends MyPreferenceActivity {

    public final static String PREF_NOTIFICATION_SHOW = "pref_notification_show";
    public final static String PREF_NOTIFICATION_PIN = "pref_notification_pin";
    public final static String PREF_NOTIFICATION_PRIORITY = "pref_notification_priority";
    public final static String PREF_START_SERVICE = "pref_start_service";
    public final static String PREF_LONG_CLICK_BEHAVIOR = "pref_long_click_behavior";
    public final static String PREF_SAVE_DATES = "pref_save_dates";
//    public final static String PREF_LAST_ACTIVE_THIS = "pref_last_active_this";
    private Toolbar mActionBar;
    private SharedPreferences.OnSharedPreferenceChangeListener myPrefChangeListener;
    private Context context;

    public ActivitySetting() {
        myPrefChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                                  String key) {
                switch (key) {
                    case PREF_START_SERVICE:
                    case PREF_NOTIFICATION_SHOW:
                    case PREF_NOTIFICATION_PRIORITY:
                    case PREF_NOTIFICATION_PIN:
                        CBWatcherService.startCBService(context, true);
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = this.getBaseContext();
        addPreferencesFromResource(R.xml.preference);
        mActionBar.setTitle(getTitle());
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
