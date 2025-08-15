package com.example.alimentaacao.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import com.example.alimentaacao.R;

public class FeedWidgetProvider extends AppWidgetProvider {
    public static final String ACTION_REFRESH = "com.example.alimentaacao.widget.REFRESH";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_feed);
            Intent svcIntent = new Intent(context, FeedWidgetService.class);
            views.setRemoteAdapter(R.id.lvFeed, svcIntent);

            Intent refresh = new Intent(context, FeedWidgetProvider.class);
            refresh.setAction(ACTION_REFRESH);
            PendingIntent pi = PendingIntent.getBroadcast(
                    context, 0, refresh, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            views.setOnClickPendingIntent(R.id.tvTitle, pi);

            appWidgetManager.updateAppWidget(appWidgetId, views);
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.lvFeed);
        }
    }

    @Override public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (ACTION_REFRESH.equals(intent.getAction())) {
            AppWidgetManager mgr = AppWidgetManager.getInstance(context);
            int[] ids = mgr.getAppWidgetIds(new ComponentName(context, FeedWidgetProvider.class));
            mgr.notifyAppWidgetViewDataChanged(ids, R.id.lvFeed);
        }
    }
}
