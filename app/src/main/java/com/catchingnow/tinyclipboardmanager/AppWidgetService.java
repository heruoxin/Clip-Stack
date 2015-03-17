package com.catchingnow.tinyclipboardmanager;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
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
    private boolean mIsStarred;

    public AppWidgetRemoteViewsFactory(Context context, Intent intent) {
        mContext = context;
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

        remoteViews.setTextViewText(R.id.widget_card_date, MyUtil.getFormatDate(mContext, clip.getDate()));
        remoteViews.setTextViewText(R.id.widget_card_time, MyUtil.getFormatTime(mContext, clip.getDate()));
        remoteViews.setTextViewText(R.id.widget_card_text, MyUtil.stringLengthCut(clip.getText()));
        remoteViews.setImageViewResource(
                R.id.widget_card_star,
                clip.isStarred() ?
                        R.drawable.ic_action_star_yellow
                        :
                        R.drawable.ic_action_star_outline_grey600
                );
        Intent fillInEditorIntent = new Intent();
        final Bundle editorExtras = new Bundle();
        editorExtras.putInt(ClipObjectActionBridge.ACTION_CODE, ClipObjectActionBridge.ACTION_EDIT);
        editorExtras.putBoolean(ClipObjectActionBridge.STATUE_IS_STARRED, clip.isStarred());
        editorExtras.putString(Intent.EXTRA_TEXT, clip.getText());
        fillInEditorIntent.putExtras(editorExtras);
        remoteViews.setOnClickFillInIntent(
                R.id.widget_card_click_edit,
                fillInEditorIntent
        );
        Intent fillInStarIntent = new Intent();
        final Bundle starExtras = new Bundle();
        starExtras.putInt(ClipObjectActionBridge.ACTION_CODE, ClipObjectActionBridge.ACTION_STAR);
        starExtras.putBoolean(ClipObjectActionBridge.STATUE_IS_STARRED, clip.isStarred());
        starExtras.putString(Intent.EXTRA_TEXT, clip.getText());
        fillInStarIntent.putExtras(starExtras);
        remoteViews.setOnClickFillInIntent(
                R.id.widget_card_click_star,
                fillInStarIntent
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
