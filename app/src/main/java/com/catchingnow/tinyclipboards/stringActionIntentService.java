package com.catchingnow.tinyclipboards;

import android.app.IntentService;
import android.content.ClipboardManager;
import android.content.Intent;
import android.os.Handler;
import android.widget.Toast;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 */
public class stringActionIntentService extends IntentService {
    public Handler mHandler;

    public stringActionIntentService() {
        super("stringActionIntentService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mHandler = new Handler();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            String clips = intent.getStringExtra(ClipListViewCreator.CLIPBOARD_STRING);
            int actionCode = intent.getIntExtra(ClipListViewCreator.CLIPBOARD_ACTION, 0);
            switch (actionCode) {
                case 0:
                    break;
                case 1:
                    copyText(clips);
                    break;
                case 2:
                    shareText(clips);
            }
        }
        Intent i = new Intent(this, CBWatcherService.class);
        this.startService(i);
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
                //make toast
                Toast.makeText(stringActionIntentService.this,
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
