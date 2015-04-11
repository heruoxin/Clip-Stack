package com.catchingnow.tinyclipboardmanager;

import android.annotation.TargetApi;
import android.app.Notification;
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
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class CBWatcherService extends Service {

    public final static String INTENT_EXTRA_FORCE_SHOW_NOTIFICATION = "com.catchingnow.tinyclipboardmanager.EXTRA.FORCE_SHOW_NOTIFICATION";
    public final static String INTENT_EXTRA_MY_ACTIVITY_ON_FOREGROUND_MESSAGE = "com.catchingnow.tinyclipboardmanager.EXTRA.MY_ACTIVITY_ON_FOREGROUND_MESSAGE";
    public final static String INTENT_EXTRA_CHANGE_STAR_STATUES = "com.catchingnow.tinyclipboardmanager.EXTRA.CHANGE_STAR_STATUES";
    public final static String INTENT_EXTRA_TEMPORARY_STOP = "com.catchingnow.tinyclipboardmanager.EXTRA.TEMPORARY_STOP";
    public final static String INTENT_EXTRA_CHECK_CLIPBOARD_NOW = "com.catchingnow.tinyclipboardmanager.EXTRA.CHECK_CLIPBOARD_NOW";

    public final static String ON_DESTROY = "onCBWatcherServiceDestroy";

    public final static int JOB_ID = 1;
    public int NUMBER_OF_CLIPS = 5; //3-6

    private final static String NOTIFICATION_GROUP = "notification_group";
    private Context mContext;
    private NotificationManagerCompat notificationManager;
    private ClipboardManager clipboardManager;
    private SharedPreferences preference;
    private Storage db;
    private Handler mHandler;

    boolean allowService = true;
    boolean allowNotification = true;
    protected boolean isStarred = false;
    protected boolean temporaryStop = false;
    private boolean pinOnTop = false;
    private int notificationPriority = 0;
    private int isMyActivitiesOnForeground = 0;
    private int pIntentId = 999;
    private OnPrimaryClipChangedListener listener = new OnPrimaryClipChangedListener() {
        public void onPrimaryClipChanged() {
            performClipboardCheck();
        }
    };

    @Override
    public void onCreate() {
        Log.v(MyUtil.PACKAGE_NAME, "onCreate CBService");
        mContext = this;
        mHandler = new Handler();
        preference = PreferenceManager.getDefaultSharedPreferences(this);
        readPreference();
        notificationManager = NotificationManagerCompat.from(this);
        db = Storage.getInstance(this.getBaseContext());
        clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        clipboardManager.addPrimaryClipChangedListener(listener);
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            Log.w(MyUtil.PACKAGE_NAME, "Not support JobScheduler");
        } else {
            bindJobScheduler();
        }
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        boolean beforeTemporaryStop = temporaryStop;
        if (intent == null) {
            intent = new Intent();
        }
        int myActivitiesOnForegroundMessage = intent.getIntExtra(INTENT_EXTRA_MY_ACTIVITY_ON_FOREGROUND_MESSAGE, 0);
        isMyActivitiesOnForeground += myActivitiesOnForegroundMessage;
        readPreference();

        if (intent.getBooleanExtra(INTENT_EXTRA_CHECK_CLIPBOARD_NOW, false)) {
            performClipboardCheck();
        }

        if (!allowService) {
            if (isMyActivitiesOnForeground <= 0) {
                stopSelf();
                isMyActivitiesOnForeground = 0;
                return Service.START_NOT_STICKY;
            }
        }

        temporaryStop = intent.getBooleanExtra(INTENT_EXTRA_TEMPORARY_STOP, false);
        if (temporaryStop != beforeTemporaryStop) {
            showNotification();

            Intent it = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
            sendBroadcast(it);

            final String toastText = temporaryStop ?
                    getString(R.string.toast_service_temporary_stop)
                    :
                    getString(R.string.toast_service_temporary_resume);
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(
                            mContext,
                            toastText,
                            Toast.LENGTH_SHORT
                            ).show();
                }
            });
        }

        if (intent.getBooleanExtra(INTENT_EXTRA_CHANGE_STAR_STATUES, false)) {
            isStarred = !isStarred;
            showNotification();
            return START_STICKY;
        }
        if (intent.getBooleanExtra(INTENT_EXTRA_FORCE_SHOW_NOTIFICATION, false)) {
            showNotification();
            return START_STICKY;
        }

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        notificationManager.cancelAll();
        ((ClipboardManager) getSystemService(CLIPBOARD_SERVICE)).removePrimaryClipChangedListener(listener);
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(new Intent(ON_DESTROY));
        super.onDestroy();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void bindJobScheduler() {
        // JobScheduler for auto clean sqlite
        JobInfo job = new JobInfo.Builder(JOB_ID, new ComponentName(this, SyncJobService.class))
                .setRequiresCharging(true)
                .setRequiresDeviceIdle(true)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED)
                .setPeriodic(24*60*60*1000)
                .setPersisted(true)
                .build();
        JobScheduler jobScheduler = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
        jobScheduler.cancel(JOB_ID);
        jobScheduler.schedule(job);
    }

    private void readPreference() {
        allowService = preference.getBoolean(ActivitySetting.PREF_START_SERVICE, true);
        allowNotification = preference.getBoolean(ActivitySetting.PREF_NOTIFICATION_SHOW, true);
        notificationPriority = Integer.parseInt(preference.getString(ActivitySetting.PREF_NOTIFICATION_PRIORITY, "0"));
        pinOnTop = preference.getBoolean(ActivitySetting.PREF_NOTIFICATION_PIN, false);
    }

    private void performClipboardCheck() {
        Log.v(MyUtil.PACKAGE_NAME, "performClipboardCheck");
        if (temporaryStop) return;
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
        if (clipString.equals("null")) return;
        if (db.getClipHistory().size() > 0) {
            if (clipString.equals(db.getClipHistory().get(0).getText())) return;
        }
        int isImportant = db.isClipObjectStarred(clipString) ? 1 : 0;
        db.modifyClip(null, clipString, isImportant);
    }

    private boolean checkNotificationPermission() {
        if (allowNotification && allowService) {
            return true;
        }
        notificationManager.cancelAll();
        return false;
    }

    private void showNotification() {
        int notificationID = 1;

        if (!checkNotificationPermission()) {
            return;
        }

        List<ClipObject> thisClips;
        if (isStarred) {
            thisClips = db.getStarredClipHistory(NUMBER_OF_CLIPS - 1);
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

        Intent openMainDialogIntent = new Intent(this, ClipObjectActionBridge.class)
                .putExtra(ClipObjectActionBridge.ACTION_CODE, ClipObjectActionBridge.ACTION_OPEN_MAIN_DIALOG);
        PendingIntent pOpenMainDialogIntent =
                PendingIntent.getService(
                        this,
                        pIntentId--,
                        openMainDialogIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );

        NotificationCompat.Builder preBuildNotification = new NotificationCompat.Builder(this)
                .setContentTitle(getString(R.string.clip_notification_title, MyUtil.stringLengthCut(thisClips.get(0).getText()))) //title
                .setSmallIcon(R.drawable.ic_stat_icon_colorful)
                .setVisibility(NotificationCompat.VISIBILITY_SECRET)
                .setColor(getResources().getColor(R.color.primary_light))
                .setContentIntent(pOpenMainDialogIntent)
                .setOngoing(pinOnTop)
                .setAutoCancel(false)
                .setGroup(NOTIFICATION_GROUP)
                .setGroupSummary(true);

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            // 5.1 icon tmp fix
            preBuildNotification
                    .setSmallIcon(R.drawable.ic_stat_icon)
                    .setColor(getResources().getColor(R.color.accent));
        }

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

        NotificationClipListAdapter notificationClipListAdapter = new NotificationClipListAdapter(this.getBaseContext(), thisClips.get(0));

        for (int i = 1; i < length; i++) {
            notificationClipListAdapter.addClips(thisClips.get(i));
        }

        Notification n = preBuildNotification.build();

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
            n.bigContentView = notificationClipListAdapter.build();
        }
        n.icon = R.drawable.ic_stat_icon;

        notificationManager.notify(0, n);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            for (Notification notification : notificationClipListAdapter.getWearNotifications()) {
                notificationManager.notify(notificationID++, notification);
            }
        }
    }

    private void showSingleNotification() {

        String currentClip = "Clipboard is empty.";
        ClipboardManager cb = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (cb.hasPrimaryClip()) {
            ClipData cd = cb.getPrimaryClip();
            if (cd.getDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)) {
                CharSequence thisClip = cd.getItemAt(0).getText();
                if (thisClip != null) {
                    if (!thisClip.toString().isEmpty()) {
                        currentClip = MyUtil.stringLengthCut(thisClip.toString());
                    }
                }
            }
        }

        Intent openMainDialogIntent = new Intent(this, ClipObjectActionBridge.class)
                .putExtra(ClipObjectActionBridge.ACTION_CODE, ClipObjectActionBridge.ACTION_OPEN_MAIN_DIALOG);
        PendingIntent pOpenMainDialogIntent =
                PendingIntent.getService(
                        this,
                        pIntentId--,
                        openMainDialogIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        Intent openEditorIntent = new Intent(this, ClipObjectActionBridge.class)
                .putExtra(ClipObjectActionBridge.ACTION_CODE, ClipObjectActionBridge.ACTION_EDIT);
        PendingIntent pOpenEditorIntent =
                PendingIntent.getService(
                        this,
                        pIntentId--,
                        openEditorIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );

        NotificationCompat.Builder preBuildN = new NotificationCompat.Builder(this)
                .setContentIntent(pOpenMainDialogIntent)
                .setContentTitle(getString(R.string.clip_notification_title, currentClip))
                .setOngoing(pinOnTop)
                .setAutoCancel(false)
                .setSmallIcon(R.drawable.ic_stat_icon_colorful)
                .setVisibility(NotificationCompat.VISIBILITY_SECRET)
                .setColor(getResources().getColor(R.color.primary_light))
                .addAction(R.drawable.ic_action_add, getString(R.string.action_add), pOpenEditorIntent);

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            // 5.1 icon tmp fix
            preBuildN.setSmallIcon(R.drawable.ic_stat_icon);
        }

        if (isStarred) {
            preBuildN
                    .setContentText(getString(R.string.clip_notification_starred_single_text));
        } else {

            preBuildN
                    .setContentText(getString(R.string.clip_notification_single_text));
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
        n.icon = R.drawable.ic_stat_icon;

        notificationManager.notify(0, n);
    }

    public static void startCBService(Context context, boolean refreshNotification) {
        startCBService(context, refreshNotification, true, 0);
    }

    public static void startCBService(Context context, int myActivitiesOnForegroundMessage) {
        startCBService(context, false, true, myActivitiesOnForegroundMessage);
    }

    public static void startCBService(Context context, boolean refreshNotification, boolean checkClipboardNow) {
        startCBService(context, refreshNotification, checkClipboardNow, 0);
    }


    public static void startCBService(Context context, boolean forceShowNotification, boolean checkClipboardNow, int myActivitiesOnForegroundMessage) {
        Intent intent = new Intent(context, CBWatcherService.class)
                .putExtra(INTENT_EXTRA_FORCE_SHOW_NOTIFICATION, forceShowNotification)
                .putExtra(INTENT_EXTRA_CHECK_CLIPBOARD_NOW, checkClipboardNow)
                .putExtra(INTENT_EXTRA_MY_ACTIVITY_ON_FOREGROUND_MESSAGE, myActivitiesOnForegroundMessage);
        context.startService(intent);
    }

    private class NotificationClipListAdapter {

        private int buttonNumber = 9999;
        private RemoteViews expandedView;
        private List<ClipObject> clips;
        private Context context;

        public NotificationClipListAdapter(Context context, ClipObject clipObject) {
            this.context = context;
            String currentClip = clipObject.getText();
            clips = new ArrayList<>();
            clips.add(clipObject);
            expandedView = new RemoteViews(this.context.getPackageName(), R.layout.notification_clip);
            expandedView.setTextViewText(R.id.current_clip, MyUtil.stringLengthCut(currentClip));
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
            expandedView.removeAllViews(R.id.notification_list);
        }

        public void addClips(ClipObject clipObject) {

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
                theClipView.setTextViewText(R.id.clip_text, "✍ " + getString(R.string.clip_notification_single_text));
                theClipView.setViewVisibility(R.id.notification_item_down_line, View.GONE);
                theClipView.setImageViewResource(R.id.clip_copy_button,
                        temporaryStop ?
                                R.drawable.ic_notification_action_play
                                :
                                R.drawable.ic_notification_action_pause
                        );
                Intent pauseIntent = new Intent(context, CBWatcherService.class)
                        .putExtra(INTENT_EXTRA_TEMPORARY_STOP, !temporaryStop);
                PendingIntent pPauseIntent = PendingIntent.getService(context,
                        buttonNumber++,
                        pauseIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                        );
                theClipView.setOnClickPendingIntent(R.id.clip_copy_button, pPauseIntent);
            } else {
                clips.add(clipObject);
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

            expandedView.addView(R.id.notification_list, theClipView);
        }

        public List<Notification> getWearNotifications() {

            List<Notification> notifications = new ArrayList<>();

            Intent openStarIntent = new Intent(this.context, CBWatcherService.class)
                    .putExtra(INTENT_EXTRA_CHANGE_STAR_STATUES, true);
            PendingIntent pOpenStarIntent = PendingIntent.getService(
                    this.context,
                    buttonNumber++,
                    openStarIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);

            Intent openMainIntent = new Intent(context, ClipObjectActionBridge.class)
                    .putExtra(ClipObjectActionBridge.ACTION_CODE, ClipObjectActionBridge.ACTION_OPEN_MAIN_DIALOG);
            PendingIntent pOpenMainIntent =
                    PendingIntent.getService(
                            context,
                            pIntentId--,
                            openMainIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT
                    );

            for (ClipObject clip : clips) {
                notifications.add(new NotificationCompat.Builder(mContext)
                        //.setStyle(wearPageStyle)
                        .setContentTitle(
                                MyUtil.getFormatDate(context, clip.getDate())
                                        + " "
                                        + MyUtil.getFormatTime(context, clip.getDate())
                        )
                        .setContentText(MyUtil.stringLengthCut(clip.getText(), 300))
                        .setSmallIcon(R.drawable.ic_stat_icon_colorful)
                        .setGroup(NOTIFICATION_GROUP)
                        .addAction(
                                isStarred ?
                                        R.drawable.ic_action_star_outline_white
                                        :
                                        R.drawable.ic_action_star_white,
                                isStarred ?
                                        getString(R.string.switch_all_items)
                                        :
                                        getString(R.string.switch_only_starred_items),
                                pOpenStarIntent
                        )
                        .addAction(R.drawable.ic_stat_icon, getString(R.string.app_name), pOpenMainIntent)
                        .build());
            }
            int size = notifications.size();
            if (size > 4) {
                notifications = notifications.subList(0, 4);
            }
            Collections.reverse(notifications);
            return notifications;

        }

        public RemoteViews build() {
            //expandedView.setTextViewText(R.id.text, "Hello World!");
            return expandedView;
        }
    }
}
