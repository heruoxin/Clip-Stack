package com.catchingnow.tinyclipboardmanager;

import android.app.backup.BackupAgentHelper;
import android.app.backup.FileBackupHelper;
import android.app.backup.SharedPreferencesBackupHelper;
import android.content.Context;
import android.util.Log;

import java.io.File;

/**
 * Created by heruoxin on 15/2/20.
 */
public class GoogleBackupAgent extends BackupAgentHelper {
    static final String BACKUP_PREFERENCE = "backup_preference";
    static final String BACKUP_DATABASE = "backup_database";
    static final String DEFAULT_PREFERENCE = "com.catchingnow.tinyclipboardmanager_preferences";

    @Override
    public void onCreate() {
        DbBackupHelper dataBaseBackupHelper =
                new DbBackupHelper(
                        this,
                        Storage.StorageHelper.DATABASE_NAME
                );
        SharedPreferencesBackupHelper mainSharedPreferencesBackupHelper =
                new SharedPreferencesBackupHelper(
                        this,
                        DEFAULT_PREFERENCE
                        );
        addHelper(BACKUP_PREFERENCE, mainSharedPreferencesBackupHelper);
        addHelper(BACKUP_DATABASE, dataBaseBackupHelper);
        super.onCreate();
    }
}

class DbBackupHelper extends FileBackupHelper {

    public DbBackupHelper(Context context, String dbName) {
        super(context, context.getDatabasePath(dbName).getAbsolutePath());
        Log.v(MyUtil.PACKAGE_NAME, "DbBackupHelper: "+context.getDatabasePath(dbName).getAbsolutePath());
    }
}