package com.example.alimentaacao.ui.widget;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.example.alimentaacao.R;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

public class ProximosEventosWidgetService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new Factory(getApplicationContext());
    }

    static class Row {
        String id;
        String title;
        Date   dateTime;
        String ownerUid;
        String ownerName; // pode vir nulo
    }

    static class Factory implements RemoteViewsService.RemoteViewsFactory {
        private final Context ctx;
        private final ArrayList<Row> rows = new ArrayList<>();
        private final SimpleDateFormat df = new SimpleDateFormat("EEE, dd/MM HH:mm", new java.util.Locale("pt", "BR"));

        Factory(Context ctx) {
            this.ctx = ctx;
        }

        @Override public void onCreate() {
            // nada
        }

        @Override
        public void onDataSetChanged() {
            rows.clear();

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            FirebaseUser fu = FirebaseAuth.getInstance().getCurrentUser();
            Date now = new Date();

            try {
                String mode = "VOLUNTARIO";
                String uid = fu != null ? fu.getUid() : null;
                if (uid != null) {
                    DocumentSnapshot userDoc = Tasks.await(db.collection("users").document(uid).get());
                    String type = userDoc != null ? userDoc.getString("type") : null;
                    if ("ONG".equalsIgnoreCase(type)) {
                        mode = "ONG";
                    }
                }

                if ("ONG".equals(mode) && fu != null) {
                    // Sem orderBy para evitar índice composto; ordena em memória
                    QuerySnapshot qs = Tasks.await(
                            db.collection("events")
                                    .whereEqualTo("ownerUid", fu.getUid())
                                    .get()
                    );
                    for (DocumentSnapshot ds : qs) {
                        Date dt = ds.getDate("dateTime");
                        if (dt == null || dt.before(now)) continue; // apenas futuros
                        Row r = new Row();
                        r.id = ds.getId();
                        r.title = safe(ds.getString("title"));
                        r.dateTime = dt;
                        r.ownerUid = ds.getString("ownerUid");
                        r.ownerName = ds.getString("ownerName"); // se você passar a denormalizar
                        rows.add(r);
                    }
                    // ordena por data ascendente
                    Collections.sort(rows, Comparator.comparing(a -> a.dateTime));
                } else {
                    // "públicos"/abertos: futuros ordenados por data (sem índice composto)
                    QuerySnapshot qs = Tasks.await(
                            db.collection("events")
                                    .whereGreaterThan("dateTime", now)
                                    .orderBy("dateTime", Query.Direction.ASCENDING)
                                    .limit(20)
                                    .get()
                    );
                    for (DocumentSnapshot ds : qs) {
                        Row r = new Row();
                        r.id = ds.getId();
                        r.title = safe(ds.getString("title"));
                        r.dateTime = ds.getDate("dateTime");
                        r.ownerUid = ds.getString("ownerUid");
                        r.ownerName = ds.getString("ownerName"); // opcional
                        rows.add(r);
                    }
                }
            } catch (Exception ignored) {
                // Se der erro, mantém lista vazia (empty view aparece)
            }
        }

        @Override public void onDestroy() { rows.clear(); }

        @Override public int getCount() { return rows.size(); }

        @Override
        public RemoteViews getViewAt(int position) {
            if (position < 0 || position >= rows.size()) return null;
            Row r = rows.get(position);

            RemoteViews item = new RemoteViews(ctx.getPackageName(), R.layout.widget_item_evento);
            item.setTextViewText(R.id.tvItemTitle, nonEmpty(r.title, "(sem título)"));

            String sub = df.format(r.dateTime != null ? r.dateTime : new Date());
            if (!TextUtils.isEmpty(r.ownerName)) {
                sub = sub + " • " + r.ownerName;
            }
            item.setTextViewText(R.id.tvItemSubtitle, sub);

            // Prepara intenção de clique (usa PendingIntentTemplate definido no Provider)
            Intent fillIn = new Intent();
            fillIn.putExtra("open_tab", R.id.nav_eventos);
            fillIn.putExtra("event_id", r.id);
            item.setOnClickFillInIntent(R.id.tvItemTitle, fillIn);

            return item;
        }

        @Override public RemoteViews getLoadingView() { return null; }
        @Override public int getViewTypeCount() { return 1; }
        @Override public long getItemId(int position) { return position; }
        @Override public boolean hasStableIds() { return true; }

        private String safe(String s) { return s != null ? s : ""; }
        private String nonEmpty(String s, String def) { return (s != null && !s.isEmpty()) ? s : def; }
    }
}
