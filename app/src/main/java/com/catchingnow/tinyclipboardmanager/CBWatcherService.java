package com.catchingnow.tinyclipboardmanager;

import android.annotation.TargetApi;
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
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;

import java.util.Date;
import java.util.List;

public class CBWatcherService extends Service {

    private final static String PACKAGE_NAME = "com.catchingnow.tinyclipboardmanager";
    public final static String INTENT_EXTRA_FORCE_SHOW_NOTIFICATION = "com.catchingnow.tinyclipboardmanager.EXTRA.FORCE_SHOW_NOTIFICATION";
    public final static String INTENT_EXTRA_MY_ACTIVITY_ON_FOREGROUND_MESSAGE = "com.catchingnow.tinyclipboardmanager.EXTRA.MY_ACTIVITY_ON_FOREGROUND_MESSAGE";
    public final static String INTENT_EXTRA_CLEAN_UP_SQLITE = "com.catchingnow.tinyclipboardmanager.EXTRA.CLEAN_UP_SQLITE";
    public final static String INTENT_EXTRA_CHANGE_STAR_STATUES = "com.catchingnow.tinyclipboardmanager.EXTRA.CHANGE_STAR_STATUES";
    public final static int JOB_ID = 1;
    public int NUMBER_OF_CLIPS = 6; //3-6
    protected boolean showStarred = false;
    private NotificationManager notificationManager;
    private SharedPreferences preference;
    private Storage db;
    private boolean onListened = false;
    private int isMyActivitiesOnForeground = 0;
    private OnPrimaryClipChangedListener listener = new OnPrimaryClipChangedListener() {
        public void onPrimaryClipChanged() {
            performClipboardCheck();
        }
    };

    @Override
    public void onCreate() {
        Log.v(PACKAGE_NAME, "onCreate");
        if (!onListened) {
            preference = PreferenceManager.getDefaultSharedPreferences(this);
            notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            db = Storage.getInstance(this.getBaseContext());
            ((ClipboardManager) getSystemService(CLIPBOARD_SERVICE)).addPrimaryClipChangedListener(listener);
            if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                Log.w(PACKAGE_NAME, "Not support JobScheduler");
            } else {
                bindJobScheduler();
            }
            onListened = true;
        }
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v(PACKAGE_NAME, "onStartCommand");
        if (intent != null) {

            int myActivitiesOnForegroundMessage = intent.getIntExtra(INTENT_EXTRA_MY_ACTIVITY_ON_FOREGROUND_MESSAGE, 0);
            isMyActivitiesOnForeground += myActivitiesOnForegroundMessage;

            if (intent.getBooleanExtra(INTENT_EXTRA_CLEAN_UP_SQLITE, false)) {
                Log.v(PACKAGE_NAME, "onStartCommand cleanUpSqlite");
                cleanUpSqlite();
            }

            if (intent.getBooleanExtra(INTENT_EXTRA_FORCE_SHOW_NOTIFICATION, false)) {
                Log.v(PACKAGE_NAME, "onStartCommand showNotification");
                showNotification();
            }

            if (intent.getBooleanExtra(INTENT_EXTRA_CHANGE_STAR_STATUES, false)) {
                Log.v(PACKAGE_NAME, "onStartCommand changeStarStatues");
                showStarred = !showStarred;
                showNotification();
            }

            if (!preference.getBoolean(ActivitySetting.PREF_START_SERVICE, true)) {
                if (isMyActivitiesOnForeground <= 0) {
                    stopSelf();
                    isMyActivitiesOnForeground = 0;
                    return Service.START_NOT_STICKY;
                }
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
        Log.v(PACKAGE_NAME, "onDes");
        notificationManager.cancel(0);
        ((ClipboardManager) getSystemService(CLIPBOARD_SERVICE)).removePrimaryClipChangedListener(listener);
        onListened = false;
        super.onDestroy();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void bindJobScheduler() {
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
        if (!cb.hasPrimaryClip()) return;
        String clipString;
        try {
            //Don't use CharSequence .toString()!
            CharSequence charSequence = cb.getPrimaryClip().getItemAt(0).getText();
            clipString = String.valueOf(charSequence);
        } catch (Error ignored) {
            return;
        }
        if (clipString.trim().isEmpty()) return;
        if (isMyActivitiesOnForeground <= 0) {
            db.modifyClip(null, clipString, Storage.MAIN_ACTIVITY_VIEW);
        } else {
            db.modifyClip(null, clipString);
        }
    }

    private void cleanUpSqlite() {
        float days = (float) Integer.parseInt(preference.getString(ActivitySetting.PREF_SAVE_DATES, "9999"));
        Log.v(PACKAGE_NAME,
                "Start clean up SQLite at " + new Date().toString() + ", clean clips before " + days + " days");
        if (db == null) {
            db = Storage.getInstance(this.getBaseContext());
        }
        db.deleteClipHistoryBefore(days);
    }

    private boolean checkNotificationPermission() {
        boolean allowNotification = preference.getBoolean(ActivitySetting.PREF_NOTIFICATION_SHOW, true);
        boolean allowService = preference.getBoolean(ActivitySetting.PREF_START_SERVICE, true);
        if (allowNotification && allowService) {
            return true;
        }
        notificationManager.cancel(0);
        return false;
    }

    private void showNotification() {

        if (!checkNotificationPermission()) {
            return;
        }

        if (db == null) {
            db = Storage.getInstance(this.getBaseContext());
        }
        List<ClipObject> thisClips;
        if (showStarred) {
            thisClips = db.getStarredClipHistory(NUMBER_OF_CLIPS);
            if (db.getClipHistory().size() == 0) {
                showSingleNotification();
                return;
            }
            thisClips.add(0, db.getClipHistory().get(0));
        } else {
            thisClips = db.getClipHistory(NUMBER_OF_CLIPS);
        }

        int length = thisClips.size();
        if (length <= 1 && !showStarred) {
            showSingleNotification();
            return;
        }
        if (length <= 1 && showStarred) {
            thisClips.add(new ClipObject(
                    getString(R.string.clip_notification_single_text),
                    new Date(),
                    true
            ));
            length +=1;
        }
        length = (length > (NUMBER_OF_CLIPS + 1)) ? (NUMBER_OF_CLIPS + 1) : length;

        Intent resultIntent = new Intent(this, ActivityMain.class)
                .putExtra(ActivityMain.EXTRA_IS_FROM_NOTIFICATION, true);
        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(
                        this,
                        0,
                        resultIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );

        boolean pinOnTop = preference.getBoolean(ActivitySetting.PREF_NOTIFICATION_PIN, false);

        NotificationCompat.Builder preBuildNotification = new NotificationCompat.Builder(this)
                .setContentTitle(getString(R.string.clip_notification_title) + thisClips.get(0).getText().trim()) //title
                .setContentIntent(resultPendingIntent)
                .setOngoing(pinOnTop)
                .setAutoCancel(!pinOnTop);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            String description = getString(R.string.clip_notification_text);
            if (showStarred) {
                description = "★ " + description;
            }
            preBuildNotification
                    .setContentText(description)
                    .setPriority(NotificationCompat.PRIORITY_MIN);
        } else {
            preBuildNotification
                    .setContentText(getString(R.string.clip_notification_text_old));
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            preBuildNotification
                    .setSmallIcon(R.drawable.icon)
                    .setVisibility(NotificationCompat.VISIBILITY_SECRET)
                    .setColor(getResources().getColor(R.color.primary_light));
        } else {
            preBuildNotification.setSmallIcon(R.drawable.icon_shadow);
        }

        NotificationClipListAdapter bigView = new NotificationClipListAdapter(this.getBaseContext(), thisClips.get(0));

        for (int i = 1; i < length; i++) {
            bigView.addClips(thisClips.get(i));
        }

        Notification n = preBuildNotification.build();

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
            n.bigContentView = bigView.build();
        }

        notificationManager.cancel(0);
        notificationManager.notify(0, n);
    }

    private void showSingleNotification() {

        if (!checkNotificationPermission()) {
            return;
        }

        String currentClip = "Clipboard is empty.";
        ClipboardManager cb = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (cb.hasPrimaryClip()) {
            ClipData cd = cb.getPrimaryClip();
            if (cd.getDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)) {
                CharSequence thisClip = cd.getItemAt(0).getText();
                if (thisClip != null) {
                    currentClip = thisClip.toString().trim();
                }
            }
        }

        boolean pinOnTop = preference.getBoolean(ActivitySetting.PREF_NOTIFICATION_PIN, false);

           NotificationCompat.Builder preBuildN = new NotificationCompat.Builder(this)
                .setContentTitle(getString(R.string.clip_notification_title) + currentClip)
                .setOngoing(pinOnTop)
                .setAutoCancel(!pinOnTop);
        if (showStarred) {
            Intent resultIntent = new Intent(this, ActivityEditor.class)
                    .putExtra(ClipObjectActionBridge.STATUE_IS_STARRED, true)
                    .putExtra(ActivityMain.EXTRA_IS_FROM_NOTIFICATION, true);
            PendingIntent resultPendingIntent =
                    PendingIntent.getActivity(
                            this,
                            0,
                            resultIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT
                    );

            preBuildN
                    .setContentIntent(resultPendingIntent)
                    .setContentText(getString(R.string.clip_notification_starred_single_text));
        } else {
            Intent resultIntent = new Intent(this, ActivityMain.class)
                    .putExtra(ActivityMain.EXTRA_IS_FROM_NOTIFICATION, true);
            PendingIntent resultPendingIntent =
                    PendingIntent.getActivity(
                            this,
                            0,
                            resultIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT
                    );

            preBuildN
                    .setContentIntent(resultPendingIntent)
                    .setContentText(getString(R.string.clip_notification_single_text));
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            preBuildN
                    .setPriority(NotificationCompat.PRIORITY_MIN);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            preBuildN
                    .setSmallIcon(R.drawable.icon)
                    .setVisibility(NotificationCompat.VISIBILITY_SECRET)
                    .setColor(getResources().getColor(R.color.primary_light));
        } else {
            preBuildN.setSmallIcon(R.drawable.icon_shadow);
        }
        Notification n = preBuildN.build();

        notificationManager.cancel(0);
        notificationManager.notify(0, n);
    }

    public static void startCBService(Context context, boolean forceShowNotification) {
        startCBService(context, forceShowNotification, 0, false);
    }

    public static void startCBService(Context context, boolean forceShowNotification, boolean doCleanUp) {
        startCBService(context, forceShowNotification, 0, doCleanUp);
    }

    public static void startCBService(Context context, boolean forceShowNotification, int myActivitiesOnForegroundMessage) {
        startCBService(context, forceShowNotification, myActivitiesOnForegroundMessage, false);
    }

    public static void startCBService(Context context, boolean forceShowNotification, int myActivitiesOnForegroundMessage, boolean doCleanUp) {
        Intent intent = new Intent(context, CBWatcherService.class)
                .putExtra(INTENT_EXTRA_FORCE_SHOW_NOTIFICATION, forceShowNotification)
                .putExtra(INTENT_EXTRA_MY_ACTIVITY_ON_FOREGROUND_MESSAGE, myActivitiesOnForegroundMessage)
                .putExtra(INTENT_EXTRA_CLEAN_UP_SQLITE, doCleanUp);
        context.startService(intent);
    }

    private class NotificationClipListAdapter {

        private int buttonNumber = 0;

        private RemoteViews expandedView;
        private Context c;
        int id = 0;

        public NotificationClipListAdapter(Context context, ClipObject clipObject) {
            c = context;
            String currentClip = clipObject.getText().trim();
            expandedView = new RemoteViews(c.getPackageName(), R.layout.notification_clip_list);
            expandedView.setTextViewText(R.id.current_clip, currentClip);
            //add pIntent for share
            Intent openShareIntent = new Intent(c, ClipObjectActionBridge.class);
            openShareIntent.putExtra(ClipObjectActionBridge.CLIPBOARD_STRING, currentClip);
            openShareIntent.putExtra(ClipObjectActionBridge.CLIPBOARD_ACTION, ClipObjectActionBridge.ACTION_SHARE);
            PendingIntent pOpenShareIntent = PendingIntent.getService(c,
                    buttonNumber++,
                    openShareIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            expandedView.setOnClickPendingIntent(R.id.clip_share_button, pOpenShareIntent);
            //add pIntent for edit
            Intent openEditIntent = new Intent(c, ClipObjectActionBridge.class)
                    .putExtra(ClipObjectActionBridge.CLIPBOARD_STRING, currentClip)
                    .putExtra(ClipObjectActionBridge.STATUE_IS_STARRED, clipObject.isStarred())
                    .putExtra(ClipObjectActionBridge.CLIPBOARD_ACTION, ClipObjectActionBridge.ACTION_EDIT);
            PendingIntent pOpenEditIntent = PendingIntent.getService(
                    c,
                    buttonNumber++,
                    openEditIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            expandedView.setOnClickPendingIntent(R.id.current_clip, pOpenEditIntent);
            //add pIntent for star click
            Intent openStarIntent = new Intent(c, CBWatcherService.class)
                    .putExtra(INTENT_EXTRA_CHANGE_STAR_STATUES, true);
            PendingIntent pOpenStarIntent = PendingIntent.getService(
                    c,
                    buttonNumber++,
                    openStarIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            expandedView.setOnClickPendingIntent(R.id.star, pOpenStarIntent);
            //set star's icon
            if (showStarred) {
                expandedView.setImageViewResource(R.id.star, R.drawable.ic_action_star_grey600);
            } else {
                expandedView.setImageViewResource(R.id.star, R.drawable.ic_action_star_outline_grey600);
            }
        }

        public NotificationClipListAdapter addClips(ClipObject clipObject) {
            id += 1;
            String s = clipObject.getText().trim();
            //Log.v(PACKAGE_NAME,"ID "+id);
            //Log.v(PACKAGE_NAME,s);
            //add view
            RemoteViews theClipView = new RemoteViews(c.getPackageName(), R.layout.notification_clip_card_view);
            if (clipObject.isStarred()) {
                theClipView.setTextViewText(R.id.clip_text, "★ "+s);
            } else {
                theClipView.setTextViewText(R.id.clip_text, s);
            }

            //add pIntent for edit

            Intent openEditIntent = new Intent(c, ClipObjectActionBridge.class)
                    .putExtra(ClipObjectActionBridge.CLIPBOARD_STRING, s)
                    .putExtra(ClipObjectActionBridge.STATUE_IS_STARRED, clipObject.isStarred())
                    .putExtra(ClipObjectActionBridge.CLIPBOARD_ACTION, ClipObjectActionBridge.ACTION_EDIT);
            PendingIntent pOpenEditIntent = PendingIntent.getService(
                    c,
                    buttonNumber++,
                    openEditIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            theClipView.setOnClickPendingIntent(R.id.clip_text, pOpenEditIntent);

            if (clipObject.getText().equals(getString(R.string.clip_notification_single_text)) && clipObject.isStarred()) {
                //hide copy button for 'add'

                //theClipView.setViewVisibility(R.id.clip_copy_button, View.GONE);
                theClipView.setImageViewResource(R.id.clip_copy_button, R.drawable.transparent);
            } else {
                //add pIntent for copy

                Intent openCopyIntent = new Intent(c, ClipObjectActionBridge.class)
                        .putExtra(ClipObjectActionBridge.CLIPBOARD_STRING, s)
                        .putExtra(ClipObjectActionBridge.STATUE_IS_STARRED, true)
                        .putExtra(ClipObjectActionBridge.CLIPBOARD_ACTION, ClipObjectActionBridge.ACTION_COPY);
                PendingIntent pOpenCopyIntent = PendingIntent.getService(c,
                        buttonNumber++,
                        openCopyIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT);
                theClipView.setOnClickPendingIntent(R.id.clip_copy_button, pOpenCopyIntent);
            }

            expandedView.addView(R.id.main_view, theClipView);
            return this;
        }

        public RemoteViews build() {
            //expandedView.setTextViewText(R.id.text, "Hello World!");
            return expandedView;
        }
    }
}
