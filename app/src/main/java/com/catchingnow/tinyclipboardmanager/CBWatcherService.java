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
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import java.util.Date;
import java.util.List;

public class CBWatcherService extends Service {

    public final static String INTENT_EXTRA_FORCE_SHOW_NOTIFICATION = "com.catchingnow.tinyclipboardmanager.EXTRA.FORCE_SHOW_NOTIFICATION";
    public final static String INTENT_EXTRA_MY_ACTIVITY_ON_FOREGROUND_MESSAGE = "com.catchingnow.tinyclipboardmanager.EXTRA.MY_ACTIVITY_ON_FOREGROUND_MESSAGE";
    public final static String INTENT_EXTRA_CHANGE_STAR_STATUES = "com.catchingnow.tinyclipboardmanager.EXTRA.CHANGE_STAR_STATUES";
    public final static int JOB_ID = 1;
    public int NUMBER_OF_CLIPS = 5; //3-6
    protected boolean isStarred = false;
    private NotificationManagerCompat notificationManager;
    private ClipboardManager clipboardManager;
    private SharedPreferences preference;
    private Storage db;
    private boolean onListened = false;
    private boolean pinOnTop = false;
    private int notificationPriority = 0;
    private int isMyActivitiesOnForeground = 0;
    private int pIntentId = -1;
    private OnPrimaryClipChangedListener listener = new OnPrimaryClipChangedListener() {
        public void onPrimaryClipChanged() {
            performClipboardCheck();
        }
    };

    @Override
    public void onCreate() {
        Log.v(MyUtil.PACKAGE_NAME, "onCreate");
        if (!onListened) {
            preference = PreferenceManager.getDefaultSharedPreferences(this);
            notificationManager = NotificationManagerCompat.from(this);
            db = Storage.getInstance(this.getBaseContext());
            clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            clipboardManager.addPrimaryClipChangedListener(listener);
            if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                Log.w(MyUtil.PACKAGE_NAME, "Not support JobScheduler");
            } else {
                bindJobScheduler();
            }
            onListened = true;
        }
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {

            int myActivitiesOnForegroundMessage = intent.getIntExtra(INTENT_EXTRA_MY_ACTIVITY_ON_FOREGROUND_MESSAGE, 0);
            isMyActivitiesOnForeground += myActivitiesOnForegroundMessage;
            notificationPriority = Integer.parseInt(preference.getString(ActivitySetting.PREF_NOTIFICATION_PRIORITY, "0"));
            pinOnTop = preference.getBoolean(ActivitySetting.PREF_NOTIFICATION_PIN, false);

            if (intent.getBooleanExtra(INTENT_EXTRA_FORCE_SHOW_NOTIFICATION, false)) {
                Log.v(MyUtil.PACKAGE_NAME, "onStartCommand showNotification");
                showNotification();
            }

            if (intent.getBooleanExtra(INTENT_EXTRA_CHANGE_STAR_STATUES, false)) {
                Log.v(MyUtil.PACKAGE_NAME, "onStartCommand changeStarStatues");
                isStarred = !isStarred;
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
        Log.v(MyUtil.PACKAGE_NAME, "onBind");
        return null;
    }

    @Override
    public void onDestroy() {
        Log.v(MyUtil.PACKAGE_NAME, "onDes");
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
                .setRequiresDeviceIdle(true)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED)
                .setPeriodic(480000)
                .setPersisted(true)
                .build();
        JobScheduler jobScheduler = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
        jobScheduler.schedule(job);
    }

    private void performClipboardCheck() {
        Log.v(MyUtil.PACKAGE_NAME, "performClipboardCheck");
        if (!clipboardManager.hasPrimaryClip()) return;
        String clipString;
        try {
            //Don't use CharSequence .toString()!
            CharSequence charSequence = clipboardManager.getPrimaryClip().getItemAt(0).getText();
            clipString = String.valueOf(charSequence);
        } catch (Error ignored) {
            return;
        }
        if (clipString.trim().isEmpty()) return;
        if (clipString.equals(db.getClipHistory().get(0).getText())) return;
        int isImportant = db.isClipObjectStarred(clipString) ? 1 : 0;
        db.modifyClip(null, clipString, isImportant);
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
        if (isStarred) {
            thisClips = db.getStarredClipHistory(NUMBER_OF_CLIPS);
            if (db.getClipHistory().size() == 0) {
                showSingleNotification();
                return;
            }
            ClipObject topClip = db.getClipHistory().get(0);
            if (thisClips.size() == 0) {
                thisClips.add(0, topClip);
            } else if (!topClip.getText().equals(thisClips.get(0).getText())) {
                thisClips.add(0, topClip);
            }
        } else {
            thisClips = db.getClipHistory(NUMBER_OF_CLIPS);
        }

        int length = thisClips.size();
        if (length <= 1 && !isStarred) {
            showSingleNotification();
            return;
        }
        thisClips.add(new ClipObject(
                getString(R.string.clip_notification_single_text),
                new Date(),
                isStarred
        ));
        length += 1;

        length = (length > (NUMBER_OF_CLIPS + 1)) ? (NUMBER_OF_CLIPS + 1) : length;

        Intent resultIntent = new Intent(this, ClipObjectActionBridge.class)
                .putExtra(ClipObjectActionBridge.ACTION_CODE, ClipObjectActionBridge.ACTION_OPEN_MAIN);
        PendingIntent resultPendingIntent =
                PendingIntent.getService(
                        this,
                        pIntentId--,
                        resultIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );


        NotificationCompat.Builder preBuildNotification = new NotificationCompat.Builder(this)
                .setContentTitle(getString(R.string.clip_notification_title, MyUtil.stringLengthCut(thisClips.get(0).getText()))) //title
                .setContentIntent(resultPendingIntent)
                .setOngoing(pinOnTop)
                .setAutoCancel(!pinOnTop);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            String description = getString(R.string.clip_notification_text);
            if (isStarred) {
                description = "★ " + description;
            }
            preBuildNotification
                    .setContentText(description);
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
        switch (notificationPriority) {
            case 0:
                preBuildNotification.setPriority(NotificationCompat.PRIORITY_MIN);
                break;
            case 1:
                preBuildNotification.setPriority(NotificationCompat.PRIORITY_DEFAULT);
                break;
            case 2:
                preBuildNotification.setPriority(NotificationCompat.PRIORITY_HIGH);
                break;
        }

        NotificationClipListAdapter bigView = new NotificationClipListAdapter(this.getBaseContext(), thisClips.get(0));

        for (int i = 1; i < length; i++) {
            bigView.addClips(thisClips.get(i));
        }

        Notification n = preBuildNotification.build();

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
            n.bigContentView = bigView.build();
        }
        if (notificationPriority > 0) {
            n.icon = R.drawable.ic_stat_icon;
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
                    currentClip = MyUtil.stringLengthCut(thisClip.toString());
                }
            }
        }

        Intent resultIntent = new Intent(this, ClipObjectActionBridge.class)
                .putExtra(ClipObjectActionBridge.ACTION_CODE, ClipObjectActionBridge.ACTION_OPEN_MAIN);
        PendingIntent resultPendingIntent =
                PendingIntent.getService(
                        this,
                        pIntentId--,
                        resultIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );

        NotificationCompat.Builder preBuildN = new NotificationCompat.Builder(this)
                .setContentIntent(resultPendingIntent)
                .setContentTitle(getString(R.string.clip_notification_title, currentClip))
                .setOngoing(pinOnTop)
                .setAutoCancel(!pinOnTop);
        if (isStarred) {
            preBuildN
                    .setContentText(getString(R.string.clip_notification_starred_single_text));
        } else {

            preBuildN
                    .setContentText(getString(R.string.clip_notification_single_text));
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            preBuildN
                    .setSmallIcon(R.drawable.icon)
                    .setVisibility(NotificationCompat.VISIBILITY_SECRET)
                    .setColor(getResources().getColor(R.color.primary_light));
        } else {
            preBuildN.setSmallIcon(R.drawable.icon_shadow);
        }
        switch (notificationPriority) {
            case 0:
                preBuildN.setPriority(NotificationCompat.PRIORITY_MIN);
                break;
            case 1:
                preBuildN.setPriority(NotificationCompat.PRIORITY_DEFAULT);
                break;
            case 2:
                preBuildN.setPriority(NotificationCompat.PRIORITY_HIGH);
                break;
        }

        Notification n = preBuildN.build();
        if (notificationPriority > 0) {
            n.icon = R.drawable.ic_stat_icon;
        }

        notificationManager.cancel(0);
        notificationManager.notify(0, n);
    }

    public static void startCBService(Context context, boolean refreshNotification) {
        startCBService(context, refreshNotification, 0);
    }

    public static void startCBService(Context context, int myActivitiesOnForegroundMessage) {
        startCBService(context, false, myActivitiesOnForegroundMessage);
    }

    private static void startCBService(Context context, boolean forceShowNotification, int myActivitiesOnForegroundMessage) {
        Intent intent = new Intent(context, CBWatcherService.class)
                .putExtra(INTENT_EXTRA_FORCE_SHOW_NOTIFICATION, forceShowNotification)
                .putExtra(INTENT_EXTRA_MY_ACTIVITY_ON_FOREGROUND_MESSAGE, myActivitiesOnForegroundMessage);
        context.startService(intent);
    }

    private class NotificationClipListAdapter {

        private int buttonNumber = 9999;

        private RemoteViews expandedView;
        private Context context;

        public NotificationClipListAdapter(Context context, ClipObject clipObject) {
            this.context = context;
            String currentClip = clipObject.getText().trim();
            expandedView = new RemoteViews(this.context.getPackageName(), R.layout.notification_clip_list);
            expandedView.setTextViewText(R.id.current_clip, currentClip);
            //add pIntent for share
            Intent openShareIntent = new Intent(this.context, ClipObjectActionBridge.class)
                    .putExtra(Intent.EXTRA_TEXT, currentClip)
                    .putExtra(ClipObjectActionBridge.ACTION_CODE, ClipObjectActionBridge.ACTION_SHARE);
            PendingIntent pOpenShareIntent = PendingIntent.getService(this.context,
                    buttonNumber++,
                    openShareIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            expandedView.setOnClickPendingIntent(R.id.clip_share_button, pOpenShareIntent);
            //add pIntent for edit
            Intent openEditIntent = new Intent(this.context, ClipObjectActionBridge.class)
                    .putExtra(Intent.EXTRA_TEXT, currentClip)
                    .putExtra(ClipObjectActionBridge.STATUE_IS_STARRED, clipObject.isStarred())
                    .putExtra(ClipObjectActionBridge.ACTION_CODE, ClipObjectActionBridge.ACTION_EDIT);
            PendingIntent pOpenEditIntent = PendingIntent.getService(
                    this.context,
                    buttonNumber++,
                    openEditIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            expandedView.setOnClickPendingIntent(R.id.current_clip, pOpenEditIntent);
            //add pIntent for star click
            Intent openStarIntent = new Intent(this.context, CBWatcherService.class)
                    .putExtra(INTENT_EXTRA_CHANGE_STAR_STATUES, true);
            PendingIntent pOpenStarIntent = PendingIntent.getService(
                    this.context,
                    buttonNumber++,
                    openStarIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            expandedView.setOnClickPendingIntent(R.id.star, pOpenStarIntent);
            //set star's icon
            if (isStarred) {
                expandedView.setImageViewResource(R.id.star, R.drawable.ic_action_star_yellow);
            } else {
                expandedView.setImageViewResource(R.id.star, R.drawable.ic_action_star_outline_grey600);
            }
        }

        public NotificationClipListAdapter addClips(ClipObject clipObject) {
            //String s = clipObject.getText().trim();
            //Log.v(MyUtil.PACKAGE_NAME,"ID "+id);
            //Log.v(MyUtil.PACKAGE_NAME,s);
            //add view
            RemoteViews theClipView = new RemoteViews(context.getPackageName(), R.layout.notification_clip_card);
            if (clipObject.isStarred()) {
                theClipView.setTextViewText(R.id.clip_text, "★ " + MyUtil.stringLengthCut(clipObject.getText()));
            } else {
                theClipView.setTextViewText(R.id.clip_text, MyUtil.stringLengthCut(clipObject.getText()));
            }

            //add pIntent for edit

            Intent openEditIntent = new Intent(context, ClipObjectActionBridge.class)
                    .putExtra(Intent.EXTRA_TEXT, clipObject.getText())
                    .putExtra(ClipObjectActionBridge.STATUE_IS_STARRED, clipObject.isStarred())
                    .putExtra(ClipObjectActionBridge.ACTION_CODE, ClipObjectActionBridge.ACTION_EDIT);
            PendingIntent pOpenEditIntent = PendingIntent.getService(
                    context,
                    buttonNumber++,
                    openEditIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            theClipView.setOnClickPendingIntent(R.id.clip_text, pOpenEditIntent);

            if (clipObject.getText().equals(getString(R.string.clip_notification_single_text))) {
                //hide copy button for 'add'
                theClipView.setImageViewResource(R.id.clip_copy_button, R.drawable.transparent);
                theClipView.setTextViewText(R.id.clip_text, "✍ " + getString(R.string.clip_notification_single_text));
                theClipView.setViewVisibility(R.id.notification_item_down_line, View.GONE);
            } else {
                //add pIntent for copy
                Intent openCopyIntent = new Intent(context, ClipObjectActionBridge.class)
                        .putExtra(Intent.EXTRA_TEXT, clipObject.getText())
                        .putExtra(ClipObjectActionBridge.STATUE_IS_STARRED, true)
                        .putExtra(ClipObjectActionBridge.ACTION_CODE, ClipObjectActionBridge.ACTION_COPY);
                PendingIntent pOpenCopyIntent = PendingIntent.getService(context,
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
