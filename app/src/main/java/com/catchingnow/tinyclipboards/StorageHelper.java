package com.catchingnow.clippingnow;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Created by heruoxin on 14/12/9.
 */

public class StorageHelper extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 2;
    private static final String DATABASE_NAME = "clippingnow.db";
    private static final String TABLE_NAME = "cliphistory";
    private static final String CLIP_STRING = "history";
    private static final String CLIP_DATE = "date";
    private static final String TABLE_CREATE =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    CLIP_DATE + " TIMESTAMP, " +
                    CLIP_STRING + " TEXT);";
    public StorageHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.v("StorageHelper", "SQL updated from" + oldVersion + "to" + newVersion);
    }
}

