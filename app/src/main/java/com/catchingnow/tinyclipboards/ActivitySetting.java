package com.catchingnow.tinyclipboards;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import java.util.Date;

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
                        CBWatcherService.toggleService(c, sharedPreferences.getBoolean(key, true));
                        break;
                    case SAVE_DATES:
                        findPreference(key).setSummary(String.format(
                                "Keep clips for %d days.",
                                Integer.parseInt(sharedPreferences.getString(key, "7"))
                        ));
                        break;
                    case NOTIFICATION_ALWAYS_SHOW:
                        if (sharedPreferences.getBoolean(key, false)) {
                            findPreference(key).setSummary("Always show.");
                        } else {
                            findPreference(key).setSummary("Only show when clipboard changed.");
                        }
                        Intent intent = new Intent(c, CBWatcherService.class);
                        intent.putExtra(CBWatcherService.INTENT_EXTRA_FORCE_SHOW_NOTIFICATION, true);
                        startService(intent);
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
    }



    @Override
    protected void onPause() {
        super.onPause();
        initSharedPrefListener();

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