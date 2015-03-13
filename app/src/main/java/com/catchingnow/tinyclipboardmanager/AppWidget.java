package com.catchingnow.tinyclipboardmanager;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.ListView;
import android.widget.RemoteViews;

import java.util.Date;


/**
 * Implementation of App Widget functionality.
 */
public class AppWidget extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        final int N = appWidgetIds.length;
        for (int i = 0; i < N; i++) {
            updateAppWidget(context, appWidgetManager, appWidgetIds[i]);
        }
    }


    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.app_widget);

        //bind title
        PendingIntent pMainIntent = PendingIntent.getActivity(
                context,
                0,
                new Intent(context, ActivityMain.class),
                PendingIntent.FLAG_UPDATE_CURRENT
        );
        views.setOnClickPendingIntent(R.id.widget_title_text, pMainIntent);

        views.setOnClickPendingIntent(R.id.widget_title_add,
                openEditorPendingIntent(
                        context,
                        new ClipObject("", new Date()),
                        1
                ));

        //set main view
        Intent intent = new Intent(context, AppWidgetService.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
        views.setRemoteAdapter(R.id.widget_main_view, intent);
        views.setEmptyView(R.id.widget_main_view, R.layout.app_widget_card_empty);
        views.setPendingIntentTemplate(
                R.id.widget_main_view,
                PendingIntent.getActivity(
                        context,
                        3,
                        new Intent(context, ActivityEditor.class),
                        PendingIntent.FLAG_UPDATE_CURRENT
                ));

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    private static PendingIntent openEditorPendingIntent(Context context, ClipObject clip, int id) {
        return PendingIntent.getActivity(
                context,
                id,
                new Intent(context, ActivityEditor.class)
                        .putExtra(ClipObjectActionBridge.STATUE_IS_STARRED, clip.isStarred())
                        .putExtra(Intent.EXTRA_TEXT, clip.getText()),
                PendingIntent.FLAG_UPDATE_CURRENT
        );
    }

}


