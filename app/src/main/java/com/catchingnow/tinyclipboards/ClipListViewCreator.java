package com.catchingnow.tinyclipboards;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Button;
import android.widget.RemoteViews;
import android.widget.TextView;


/**
 * Created by heruoxin on 14/12/18.
 */
public class ClipListViewCreator {
    private final static String PACKAGE_NAME = "com.catchingnow.tinyclipboards";
    public final static String CLIPBOARD_STRING = "com.catchingnow.tinyclipboards.clipboardString";
    private int buttonNumber = 0;

    private RemoteViews expandedView;
    private Context c;
    int id=0;
    public ClipListViewCreator (Context context, String currentClip) {
        c = context;
        currentClip = currentClip.trim();
        expandedView = new RemoteViews(c.getPackageName(), R.layout.cliplist_view);
        expandedView.setTextViewText(R.id.current_clip, c.getString(R.string.clip_notification_title)+currentClip);
    }
    public ClipListViewCreator addClips (String s) {
        id += 1;
        s = s.trim();
        Log.v(PACKAGE_NAME,"ID "+id);
        Log.v(PACKAGE_NAME,s);
        //add view
        RemoteViews theClipView = new RemoteViews(c.getPackageName(), R.layout.clipaction_view);
        theClipView.setTextViewText(R.id.clip_text, s);

        //add pIntent

        Intent openCopyIntent = new Intent(c, copyToClipboardIntentService.class);
        openCopyIntent.putExtra(CLIPBOARD_STRING, s);
        PendingIntent pOpenCopyIntent = PendingIntent.getService(c, buttonNumber++, openCopyIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        theClipView.setOnClickPendingIntent(R.id.clip_line, pOpenCopyIntent);
        expandedView.addView(R.id.main_view, theClipView);
        return this;
    }
    public RemoteViews build () {
        //expandedView.setTextViewText(R.id.text, "Hello World!");
        return expandedView;
    }
}
