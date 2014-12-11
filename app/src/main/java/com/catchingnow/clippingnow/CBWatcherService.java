package com.catchingnow.clippingnow;

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

    public final static String CLIPBOARD_STRING = "com.catchingnow.clippingnow.clipboardString";
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
        ((ClipboardManager) getSystemService(CLIPBOARD_SERVICE)).addPrimaryClipChangedListener(listener);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
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
               // .setStyle(new Notification.InboxStyle()
               //         .addLine("1. "+thisClips.get(length - 0))
               //         .addLine("2. "+thisClips.get(length - 1))
               //         .addLine("3. "+thisClips.get(length - 2))
               //         //.setSummaryText("more")
               //         )
                .setPriority(Notification.PRIORITY_MIN)
                .setAutoCancel(true);
                //.addAction(R.drawable.ic_drawer, getString(R.string.clip_notification_button_one), pIntent)
                //.addAction(R.drawable.ic_drawer, getString(R.string.clip_notification_button_two), pIntent)
                //.addAction(R.drawable.ic_drawer, getString(R.string.clip_notification_button_three), pIntent)

        Notification.InboxStyle notificationStyle = new Notification.InboxStyle()
                .setBigContentTitle(getString(R.string.clip_notification_big_title))
                .setSummaryText(getString(R.string.clip_notification_big_summary_text) + thisClips.get(0).trim());

        //Intent openMainIntent = new Intent(this, MainActivity.class);
        //PendingIntent pOpenMainIntent = PendingIntent.getActivity(this, 0, openMainIntent, 0);

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
        //        .setContentIntent(pOpenMainIntent)
                .setStyle(notificationStyle)
                .build();

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        notificationManager.cancelAll();
        notificationManager.notify(0, n);
    }

}