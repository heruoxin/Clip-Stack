package com.catchingnow.tinyclipboardmanager;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by heruoxin on 14/12/9.
 */
public class Storage {
    public final static String UPDATE_DB = "updateDB";
    public final static String UPDATE_DB_ADD = "updateDbAdd";
    public final static String UPDATE_DB_DELETE = "updateDbDelete";
    private static final String TABLE_NAME = "clipHistory";
    private static final String CLIP_STRING = "history";
    private static final String CLIP_DATE = "date";
    private static final String CLIP_IS_STAR = "star";
    private static Storage mInstance = null;
    private StorageHelper dbHelper;
    private SQLiteDatabase db;
    private Context context;
    private ClipboardManager clipboardManager;
    private List<ClipObject> clipsInMemory;
    private boolean isClipsInMemoryChanged = true;

    private Storage(Context context) {
        this.context = context;
        this.clipboardManager = (ClipboardManager) this.context.getSystemService(Context.CLIPBOARD_SERVICE);
        this.dbHelper = new StorageHelper(this.context);
    }

    public static Storage getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new Storage(context.getApplicationContext());
        }
        return mInstance;
    }

    private String sqliteEscape(String keyWord) {
        return DatabaseUtils.sqlEscapeString(keyWord);
//        if ("".equals(keyWord) || keyWord == null) {
//            return keyWord;
//        }
//        return keyWord
//                .replace("'", "''")
//                .replace("/", "//")
//                .replace("[", "/[")
//                .replace("]", "/]")
//                .replace("%", "/%")
//                .replace("&", "/&")
//                .replace("_", "/_")
//                .replace("(", "/(")
//                .replace(")", "/)")
//                ;
    }

    private void open() {
        if (db == null) {
            db = dbHelper.getWritableDatabase();
        } else if (!db.isOpen()) {
            db = dbHelper.getWritableDatabase();
        }
    }

    private void close() {
        if (db != null) {
            if (db.isOpen()) {
                db.close();
            }
        }
    }

    public List<ClipObject> getClipHistory(String queryString) {
        List<ClipObject> allClips = getClipHistory();
        List<ClipObject> queryClips = new ArrayList<>();
        if ("".equals(queryString) || queryString == null) {
            return allClips;
        }
        for (ClipObject clip : allClips) {
            if (clip.getText().contains(queryString)) {
                queryClips.add(clip);
            }
        }
        return queryClips;
    }

    public List<ClipObject> getClipHistory(int n) {
        List<ClipObject> allClips = getClipHistory();
        List<ClipObject> queryClips = new ArrayList<>();
        n = (n > allClips.size() ? allClips.size() : n);
        for (int i = 0; i < n; i++) {
            queryClips.add(allClips.get(i));
        }
        return queryClips;
    }

    public List<ClipObject> getClipHistory() {
        if (isClipsInMemoryChanged) {
            open();
            String sortOrder = CLIP_DATE + " DESC";
            String[] COLUMNS = {CLIP_STRING, CLIP_DATE, CLIP_IS_STAR};
            Cursor c;
            c = db.query(TABLE_NAME, COLUMNS, null, null, null, null, sortOrder);
            //context = db.query(TABLE_NAME, COLUMNS, CLIP_STRING + " LIKE '%" + sqliteEscape(queryString) + "%'", null, null, null, sortOrder);
            clipsInMemory = new ArrayList<>();
            while (c.moveToNext()) {
                clipsInMemory.add(
                        new ClipObject(
                                c.getString(0),
                                new Date(c.getLong(1)),
                                c.getInt(2) > 0
                        )
                );
            }
            c.close();
            close();
            isClipsInMemoryChanged = false;
        }
        return clipsInMemory;
    }

    public List<ClipObject> getStarredClipHistory() {
        List<ClipObject> allClips = getClipHistory();
        List<ClipObject> starredClips = new ArrayList<>();
        for (ClipObject clipObject : allClips) {
            if (clipObject.isStarred()) {
                starredClips.add(clipObject);
            }
        }
        return starredClips;
    }

    public List<ClipObject> getStarredClipHistory(int n) {
        List<ClipObject> starredClips = getStarredClipHistory();
        if (n > starredClips.size()) {
            n = starredClips.size();
        }
        return starredClips.subList(0, n);
    }

    public List<ClipObject> getStarredClipHistory(String queryString) {
        List<ClipObject> allStarredClips = getStarredClipHistory();
        List<ClipObject> queryClips = new ArrayList<>();
        if ("".equals(queryString) || queryString == null) {
            return allStarredClips;
        }
        for (ClipObject clipObject : allStarredClips) {
            if (clipObject.getText().contains(queryString)) {
                queryClips.add(clipObject);
            }
        }
        return queryClips;
    }

    public void deleteAllClipHistory() {
        //for ActivityMain Clear All
        isClipsInMemoryChanged = true;
        open();
        int row_id = db.delete(
                TABLE_NAME,
                CLIP_IS_STAR + "='" + 0 + "'",
                null
        );
        close();
        if (row_id == -1) {
            Log.e("Storage", "write db error: deleteAllClipHistory.");
        }
        refreshAllTypeOfList(true, null);
    }

    private boolean deleteClipHistoryBefore(float days) {
        //for bindJobScheduler
        isClipsInMemoryChanged = true;
        Date date = new Date();
        long timeStamp = (long) (date.getTime() - days * 86400000);
        open();
        int row_id = db.delete(
                TABLE_NAME,
                CLIP_DATE + "<'" + timeStamp + "'" + " AND " + CLIP_IS_STAR + "='" + 0 + "'",
                null
        );
        close();
        //refreshAllTypeOfList(Storage.MAIN_ACTIVITY_VIEW);
        if (row_id == -1) {
            Log.e("Storage", "write db error: deleteClipHistoryBefore " + days);
            return false;
        }
        return true;
    }

    public boolean cleanUp() {
        SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(context);
        float days = (float) Integer.parseInt(preference.getString(ActivitySetting.PREF_SAVE_DATES, "9999"));
        Log.v(MyUtil.PACKAGE_NAME,
                "Start clean up SQLite at " + new Date().toString() + ", clean clips before " + days + " days");
        return deleteClipHistoryBefore(days);
    }

    private boolean deleteClipHistory(ClipObject clipObject) {
        return deleteClipHistory(clipObject.getText());
    }

    private boolean deleteClipHistory(String query) {
        int row_id = db.delete(TABLE_NAME, CLIP_STRING + "=" + sqliteEscape(query), null);
        if (row_id == -1) {
            Log.e("Storage", "write db error: deleteClipHistory " + query);
            return false;
        }
        return true;
    }

    private ClipObject getClipObjectFromString(String string) {
        List<ClipObject> clipObjects = getClipHistory();
        for (ClipObject clipObject : clipObjects) {
            if (clipObject.getText().equals(string)) {
                return clipObject;
            }
        }
        return null;
    }

    private boolean addClipHistory(String currentString) {
        return addClipHistory(new ClipObject(currentString, new Date()));
    }

    private boolean addClipHistory(ClipObject clipObject) {
        deleteClipHistory(clipObject.getText());
        long timeStamp = clipObject.getDate().getTime();
        ContentValues values = new ContentValues();
        values.put(CLIP_DATE, timeStamp);
        values.put(CLIP_STRING, clipObject.getText());
        values.put(CLIP_IS_STAR, clipObject.isStarred());
        long row_id = db.insert(TABLE_NAME, null, values);
        if (row_id == -1) {
            Log.e("Storage", "write db error: addClipHistory " + clipObject.getText());
            return false;
        }
        return true;
    }

    public boolean isClipObjectStarred(ClipObject clipObject) {
        return isClipObjectStarred(clipObject.getText());
    }

    public boolean isClipObjectStarred(String string) {
        List<ClipObject> allClips = getClipHistory();
        for (ClipObject clipObject : allClips) {
            if (clipObject.getText().equals(string)) {
                return clipObject.isStarred();
            }
        }
        return false;
    }

    public void starredClip(ClipObject clipObject) {
        open();
        deleteClipHistory(clipObject);
        addClipHistory(clipObject);
        close();
        isClipsInMemoryChanged = true;
        refreshAllTypeOfList(false, null);
    }

    public void importClips(ArrayList<ClipObject> clipObjects) {
        open();
        for (ClipObject clipObject : clipObjects) {
            addClipHistory(clipObject);
        }
        close();
        isClipsInMemoryChanged = true;
        refreshAllTypeOfList(true, null);
    }

    public void modifyClip(String oldClip, String newClip) {
        modifyClip(oldClip, newClip, 0);
    }

    public void modifyClip(String oldClip, String newClip, int isImportant) {
        Log.v(MyUtil.PACKAGE_NAME, "modifyClip(" + oldClip + ", " + newClip + ", " + isImportant + ")");
        if (oldClip == null) {
            oldClip = "";
        }
        if (newClip == null) {
            newClip = "";
        }

//        if (newClip.equals(oldClip)) {
//            if (isImportant !=0) {
//                ClipObject oldClipObject = getClipObjectFromString(oldClip);
//                oldClipObject.setStarred((isImportant == 1));
//                starredClip(oldClipObject);
//            }
//            return;
//        }
//        if (newClip.equals(topClipInStack)) {
//            return;
//        }

        boolean isStarred = isClipObjectStarred(oldClip);

        if (isImportant == 1) {
            isStarred = true;
        }
        if (isImportant == -1) {
            isStarred = false;
        }

        open();
        if (!oldClip.isEmpty()) {
            deleteClipHistory(oldClip);
        }
        if (!newClip.isEmpty()) {
            addClipHistory(new ClipObject(
                    newClip,
                    new Date(),
                    isStarred
            ));
        }
        close();
        isClipsInMemoryChanged = true;

        refreshAllTypeOfList(!newClip.isEmpty(), oldClip);

    }

    public boolean updateSystemClipboard() {

        //sync system clipboard and storage.

        String topClipInStack;
        if (getClipHistory().size() > 0) {
            topClipInStack = getClipHistory().get(0).getText();
        } else {
            topClipInStack = "";
        }

        String clipString;
        if (!clipboardManager.hasPrimaryClip()) {
            clipboardManager.setText(topClipInStack);
            return true;
        }
        try {
            //Don't use CharSequence .toString()!
            CharSequence charSequence = clipboardManager.getPrimaryClip().getItemAt(0).getText();
            clipString = String.valueOf(charSequence);
        } catch (Error ignored) {
            clipboardManager.setText(topClipInStack);
            return true;
        }

        if (!topClipInStack.equals(clipString)) {
            clipboardManager.setText(topClipInStack);
            return true;
        }
        return false;
    }

    private void refreshAllTypeOfList(Boolean added, String deletedString) {
//        if (notUpdateWhich == MAIN_ACTIVITY_VIEW) {
//            CBWatcherService.startCBService(context, true);
//        } else if (notUpdateWhich == NOTIFICATION_VIEW) {
//            updateSystemClipboard();
//            ActivityMain.refreshMainView(context, "");
//        } else if (notUpdateWhich == SYSTEM_CLIPBOARD) {
//            ActivityMain.refreshMainView(context, "");
//            CBWatcherService.startCBService(context, true);
//        } else {
//            updateSystemClipboard();
//            CBWatcherService.startCBService(context, true);
//            ActivityMain.refreshMainView(context, "");
//        }
        CBWatcherService.startCBService(context, true);
        updateDbBroadcast(context, added, deletedString);
    }

    public static void updateDbBroadcast(Context context, Boolean added, String deletedString) {
        Intent intent = new Intent(UPDATE_DB);
        if (added) {
            intent.putExtra(UPDATE_DB_ADD, true);
        }
        if (deletedString != null) {
            if (!deletedString.isEmpty()) {
                intent.putExtra(UPDATE_DB_DELETE, deletedString);
            }
        }
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    public class StorageHelper extends SQLiteOpenHelper {
        private static final int DATABASE_VERSION = 3;
        private static final String DATABASE_NAME = "clippingnow.db";
        private static final String TABLE_NAME = "cliphistory";
        private static final String TABLE_CREATE =
                "CREATE TABLE " + TABLE_NAME + " (" +
                        CLIP_DATE + " TIMESTAMP, " +
                        CLIP_STRING + " TEXT, " +
                        CLIP_IS_STAR + " BOOLEAN" +
                        ");";

        public StorageHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(TABLE_CREATE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.v(MyUtil.PACKAGE_NAME, "SQL updated from" + oldVersion + "to" + newVersion);
            if (oldVersion <= 2) {
                // add star option
                db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN " + CLIP_IS_STAR + " BOOLEAN DEFAULT 0");
            }
        }
    }

}
