package com.catchingnow.tinyclipboardmanager;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import java.util.List;

/**
 * Created by heruoxin on 15/3/13.
 */


public class AppWidgetService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new AppWidgetRemoteViewsFactory(this.getApplicationContext(), intent);
    }
}

class AppWidgetRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {
    private Context mContext;
    private int mAppWidgetId;
    private Storage db;
    private List<ClipObject> clipObjects;

    public AppWidgetRemoteViewsFactory(Context context, Intent intent) {
        mContext = context;
        mAppWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);
    }

    public void onCreate() {
        // Since we reload the cursor in onDataSetChanged() which gets called immediately after
        // onCreate(), we do nothing here.
        db = Storage.getInstance(mContext);
    }

    public void onDestroy() {
    }

    public int getCount() {
        return clipObjects.size();
    }

    public RemoteViews getViewAt(int position) {
        ClipObject clip = clipObjects.get(position);
        RemoteViews remoteViews = new RemoteViews(mContext.getPackageName(), R.layout.app_widget_card);

        remoteViews.setTextViewText(R.id.widget_card_text, clip.getText());
        remoteViews.setTextViewText(R.id.widget_card_time, clip.getDate().toLocaleString());
        Intent fillInIntent = new Intent();
        final Bundle extras = new Bundle();
        extras.putBoolean(ClipObjectActionBridge.STATUE_IS_STARRED, clip.isStarred());
        extras.putString(Intent.EXTRA_TEXT, clip.getText());
        fillInIntent.putExtras(extras);
        remoteViews.setOnClickFillInIntent(
                R.id.widget_card_text,
                fillInIntent
        );

        return remoteViews;
    }

    public RemoteViews getLoadingView() {
        // We aren't going to return a default loading view in this sample
        return null;
    }

    public int getViewTypeCount() {
        return 1;
    }

    public long getItemId(int position) {
        return position;
    }

    public boolean hasStableIds() {
        return true;
    }

    public void onDataSetChanged() {
        clipObjects = db.getClipHistory();
    }
}
