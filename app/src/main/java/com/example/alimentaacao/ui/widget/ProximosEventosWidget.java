package com.example.alimentaacao.ui.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.example.alimentaacao.R;
import com.example.alimentaacao.ui.MainActivity;

public class ProximosEventosWidget extends AppWidgetProvider {

    public static final String ACTION_REFRESH = "com.example.alimentaacao.ACTION_REFRESH_WIDGET";

    @Override
    public void onUpdate(Context context, AppWidgetManager mgr, int[] appWidgetIds) {
        // Atualiza todos os widgets desta classe
        for (int id : appWidgetIds) {
            RemoteViews rv = buildViews(context);
            attachIntents(context, rv, id);
            mgr.updateAppWidget(id, rv);
        }
        // Aqui você pode disparar um Worker/Service para buscar eventos e depois chamar updateAll(...)
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (ACTION_REFRESH.equals(intent.getAction())) {
            updateAll(context);
        }
    }

    private void updateAll(Context context) {
        AppWidgetManager mgr = AppWidgetManager.getInstance(context);
        int[] ids = mgr.getAppWidgetIds(new ComponentName(context, ProximosEventosWidget.class));
        onUpdate(context, mgr, ids);
    }

    private RemoteViews buildViews(Context context) {
        RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget_proximos_eventos);
        // Exemplo: texto fake (troque depois por dados reais)
        rv.setTextViewText(R.id.tvLine1, "Sábado 10:00 — Feira Solidária");
        rv.setTextViewText(R.id.tvLine2, "Domingo 09:00 — Coleta de Alimentos");
        rv.setTextViewText(R.id.tvLine3, "Terça 19:30 — Jantar Comunitário");
        return rv;
    }

    private void attachIntents(Context context, RemoteViews rv, int appWidgetId) {
        // Abrir o app (aba de eventos)
        Intent open = new Intent(context, MainActivity.class)
                .putExtra("open_tab", R.id.nav_eventos)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent piOpen = PendingIntent.getActivity(
                context, appWidgetId, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        rv.setOnClickPendingIntent(R.id.tvTitle, piOpen);

        // Refresh do widget
        Intent refresh = new Intent(context, ProximosEventosWidget.class).setAction(ACTION_REFRESH);
        PendingIntent piRefresh = PendingIntent.getBroadcast(
                context, appWidgetId, refresh,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        rv.setOnClickPendingIntent(R.id.btnRefresh, piRefresh);
    }
}
