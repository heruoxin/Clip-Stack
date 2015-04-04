package com.catchingnow.tinyclipboardmanager;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.widget.RemoteViews;

import java.util.Date;


/**
 * Implementation of App Widget functionality.
 */
public class AppWidget extends AppWidgetProvider {
    public static final String WIDGET_IS_STARRED = "widget_is_starred";

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

    private static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                        int appWidgetId) {
        SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(context);
        boolean isStarred = preference.getBoolean(WIDGET_IS_STARRED, false);

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.app_widget);

        //set title
        views.setImageViewResource(
                R.id.widget_title_star,
                isStarred ?
                        R.drawable.ic_action_star_white
                        :
                        R.drawable.ic_action_star_outline_white
        );

        //bind title
        PendingIntent pMainIntent = PendingIntent.getActivity(
                context,
                0,
                new Intent(context, ActivityMain.class),
                PendingIntent.FLAG_UPDATE_CURRENT
        );
        views.setOnClickPendingIntent(R.id.widget_title_text, pMainIntent);

        PendingIntent pStarIntent = PendingIntent.getService(
                context,
                1,
                new Intent(context, ClipObjectActionBridge.class)
                        .putExtra(ClipObjectActionBridge.ACTION_CODE,
                                ClipObjectActionBridge.ACTION_CHANGE_WIDGET_STAR),
                PendingIntent.FLAG_UPDATE_CURRENT
        );
        views.setOnClickPendingIntent(R.id.widget_title_star, pStarIntent);

        PendingIntent pAddIntent = PendingIntent.getActivity(
                context,
                2,
                new Intent(context, ActivityEditor.class)
                        .putExtra(ClipObjectActionBridge.STATUE_IS_STARRED, isStarred)
                        .putExtra(Intent.EXTRA_TEXT, ""),
                PendingIntent.FLAG_UPDATE_CURRENT
        );
        views.setOnClickPendingIntent(R.id.widget_title_add, pAddIntent);

        //set main view
        Intent intent = new Intent(context, AppWidgetService.class)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                .putExtra(WIDGET_IS_STARRED, isStarred)
                .putExtra("TIME", new Date().getTime());
        intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
        views.setRemoteAdapter(R.id.widget_main_view, intent);
        views.setEmptyView(R.id.widget_main_view, R.layout.app_widget_card_empty);
        views.setPendingIntentTemplate(
                R.id.widget_main_view,
                PendingIntent.getService(
                        context,
                        8,
                        new Intent(context, ClipObjectActionBridge.class),
                        PendingIntent.FLAG_UPDATE_CURRENT
                ));

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    public static void updateAllAppWidget(Context context) {
        int ids[] = AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context, AppWidget.class));
        for (int id : ids) {
            AppWidget.updateAppWidget(context, AppWidgetManager.getInstance(context), id);
        }
    }

}

