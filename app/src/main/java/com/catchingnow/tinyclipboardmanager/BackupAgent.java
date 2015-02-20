package com.catchingnow.tinyclipboardmanager;

import android.app.backup.BackupAgentHelper;
import android.app.backup.SharedPreferencesBackupHelper;
import android.util.Log;

/**
 * Created by heruoxin on 15/2/20.
 */
public class BackupAgent  extends BackupAgentHelper {
    static final String BACKUP_PREFERENCE = "backup_preference";
    static final String BACKUP_DATABASE = "backup_database";
    static final String DEFAULT_PREFERENCE = "com.catchingnow.tinyclipboardmanager_preferences";

    @Override
    public void onCreate() {
        SharedPreferencesBackupHelper sharedPreferencesBackupHelper =
                new SharedPreferencesBackupHelper(
                        this,
                        DEFAULT_PREFERENCE
                        );
        addHelper(BACKUP_PREFERENCE, sharedPreferencesBackupHelper);
        super.onCreate();
    }
}
