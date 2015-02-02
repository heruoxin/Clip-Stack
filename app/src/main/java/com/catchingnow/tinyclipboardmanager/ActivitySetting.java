package com.catchingnow.tinyclipboardmanager;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Switch;

public class ActivitySetting extends PreferenceActivity {

    public final static String NOTIFICATION_ALWAYS_SHOW = "pref_notification_always_show";
    public final static String SERVICE_STATUS = "pref_start_service";
    public final static String SAVE_DATES = "pref_save_dates";
    public final static String LAST_ACTIVE_THIS = "pref_last_active_this";
    private Toolbar mActionBar;
    private SharedPreferences.OnSharedPreferenceChangeListener myPrefListener;
    private Context c;

    public ActivitySetting() {
        myPrefListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                                  String key) {
                switch (key) {
                    case SERVICE_STATUS:
                    case NOTIFICATION_ALWAYS_SHOW:
                        CBWatcherService.startCBService(c, true);
                        break;
                    case SAVE_DATES:
                        int i = Integer.parseInt(sharedPreferences.getString(key, "7"));
                        if (i > 9998) {
                            findPreference(key).setSummary(getString(R.string.pref_storage_summary_infinite));
                        } else {
                            findPreference(key).setSummary(String.format(getString(R.string.pref_storage_summary_days), i));
                        }
                        break;
                }
            }
        };
    }

    public void initSharedPrefListener(){
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(myPrefListener);
//        SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(this);
//        preference.edit().putLong(LAST_ACTIVE_THIS, new Date().getTime()).commit();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        c = this.getBaseContext();
        addPreferencesFromResource(R.xml.preference);
        mActionBar.setTitle(getTitle());
        initSharedPrefListener();
    }

    @Override
    protected void onResume() {
        super.onResume();
        initSharedPrefListener();
        CBWatcherService.startCBService(this, false, 1);
    }

    @Override
    protected void onPause() {
        super.onPause();
        initSharedPrefListener();
        CBWatcherService.startCBService(this, false, -1);

    }

    @Override
    public void setContentView(int layoutResID) {
        ViewGroup contentView = (ViewGroup) LayoutInflater.from(this).inflate(
                R.layout.activity_setting, new LinearLayout(this), false);

        mActionBar = (Toolbar) contentView.findViewById(R.id.action_bar);
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