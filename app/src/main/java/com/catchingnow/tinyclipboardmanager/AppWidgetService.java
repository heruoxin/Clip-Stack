package com.catchingnow.tinyclipboardmanager;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
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
    private boolean mIsStarred;
    private DateFormat dateFormat;
    private DateFormat timeFormat;

    public AppWidgetRemoteViewsFactory(Context context, Intent intent) {
        mContext = context;
        dateFormat = new SimpleDateFormat(mContext.getString(R.string.date_format));
        timeFormat = new SimpleDateFormat(mContext.getString(R.string.time_format));
        mAppWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);
        mIsStarred = intent.getBooleanExtra(AppWidget.WIDGET_IS_STARRED, false);

    }

    public void onCreate() {
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

        remoteViews.setTextViewText(R.id.widget_card_date, dateFormat.format(clip.getDate()));
        remoteViews.setTextViewText(R.id.widget_card_time, timeFormat.format(clip.getDate()));
        remoteViews.setTextViewText(R.id.widget_card_text, MyUtil.stringLengthCut(clip.getText()).trim());
        remoteViews.setImageViewResource(
                R.id.widget_card_star,
                clip.isStarred() ?
                        R.drawable.ic_action_star_yellow
                        :
                        R.drawable.ic_action_star_outline_grey600
                );
        Intent fillInIntent = new Intent();
        final Bundle extras = new Bundle();
        extras.putBoolean(ClipObjectActionBridge.STATUE_IS_STARRED, clip.isStarred());
        extras.putString(Intent.EXTRA_TEXT, clip.getText());
        fillInIntent.putExtras(extras);
        remoteViews.setOnClickFillInIntent(
                R.id.widget_card,
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
        return false;
    }

    public void onDataSetChanged() {
        clipObjects = null;
        if (mIsStarred) {
            clipObjects = db.getStarredClipHistory();
        } else {
            clipObjects = db.getClipHistory();
        }
    }
}
