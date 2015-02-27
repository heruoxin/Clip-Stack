package com.catchingnow.tinyclipboardmanager;

import android.app.IntentService;
import android.content.ClipboardManager;
import android.content.Intent;
import android.os.Handler;
import android.widget.Toast;


/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 */
public class StringActionIntentService extends IntentService {
    public final static int ACTION_COPY  = 1;
    public final static int ACTION_SHARE = 2;
    public final static int ACTION_EDIT  = 3;
    public final static String CLIPBOARD_STRING = "com.catchingnow.tinyclipboardmanager.clipboardString";
    public final static String CLIPBOARD_ACTION = "com.catchingnow.tinyclipboardmanager.clipboarAction";

    public Handler mHandler;
    public StringActionIntentService() {
        super("StringActionIntentService");
    }

    private Intent intent;

    @Override
    public void onCreate() {
        super.onCreate();
        mHandler = new Handler();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            Intent it = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
            sendBroadcast(it);

            this.intent = intent;

            String clips = intent.getStringExtra(CLIPBOARD_STRING);
            int actionCode = intent.getIntExtra(CLIPBOARD_ACTION, 0);
            switch (actionCode) {
                case 0:
                    break;
                case ACTION_COPY:
                    copyText(clips);
                    break;
                case ACTION_SHARE:
                    shareText(clips);
                    break;
                case ACTION_EDIT:
                    editText(clips);
                    break;
            }
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

    protected void copyText(final String clips) {
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
                Toast.makeText(StringActionIntentService.this,
                        getString(R.string.toast_front_string)+toastClips+"\n"+getString(R.string.toast_end_string),
                        Toast.LENGTH_SHORT
                ).show();

            }
        });

    }

    public void editText(final String clips) {
        Intent i = new Intent(this, ActivityEditor.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(CBWatcherService.INTENT_EXTRA_IS_STARRED,
                        intent.getBooleanExtra(CBWatcherService.INTENT_EXTRA_IS_STARRED, false)
                        )
                .putExtra(Intent.EXTRA_TEXT, clips);
        startActivity(i);
    }

}
