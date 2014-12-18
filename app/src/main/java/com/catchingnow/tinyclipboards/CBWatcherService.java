package com.catchingnow.tinyclipboards;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.ClipboardManager.OnPrimaryClipChangedListener;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class CBWatcherService extends Service {

    public final static String CLIPBOARD_STRING = "com.catchingnow.tinyclipboards.clipboardString";
    private int buttonNumber = 0;
    private List<String> clips = new ArrayList<String>();
    private final String tag = "[[ClipboardWatcherService]] ";
    private OnPrimaryClipChangedListener listener = new OnPrimaryClipChangedListener() {
        public void onPrimaryClipChanged() {
            performClipboardCheck();
        }
    };


    @Override
    public void onCreate() {
        Log.v(CLIPBOARD_STRING, "onCreate");
        ((ClipboardManager) getSystemService(CLIPBOARD_SERVICE)).addPrimaryClipChangedListener(listener);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v(CLIPBOARD_STRING, "onStartCommand");
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.v(CLIPBOARD_STRING, "onBind");
        return null;
    }

    private void performClipboardCheck() {
        ClipboardManager cb = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (cb.hasPrimaryClip()) {
            ClipData cd = cb.getPrimaryClip();
            if (cd.getDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)) {
                String thisClip = cd.getItemAt(0).getText().toString();
                addClip(thisClip);
                showNotification();
            }
        }
    }

    public List<String> getClips () {
        return clips;
    }
    public void addClip(String s) {
        if (s == null) return;
        for (int i=0; i<clips.size(); i++) {
            if (s.equals(clips.get(i))) {
                clips.remove(i);
            }
        }
        clips.add(0, s);
    }

    public void showNotification() {

        List<String> thisClips = getClips();
        int length = thisClips.size();
        if (length <= 1) {
            return;
        }
        length = (length > 4) ? 4 : length;

        Notification.Builder preBuildNotification  = new Notification.Builder(this)
                .setContentTitle(getString(R.string.clip_notification_title)+thisClips.get(0)) //title
                .setContentText(getString(R.string.clip_notification_text))
                .setSmallIcon(R.drawable.ic_action_copy_black)
                .setPriority(Notification.PRIORITY_MIN)
                .setAutoCancel(true);

        Notification.InboxStyle notificationStyle = new Notification.InboxStyle()
                .setBigContentTitle(getString(R.string.clip_notification_big_title))
                .setSummaryText(getString(R.string.clip_notification_big_summary_text) + thisClips.get(0).trim());


        for (int i=1; i<length; i++) {
            notificationStyle.addLine(i+". "+thisClips.get(i).trim());

            Intent openCopyIntent = new Intent(this, copyToClipboardIntentService.class);
            openCopyIntent.putExtra(CLIPBOARD_STRING, thisClips.get(i));
            PendingIntent pOpenCopyIntent = PendingIntent.getService(this, buttonNumber++, openCopyIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            preBuildNotification.addAction(
                    R.drawable.ic_action_copy,
                    getString(R.string.clip_notification_button) + " " + i,
                    pOpenCopyIntent);
        }

        Notification n = preBuildNotification
        //        .setStyle(notificationStyle)
                .build();

        ClipListViewCreator bigView = new ClipListViewCreator(this.getBaseContext(), "hhh");
        n.bigContentView = bigView.build();

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        notificationManager.cancelAll();
        notificationManager.notify(0, n);
    }

}