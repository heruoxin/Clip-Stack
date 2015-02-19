package com.catchingnow.tinyclipboardmanager;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by heruoxin on 14/12/9.
 */
public class Storage {
    public final static String PACKAGE_NAME = "com.catchingnow.tinyclipboardmanager";
    public final static int NOTIFICATION_VIEW = 1;
    public final static int MAIN_ACTIVITY_VIEW = 2;
    public final static int SYSTEM_CLIPBOARD = 4;
    private static final String TABLE_NAME = "clipHistory";
    private static final String CLIP_STRING = "history";
    private static final String CLIP_DATE = "date";
    private static Storage mInstance = null;
    private StorageHelper dbHelper;
    private SQLiteDatabase db;
    private Context context;
    private ClipboardManager cb;
    private List<ClipObject> clipsInMemory;
    private boolean isClipsInMemoryChanged = true;
    private String topClipInStack = "";

    private Storage(Context context) {
        this.context = context;
        this.cb = (ClipboardManager) this.context.getSystemService(Context.CLIPBOARD_SERVICE);
        this.dbHelper = new StorageHelper(this.context);
    }

    public static Storage getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new Storage(context.getApplicationContext());
        }
        return mInstance;
    }

    private String sqliteEscape(String keyWord) {
        if ("".equals(keyWord) || keyWord == null) {
            return keyWord;
        }
        return keyWord
                .replace("'", "''")
//                .replace("/", "//")
//                .replace("[", "/[")
//                .replace("]", "/]")
//                .replace("%", "/%")
//                .replace("&", "/&")
//                .replace("_", "/_")
//                .replace("(", "/(")
//                .replace(")", "/)")
                ;
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
        if ("".equals(queryString)) {
            return allClips;
        }
        if (queryString == null) {
            return allClips;
        }
        List<ClipObject> queryClips = new ArrayList<>();
        for (ClipObject clip:allClips) {
            if(clip.getText().contains(queryString)) {
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
            String[] COLUMNS = {CLIP_STRING, CLIP_DATE};
            Cursor c;
            c = db.query(TABLE_NAME, COLUMNS, null, null, null, null, sortOrder);
            //context = db.query(TABLE_NAME, COLUMNS, CLIP_STRING + " LIKE '%" + sqliteEscape(queryString) + "%'", null, null, null, sortOrder);
            clipsInMemory = new ArrayList<>();
            while (c.moveToNext()) {
                clipsInMemory.add(new ClipObject(c.getString(0), new Date(c.getLong(1))));
            }
            c.close();
            close();
            isClipsInMemoryChanged = false;
        }
        return clipsInMemory;
    }

    private void refreshTopClipInStack() {
        if (getClipHistory().size() > 0) {
            topClipInStack = getClipHistory().get(0).getText();
        } else {
            topClipInStack = "";
        }
    }

    public void deleteAllClipHistory() {
        //for ActivityMain Clear All
        isClipsInMemoryChanged = true;
        open();
        int row_id = db.delete(TABLE_NAME, CLIP_DATE + ">'" + 0 + "'", null);
        close();
        if (row_id == -1) {
            Log.e("Storage", "write db error: deleteAllClipHistory.");
        }
        refreshAllTypeOfList(0);
        refreshTopClipInStack();
    }

    public boolean deleteClipHistoryBefore(float days) {
        //for bindJobScheduler
        isClipsInMemoryChanged = true;
        Date date = new Date();
        long timeStamp = (long) (date.getTime() - days * 86400000);
        open();
        int row_id = db.delete(TABLE_NAME, CLIP_DATE + "<'" + timeStamp + "'", null);
        close();
        //refreshAllTypeOfList(Storage.MAIN_ACTIVITY_VIEW);
        refreshTopClipInStack();
        if (row_id == -1) {
            Log.e("Storage", "write db error: deleteClipHistoryBefore " + days);
            return false;
        }
        return true;
    }

    private boolean deleteClipHistory(String query) {
        int row_id = db.delete(TABLE_NAME, CLIP_STRING + "='" + sqliteEscape(query) + "'", null);
        if (row_id == -1) {
            Log.e("Storage", "write db error: deleteClipHistory " + query);
            return false;
        }
        return true;
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
        long row_id = db.insert(TABLE_NAME, null, values);
        if (row_id == -1) {
            Log.e("Storage", "write db error: addClipHistory " + clipObject.getText());
            return false;
        }
        return true;
    }

    public void importClips(ArrayList<ClipObject> clipObjects) {
        open();
        for (ClipObject clipObject: clipObjects) {
            addClipHistory(clipObject);
        }
        close();
        isClipsInMemoryChanged = true;
        refreshAllTypeOfList(0);
    }

    public void modifyClip(String oldClip, String newClip) {
        modifyClip(oldClip, newClip, 0);
    }

    public void modifyClip(String oldClip, String newClip, int notUpdateWhich) {
        Log.v(PACKAGE_NAME, "modifyClip("+oldClip+", "+newClip+", "+notUpdateWhich+")");
        isClipsInMemoryChanged = true;
        if (oldClip == null) {
            oldClip = "";
        }
        if (newClip == null) {
            newClip = "";
        }
        if (newClip.equals(oldClip)) {
            return;
        }
        if (newClip.equals(topClipInStack)) {
            Log.v(PACKAGE_NAME,"Equals to TopStack");
            return;
        }
        open();
        if (!newClip.equals("")) {
            addClipHistory(newClip);
        }
        if (!oldClip.equals("")) {
            deleteClipHistory(oldClip);
        }
        close();

        refreshTopClipInStack();
        refreshAllTypeOfList(notUpdateWhich);

    }

    private void updateSystemClipboard(boolean alsoStartService) {

        //sync system clipboard and storage.
        if (cb.hasPrimaryClip()) {
            ClipData cd = cb.getPrimaryClip();
            if (cd.getDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)) {
                CharSequence thisClip = cd.getItemAt(0).getText();
                if (thisClip != null) {
                    if (!thisClip.toString().equals(topClipInStack)) {
                        cb.setText(topClipInStack);
                        return;
                    }
                }
            }
        }
        if (alsoStartService) {
            Log.v(PACKAGE_NAME, "Storage updateSystemClipboard");
            CBWatcherService.startCBService(context, true);
        }
    }

    private void refreshAllTypeOfList(int notUpdateWhich) {
        if (notUpdateWhich == MAIN_ACTIVITY_VIEW) {
            updateSystemClipboard(true);
            //CBWatcherService.startCBService(context, true);
        } else if (notUpdateWhich == NOTIFICATION_VIEW) {
            updateSystemClipboard(false);
            ActivityMain.refreshMainView(context, "");
        } else if (notUpdateWhich == SYSTEM_CLIPBOARD) {
            ActivityMain.refreshMainView(context, "");
            Log.v(PACKAGE_NAME, "Storage refreshAllTypeOfList");
            CBWatcherService.startCBService(context, true);
        } else {
            updateSystemClipboard(true);
            //CBWatcherService.startCBService(context, true);
            ActivityMain.refreshMainView(context, "");
        }
    }


    public class StorageHelper extends SQLiteOpenHelper {
        private final static String PACKAGE_NAME = "com.catchingnow.tinyclipboardmanager";
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
            Log.v(PACKAGE_NAME, "SQL updated from" + oldVersion + "to" + newVersion);
        }
    }


//    public void printClips(int n) {
//        for (int i=0; i<n; i++){
//            String s = getClipHistory(n);
//            Log.v("printClips", s);
//        }
//    }

}
