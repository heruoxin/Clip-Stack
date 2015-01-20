package com.catchingnow.tinyclipboards;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.ClipboardManager.OnPrimaryClipChangedListener;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class CBWatcherService extends Service {

    private final static String PACKAGE_NAME = "com.catchingnow.tinyclipboards";
    public final static int JOB_ID = 1;
    public int NUMBER_OF_CLIPS = 5;
    private Storage db;
    private OnPrimaryClipChangedListener listener = new OnPrimaryClipChangedListener() {
        public void onPrimaryClipChanged() {
            performClipboardCheck();
        }
    };


    @Override
    public void onCreate() {
        Log.v(PACKAGE_NAME, "onCreate");
        db = new Storage(this.getBaseContext());
        ((ClipboardManager) getSystemService(CLIPBOARD_SERVICE)).addPrimaryClipChangedListener(listener);
        bindJobScheduler();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v(PACKAGE_NAME, "onStartCommand");
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.v(PACKAGE_NAME, "onBind");
        return null;
    }

    public void bindJobScheduler() {
        // JobScheduler for auto clean sqlite
        JobInfo job = new JobInfo.Builder(JOB_ID, new ComponentName(this, SyncJobService.class))
                .setRequiresCharging(true)
                .setPeriodic(480000)
                .setPersisted(true)
                .build();
        JobScheduler jobScheduler = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
        jobScheduler.schedule(job);
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
        return db.getClipHistory(NUMBER_OF_CLIPS);
    }
    public boolean addClip(String s) {
        if (s == null) return false;
        return db.addClipHistory(s);
    }

    public void showNotification() {

        List<String> thisClips = getClips();
        int length = thisClips.size();
        if (length <= 1) {
            return;
        }
        length = (length > (NUMBER_OF_CLIPS + 1)) ? (NUMBER_OF_CLIPS + 1) : length;

        Notification.Builder preBuildNotification  = new Notification.Builder(this)
                .setContentTitle(getString(R.string.clip_notification_title)+thisClips.get(0)) //title
                .setContentText(getString(R.string.clip_notification_text))
                .setSmallIcon(R.drawable.ic_action_copy_black)
                .setPriority(Notification.PRIORITY_MIN)
                .setAutoCancel(true);

        ClipListViewCreator bigView = new ClipListViewCreator(this.getBaseContext(), thisClips.get(0));

        for (int i=1; i<length; i++) {
            bigView.addClips(thisClips.get(i));
        }

        Notification n = preBuildNotification.build();

        n.bigContentView = bigView.build();

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        notificationManager.cancelAll();
        notificationManager.notify(0, n);
    }

}