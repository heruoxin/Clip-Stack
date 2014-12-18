package com.catchingnow.tinyclipboards;

import android.content.Context;
import android.widget.RemoteViews;


/**
 * Created by heruoxin on 14/12/18.
 */
public class ClipListViewCreator {
    RemoteViews expandedView;
    Context c;
    public ClipListViewCreator (Context context, String currentClip) {
        c = context;
        expandedView = new RemoteViews(c.getPackageName(), R.layout.cliplist_view);
    }
    public ClipListViewCreator addClips (String s) {
        return this;
    }
    public RemoteViews build () {
        expandedView.setTextViewText(R.id.text, "Hello World!");
        return expandedView;
    }
}
