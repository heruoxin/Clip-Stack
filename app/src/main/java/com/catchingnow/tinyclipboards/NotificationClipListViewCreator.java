package com.catchingnow.tinyclipboards;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;


/**
 * Created by heruoxin on 14/12/18.
 */
public class NotificationClipListViewCreator {
    private final static String PACKAGE_NAME = "com.catchingnow.tinyclipboards";
    public final static String CLIPBOARD_STRING = "com.catchingnow.tinyclipboards.clipboardString";
    public final static String CLIPBOARD_ACTION = "com.catchingnow.tinyclipboards.clipboarAction";

    private int buttonNumber = 0;

    private RemoteViews expandedView;
    private Context c;
    int id=0;
    public NotificationClipListViewCreator(Context context, String currentClip) {
        c = context;
        currentClip = currentClip.trim();
        expandedView = new RemoteViews(c.getPackageName(), R.layout.notification_clip_list);
        expandedView.setTextViewText(R.id.current_clip, currentClip);
        //add pIntent for share
        Intent openShareIntent = new Intent(c, StringActionIntentService.class);
        openShareIntent.putExtra(CLIPBOARD_STRING, currentClip);
        openShareIntent.putExtra(CLIPBOARD_ACTION, StringActionIntentService.ACTION_SHARE);
        PendingIntent pOpenShareIntent = PendingIntent.getService(c, buttonNumber++, openShareIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        expandedView.setOnClickPendingIntent(R.id.clip_share_button, pOpenShareIntent);
    }
    public NotificationClipListViewCreator addClips (String s) {
        id += 1;
        s = s.trim();
        //Log.v(PACKAGE_NAME,"ID "+id);
        //Log.v(PACKAGE_NAME,s);
        //add view
        RemoteViews theClipView = new RemoteViews(c.getPackageName(), R.layout.notification_clip_card);
        theClipView.setTextViewText(R.id.clip_text, s);

        //add pIntent for copy

        Intent openCopyIntent = new Intent(c, StringActionIntentService.class);
        openCopyIntent.putExtra(CLIPBOARD_STRING, s);
        openCopyIntent.putExtra(CLIPBOARD_ACTION, StringActionIntentService.ACTION_COPY);
        PendingIntent pOpenCopyIntent = PendingIntent.getService(c, buttonNumber++, openCopyIntent, PendingIntent.FLAG_UPDATE_CURRENT);




        theClipView.setOnClickPendingIntent(R.id.clip_copy_button, pOpenCopyIntent);
        expandedView.addView(R.id.main_view, theClipView);
        return this;
    }
    public RemoteViews build () {
        //expandedView.setTextViewText(R.id.text, "Hello World!");
        return expandedView;
    }
}
