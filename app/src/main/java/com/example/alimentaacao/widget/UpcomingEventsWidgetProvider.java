package com.example.alimentaacao.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.text.format.DateFormat;
import android.widget.RemoteViews;

import com.example.alimentaacao.R;
import com.example.alimentaacao.ui.MainActivity;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Widget simples listando até 3 próximos eventos (título + data). */
public class UpcomingEventsWidgetProvider extends AppWidgetProvider {

    public static final String ACTION_REFRESH =
            "com.example.alimentaacao.widget.UpcomingEventsWidgetProvider.REFRESH";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] ids) {
        for (int id : ids) {
            updateNow(context, appWidgetManager, id);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (ACTION_REFRESH.equals(intent.getAction())) {
            AppWidgetManager mgr = AppWidgetManager.getInstance(context);
            int[] ids = mgr.getAppWidgetIds(new ComponentName(context, UpcomingEventsWidgetProvider.class));
            for (int id : ids) {
                updateNow(context, mgr, id);
            }
        }
    }

    private void updateNow(Context context, AppWidgetManager mgr, int appWidgetId) {
        RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget_upcoming_events);

        // Clique no cabeçalho/raiz abre a aba Eventos do app
        Intent open = new Intent(context, MainActivity.class);
        open.putExtra("open_tab", "eventos");
        PendingIntent piOpen = PendingIntent.getActivity(
                context, 1001, open, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        rv.setOnClickPendingIntent(R.id.widget_root, piOpen);

        // Botão refresh (broadcast para o próprio provider)
        Intent refreshI = new Intent(context, UpcomingEventsWidgetProvider.class);
        refreshI.setAction(ACTION_REFRESH);
        PendingIntent piRefresh = PendingIntent.getBroadcast(
                context, 1002, refreshI, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        rv.setOnClickPendingIntent(R.id.btnRefresh, piRefresh);

        // Estado inicial (carregando/limpo)
        rv.setTextViewText(R.id.row1, "Carregando...");
        rv.setTextViewText(R.id.row2, "");
        rv.setTextViewText(R.id.row3, "");
        mgr.updateAppWidget(appWidgetId, rv);

        // Busca assíncrona no Firestore: próximos 3 pela data
        FirebaseFirestore.getInstance().collection("events")
                .whereGreaterThan("dateTime", new Date())
                .orderBy("dateTime", Query.Direction.ASCENDING)
                .limit(3)
                .get()
                .addOnSuccessListener(qs -> {
                    List<String> lines = new ArrayList<>();
                    if (qs != null) {
                        for (com.google.firebase.firestore.DocumentSnapshot ds : qs.getDocuments()) {
                            String title = ds.getString("title");
                            Date dt = ds.getDate("dateTime");
                            String when = (dt != null)
                                    ? formatDateShort(context, dt)
                                    : "-";
                            lines.add((title != null ? title : "(sem título)") + " — " + when);
                        }
                    }
                    applyLinesAndUpdate(context, mgr, appWidgetId, rv, lines);
                })
                .addOnFailureListener(e -> {
                    rv.setTextViewText(R.id.row1, "Falha ao carregar");
                    rv.setTextViewText(R.id.row2, "");
                    rv.setTextViewText(R.id.row3, "");
                    mgr.updateAppWidget(appWidgetId, rv);
                });
    }

    private void applyLinesAndUpdate(Context c, AppWidgetManager mgr, int id, RemoteViews rv, List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            rv.setTextViewText(R.id.row1, "Sem eventos próximos");
            rv.setTextViewText(R.id.row2, "");
            rv.setTextViewText(R.id.row3, "");
        } else {
            rv.setTextViewText(R.id.row1, lines.get(0));
            rv.setTextViewText(R.id.row2, lines.size() > 1 ? lines.get(1) : "");
            rv.setTextViewText(R.id.row3, lines.size() > 2 ? lines.get(2) : "");
        }
        mgr.updateAppWidget(id, rv);
    }

    private String formatDateShort(Context ctx, Date d) {
        java.text.DateFormat df = DateFormat.getDateFormat(ctx);
        java.text.DateFormat tf = DateFormat.getTimeFormat(ctx);
        return df.format(d) + " " + tf.format(d);
    }
}
