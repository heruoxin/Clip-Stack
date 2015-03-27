package com.catchingnow.tinyclipboardmanager;

import android.app.IntentService;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;


/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 */
public class ClipObjectActionBridge extends IntentService {
    public final static int ACTION_COPY = 1;
    public final static int ACTION_SHARE = 2;
    public final static int ACTION_EDIT = 3;
    public final static int ACTION_STAR = 4;
    public final static int ACTION_OPEN_MAIN_DIALOG = 5;
    public final static int ACTION_REFRESH_WIDGET = 6;
    public final static int ACTION_CHANGE_WIDGET_STAR = 7;
    public final static String ACTION_CODE = "com.catchingnow.tinyclipboardmanager.actionCode";
    public final static String STATUE_IS_STARRED  = "com.catchingnow.tinyclipboardmanager.isStarred";


    public Handler mHandler;
    public ClipObjectActionBridge() {
        super("ClipObjectActionBridge");
    }

    private Intent intent;

    @Override
    public void onCreate() {
        super.onCreate();
        mHandler = new Handler();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Intent it = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        sendBroadcast(it);

        this.intent = intent;

        if (intent == null) return;

        String clips = intent.getStringExtra(Intent.EXTRA_TEXT);
        int actionCode = intent.getIntExtra(ACTION_CODE, 0);
        Log.v(MyUtil.PACKAGE_NAME, "ACTION_CODE: " + actionCode);
        switch (actionCode) {
            case 0:
                break;
            case ACTION_COPY:
                copyText(clips);
                return;
            case ACTION_SHARE:
                shareText(clips);
                return;
            case ACTION_EDIT:
                editText(clips);
                return;
            case ACTION_STAR:
                starText(clips);
                return;
            case ACTION_OPEN_MAIN_DIALOG:
                openMainDialogActivity();
                return;
            case ACTION_REFRESH_WIDGET:
                AppWidget.updateAllAppWidget(this);
                return;
            case ACTION_CHANGE_WIDGET_STAR:
                changeWidgetStarredStatus();
                return;
        }
    }

    private void shareText(final String clips) {
        Intent i = new Intent();
        i.setAction(Intent.ACTION_SEND);
        i.putExtra(Intent.EXTRA_TEXT, clips);
        i.setType("text/plain");
        Intent sendIntent = Intent.createChooser(i, getString(R.string.share_clipboard_to));
        sendIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(sendIntent);
    }

    private void copyText(final String clips) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                //copy clips to clipboard
                ClipboardManager cb = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                cb.setText(clips);

                //make toast
                String toastClips =clips;
                if ( clips.length() > 15) {
                    toastClips = clips.substring(0, 15) + "…";
                }
                Toast.makeText(ClipObjectActionBridge.this,
                        getString(R.string.toast_copied, toastClips+"\n"),
                        Toast.LENGTH_SHORT
                ).show();

            }
        });

    }

    private void editText(final String clips) {
        Intent i = new Intent(this, ActivityEditor.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(STATUE_IS_STARRED,
                        intent.getBooleanExtra(STATUE_IS_STARRED, false)
                        )
                .putExtra(Intent.EXTRA_TEXT, clips);
        startActivity(i);
    }

    private void starText(final String clips) {
        Storage db = Storage.getInstance(this);
        db.changeClipStarStatus(clips);
    }

    private void openMainDialogActivity() {
        //open by this will be auto closed when copy.
        Intent i = new Intent(this, ActivityMainDialog.class)
                .putExtra(ActivityMain.EXTRA_IS_FROM_NOTIFICATION, true)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                .addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        startActivity(i);
    }

    private void changeWidgetStarredStatus() {
        SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isStarred = preference.getBoolean(AppWidget.WIDGET_IS_STARRED, false);
        preference.edit()
                .putBoolean(AppWidget.WIDGET_IS_STARRED, !isStarred)
                .apply();

        AppWidget.updateAllAppWidget(this);
    }

}
