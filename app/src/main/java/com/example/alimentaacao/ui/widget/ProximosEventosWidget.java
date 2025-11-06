package com.example.alimentaacao.ui.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.RemoteViews;

import com.example.alimentaacao.R;
import com.example.alimentaacao.data.model.Event;
import com.example.alimentaacao.ui.MainActivity;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ProximosEventosWidget extends AppWidgetProvider {

    public static final String ACTION_REFRESH = "com.example.alimentaacao.ACTION_REFRESH_WIDGET";

    private static final String PREFS = "widget_events_prefs";
    private static final String KEY_LAST_LAT = "last_lat";
    private static final String KEY_LAST_LNG = "last_lng";

    @Override
    public void onUpdate(Context context, AppWidgetManager mgr, int[] appWidgetIds) {
        for (int id : appWidgetIds) {
            RemoteViews rv = buildSkeleton(context);
            attachFallbackClicks(context, rv, id);
            mgr.updateAppWidget(id, rv);
            refreshFromFirestore(context, mgr, id);
        }
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

    private void refreshFromFirestore(Context ctx, AppWidgetManager mgr, int appWidgetId) {
        FirebaseFirestore.getInstance()
                .collection("events")
                .orderBy("dateTime", Query.Direction.ASCENDING)
                .limit(20)
                .get()
                .addOnSuccessListener(qs -> {
                    Date now = new Date();
                    List<Event> futuros = new ArrayList<>();
                    if (qs != null) {
                        for (DocumentSnapshot ds : qs.getDocuments()) {
                            try {
                                Event e = ds.toObject(Event.class);
                                if (e == null) continue;
                                e.id = ds.getId();
                                if (e.dateTime == null || !e.dateTime.before(now)) {
                                    futuros.add(e);
                                }
                                if (futuros.size() >= 3) break;
                            } catch (Exception ignored) {}
                        }
                    }

                    RemoteViews rv = new RemoteViews(ctx.getPackageName(), R.layout.widget_proximos_eventos);

                    // zera
                    rv.setTextViewText(R.id.tvLine1, "");
                    rv.setTextViewText(R.id.tvLine2, "");
                    rv.setTextViewText(R.id.tvLine3, "");

                    int[] lineIds = new int[]{R.id.tvLine1, R.id.tvLine2, R.id.tvLine3};

                    for (int i = 0; i < Math.min(futuros.size(), 3); i++) {
                        Event e = futuros.get(i);
                        String titulo = e.title != null ? e.title : "(sem título)";
                        String horario = (e.dateTime != null)
                                ? java.text.DateFormat.getDateTimeInstance().format(e.dateTime)
                                : "-";
                        String linha = horario + " — " + titulo;

                        rv.setTextViewText(lineIds[i], linha);

                        if (e.lat != null && e.lng != null) {
                            int req = appWidgetId * 10 + i;
                            PendingIntent pi = makeOpenMapPI(ctx, req, e.lat, e.lng, titulo);
                            rv.setOnClickPendingIntent(lineIds[i], pi);
                            if (i == 0) saveLastLatLng(ctx, e.lat, e.lng);
                        } else {
                            rv.setOnClickPendingIntent(lineIds[i], null);
                        }
                    }

                    attachHeaderClicks(ctx, rv, appWidgetId);
                    mgr.updateAppWidget(appWidgetId, rv);
                })
                .addOnFailureListener(err -> {
                    // mantém layout básico; usuário pode tocar refresh
                });
    }

    private RemoteViews buildSkeleton(Context context) {
        RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget_proximos_eventos);
        rv.setTextViewText(R.id.tvLine1, "Carregando…");
        rv.setTextViewText(R.id.tvLine2, "");
        rv.setTextViewText(R.id.tvLine3, "");
        attachHeaderClicks(context, rv, 1000);
        return rv;
    }

    private void attachHeaderClicks(Context context, RemoteViews rv, int appWidgetId) {
        // Título abre a aba de eventos (lista completa)
        Intent openEvents = new Intent(context, MainActivity.class)
                .putExtra("open_tab", R.id.nav_eventos)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent piOpenEvents = PendingIntent.getActivity(
                context, appWidgetId + 999, openEvents,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        rv.setOnClickPendingIntent(R.id.tvTitle, piOpenEvents);

        // Botão de refresh
        Intent refresh = new Intent(context, ProximosEventosWidget.class).setAction(ACTION_REFRESH);
        PendingIntent piRefresh = PendingIntent.getBroadcast(
                context, appWidgetId, refresh,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        rv.setOnClickPendingIntent(R.id.btnRefresh, piRefresh);
    }

    private void attachFallbackClicks(Context ctx, RemoteViews rv, int appWidgetId) {
        double[] last = getLastLatLng(ctx);
        if (!Double.isNaN(last[0]) && !Double.isNaN(last[1])) {
            PendingIntent pi = makeOpenMapPI(ctx, appWidgetId, last[0], last[1], "Evento");
            rv.setOnClickPendingIntent(R.id.tvLine1, pi);
            rv.setOnClickPendingIntent(R.id.tvLine2, pi);
            rv.setOnClickPendingIntent(R.id.tvLine3, pi);
        } else {
            rv.setOnClickPendingIntent(R.id.tvLine1, null);
            rv.setOnClickPendingIntent(R.id.tvLine2, null);
            rv.setOnClickPendingIntent(R.id.tvLine3, null);
        }
        attachHeaderClicks(ctx, rv, appWidgetId);
    }

    private PendingIntent makeOpenMapPI(Context ctx, int requestCode, double lat, double lng, String title) {
        Intent open = new Intent(ctx, MainActivity.class)
                .putExtra("open_tab", R.id.nav_mapa)
                .putExtra("focus_lat", lat)
                .putExtra("focus_lng", lng)
                .putExtra("focus_title", title)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return PendingIntent.getActivity(
                ctx, requestCode, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private void saveLastLatLng(Context ctx, double lat, double lng) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        sp.edit()
                .putLong(KEY_LAST_LAT, Double.doubleToRawLongBits(lat))
                .putLong(KEY_LAST_LNG, Double.doubleToRawLongBits(lng))
                .apply();
    }

    private double[] getLastLatLng(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        double lat = Double.longBitsToDouble(sp.getLong(KEY_LAST_LAT, Double.doubleToLongBits(Double.NaN)));
        double lng = Double.longBitsToDouble(sp.getLong(KEY_LAST_LNG, Double.doubleToLongBits(Double.NaN)));
        return new double[]{lat, lng};
    }
}