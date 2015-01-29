package com.catchingnow.tinyclipboards;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.ClipboardManager.OnPrimaryClipChangedListener;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class CBWatcherService extends Service {

    private final static String PACKAGE_NAME = "com.catchingnow.tinyclipboards";
    public final static String INTENT_EXTRA_FORCE_SHOW_NOTIFICATION = "com.catchingnow.tinyclipboards.EXTRA.FORCE_SHOW_NOTIFICATION";
    public final static int JOB_ID = 1;
    public int NUMBER_OF_CLIPS = 6; //3-9
    private NotificationManager notificationManager;
    private Storage db;
    private OnPrimaryClipChangedListener listener = new OnPrimaryClipChangedListener() {
        public void onPrimaryClipChanged() {
            performClipboardCheck();
        }
    };


    @Override
    public void onCreate() {
        Log.v(PACKAGE_NAME, "onCreate");
        SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(this);
        if (!preference.getBoolean(ActivitySetting.SERVICE_STATUS, true)) {
            Log.v(PACKAGE_NAME, "pref said cannot start service!");
            return;
        }
        db = new Storage(this.getBaseContext());
        ((ClipboardManager) getSystemService(CLIPBOARD_SERVICE)).addPrimaryClipChangedListener(listener);
        bindJobScheduler();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v(PACKAGE_NAME, "onStartCommand");
        if (intent != null) {
            if (intent.getBooleanExtra(INTENT_EXTRA_FORCE_SHOW_NOTIFICATION, false)) {
                showNotification();
            }
        }
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.v(PACKAGE_NAME, "onBind");
        return null;
    }

    @Override
    public void onDestroy() {
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancel(0);
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

    public List<ClipObject> getClips() {
        return db.getClipHistory(NUMBER_OF_CLIPS);
    }

    public boolean addClip(String s) {
        if (s == null) return false;
        return db.addClipHistory(s);
    }

    public void showNotification() {

        List<String> thisClipText = new ArrayList<String>();
        List<ClipObject> thisClips = getClips();
        for (ClipObject thisClip : thisClips) {
            thisClipText.add(thisClip.text);
        }
        int length = thisClipText.size();
        if (length <= 1) {
            return;
        }
        length = (length > (NUMBER_OF_CLIPS + 1)) ? (NUMBER_OF_CLIPS + 1) : length;

        Intent resultIntent = new Intent(this, ActivityMain.class);
        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(
                        this,
                        0,
                        resultIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );

        SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(this);
        boolean pinOnTop = preference.getBoolean(ActivitySetting.NOTIFICATION_ALWAYS_SHOW, false);

        Notification.Builder preBuildNotification = new Notification.Builder(this)
                .setContentTitle(getString(R.string.clip_notification_title) + thisClipText.get(0)) //title
                .setContentText(getString(R.string.clip_notification_text))
                .setSmallIcon(R.drawable.ic_action_copy_black)
                .setPriority(Notification.PRIORITY_MIN)
                .setContentIntent(resultPendingIntent)
                .setOngoing(pinOnTop)
                .setAutoCancel(!pinOnTop);

        NotificationClipListViewCreator bigView = new NotificationClipListViewCreator(this.getBaseContext(), thisClipText.get(0));

        for (int i = 1; i < length; i++) {
            bigView.addClips(thisClipText.get(i));
        }

        Notification n = preBuildNotification.build();

        n.bigContentView = bigView.build();

        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancel(0);
        notificationManager.notify(0, n);
    }


    public static void toggleService(Context c, boolean startOrStop) {
        if (startOrStop) {
            c.startService(new Intent(c, CBWatcherService.class));
        } else {
            c.stopService(new Intent(c, CBWatcherService.class));
        }
    }

    public static void forceShowNotification(Context context) {
        Intent intent = new Intent(context, CBWatcherService.class);
        intent.putExtra(CBWatcherService.INTENT_EXTRA_FORCE_SHOW_NOTIFICATION, true);
        context.startService(intent);
    }

}