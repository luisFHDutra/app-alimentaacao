package com.example.alimentaacao.ui.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.RemoteViews;

import com.example.alimentaacao.R;
import com.example.alimentaacao.ui.MainActivity;

public class ProximosEventosWidget extends AppWidgetProvider {

    public static final String ACTION_REFRESH = "com.example.alimentaacao.ACTION_REFRESH_WIDGET";

    @Override
    public void onUpdate(Context context, AppWidgetManager mgr, int[] appWidgetIds) {
        for (int id : appWidgetIds) {
            RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget_proximos_eventos);

            // Adapter para a ListView do widget
            Intent svc = new Intent(context, ProximosEventosWidgetService.class);
            svc.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id);
            // Necessário para Intent únicas por ID
            svc.setData(Uri.parse(svc.toUri(Intent.URI_INTENT_SCHEME)));

            rv.setRemoteAdapter(R.id.listEvents, svc);
            rv.setEmptyView(R.id.listEvents, R.id.tvEmpty);

            // Clique em uma linha: abre app na aba de eventos
            Intent open = new Intent(context, MainActivity.class)
                    .putExtra("open_tab", R.id.nav_eventos)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent piOpen = PendingIntent.getActivity(
                    context, id, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            rv.setPendingIntentTemplate(R.id.listEvents, piOpen);

            // Botão de refresh
            Intent refresh = new Intent(context, ProximosEventosWidget.class).setAction(ACTION_REFRESH);
            PendingIntent piRefresh = PendingIntent.getBroadcast(
                    context, id, refresh, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            rv.setOnClickPendingIntent(R.id.btnRefresh, piRefresh);

            mgr.updateAppWidget(id, rv);
        }
        // dispara uma atualização de dados após ligar o adapter
        mgr.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.listEvents);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (ACTION_REFRESH.equals(intent.getAction())) {
            AppWidgetManager mgr = AppWidgetManager.getInstance(context);
            int[] ids = mgr.getAppWidgetIds(new ComponentName(context, ProximosEventosWidget.class));
            // avisa o sistema para recarregar a coleção
            mgr.notifyAppWidgetViewDataChanged(ids, R.id.listEvents);
        }
    }
}
