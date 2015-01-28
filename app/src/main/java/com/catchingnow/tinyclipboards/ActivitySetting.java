package com.catchingnow.tinyclipboards;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

public class ActivitySetting extends PreferenceActivity {

    public final static String NOTIFICATION_ALWAYS_SHOW = "pref_notification_always_show";
    public final static String SERVICE_STATUS = "pref_start_service";
    private Toolbar mActionBar;
    private SharedPreferences.OnSharedPreferenceChangeListener myPrefListner;
    private Context c;

    public ActivitySetting() {
        myPrefListner = new SharedPreferences.OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                                  String key) {
                switch (key) {
                    case SERVICE_STATUS:
                        CBWatcherService.toggleService(c, sharedPreferences.getBoolean(key, true));
                        break;
                    case NOTIFICATION_ALWAYS_SHOW:
                        if (sharedPreferences.getBoolean(key, false)) {
                            findPreference(key).setSummary("Always show.");
                        } else {
                            findPreference(key).setSummary("When clipboard changed.");
                        }
                        Intent intent = new Intent(c, CBWatcherService.class);
                        intent.putExtra(CBWatcherService.INTENT_EXTRA_FORCE_SHOW_NOTIFICATION, true);
                        startService(intent);
                        break;
                }
            }
        };
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        c = this.getBaseContext();
        addPreferencesFromResource(R.xml.preference);
        mActionBar.setTitle(getTitle());
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(myPrefListner);
    }
    @Override
    protected void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(myPrefListner);
    }



    @Override
    protected void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(myPrefListner);

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