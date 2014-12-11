package com.catchingnow.clippingnow;

import android.app.IntentService;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 */
public class copyToClipboardIntentService extends IntentService {
    public Handler mHandler;
    private String clips;

    public copyToClipboardIntentService() {
        super("copyToClipboardIntentService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mHandler = new Handler();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            clips = intent.getStringExtra(CBWatcherService.CLIPBOARD_STRING);
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    //make toast
                    Toast.makeText(copyToClipboardIntentService.this,
                            getString(R.string.toast_front_string)+clips+getString(R.string.toast_end_string),
                            Toast.LENGTH_LONG
                    ).show();

                    //copy clips to clipboard
                    ClipboardManager cb = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                    cb.setText(clips);
                }
            });

        }
    }

}
