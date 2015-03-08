package com.catchingnow.tinyclipboardmanager;

import android.app.backup.BackupManager;
import android.content.Context;
import android.content.SharedPreferences;
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
                }
                requestBackup(context);
            }
        };
    }

    public void initSharedPrefListener() {
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(myPrefChangeListener);
//        SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(this);
//        preference.edit().putLong(PREF_LAST_ACTIVE_THIS, new Date().getTime()).commit();
    }

    public static void requestBackup(Context context) {
        Log.d(MyUtil.PACKAGE_NAME, "requestBackup");
        BackupManager backupManager = new BackupManager(context);
        backupManager.dataChanged();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = this.getBaseContext();
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
