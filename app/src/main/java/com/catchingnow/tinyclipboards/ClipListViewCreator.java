package com.catchingnow.tinyclipboards;

import android.content.Context;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.TextView;


/**
 * Created by heruoxin on 14/12/18.
 */
public class ClipListViewCreator {
    final static String PACKAGE_NAME = "com.catchingnow.tinyclipboards";
    RemoteViews expandedView;
    Context c;
    int id=0;
    public ClipListViewCreator (Context context, String currentClip) {
        c = context;
        expandedView = new RemoteViews(c.getPackageName(), R.layout.cliplist_view);
    }
    public ClipListViewCreator addClips (String s) {
        id += 1;
        Log.v(PACKAGE_NAME,"ID "+id);
        Log.v(PACKAGE_NAME,s);
        RemoteViews theClipView = new RemoteViews(c.getPackageName(), R.layout.clipaction_view);
        theClipView.setTextViewText(R.id.clip_text, s);
        expandedView.addView(R.id.main_view, theClipView);
        return this;
    }
    public RemoteViews build () {
        //expandedView.setTextViewText(R.id.text, "Hello World!");
        return expandedView;
    }
}
