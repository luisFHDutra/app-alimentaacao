package com.example.alimentaacao.widget;

import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;
import com.example.alimentaacao.R;
import com.example.alimentaacao.data.model.Event;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.util.ArrayList;
import java.util.List;

public class FeedWidgetFactory implements RemoteViewsService.RemoteViewsFactory {
    private final Context ctx;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final List<Event> items = new ArrayList<>();

    public static final String ACTION_TOGGLE_CONFIRM = "com.example.alimentaacao.widget.TOGGLE_CONFIRM";
    public static final String EXTRA_EVENT_ID = "event_id";

    public FeedWidgetFactory(Context c) { this.ctx = c; }

    @Override public void onCreate() { load(); }
    @Override public void onDataSetChanged() { load(); }
    @Override public void onDestroy() { items.clear(); }
    @Override public int getCount() { return items.size(); }

    @Override public RemoteViews getViewAt(int position) {
        if (position < 0 || position >= items.size()) return null;
        Event e = items.get(position);
        RemoteViews row = new RemoteViews(ctx.getPackageName(), R.layout.widget_feed_item);
        row.setTextViewText(R.id.tvPrimary, e.title == null ? "Evento" : e.title);

        Intent i = new Intent(ctx, WidgetActionReceiver.class);
        i.setAction(ACTION_TOGGLE_CONFIRM);
        i.putExtra(EXTRA_EVENT_ID, e.id);
        row.setOnClickPendingIntent(
                R.id.btnAction,
                android.app.PendingIntent.getBroadcast(
                        ctx, position, i,
                        android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE
                )
        );
        return row;
    }

    @Override public RemoteViews getLoadingView() { return null; }
    @Override public int getViewTypeCount() { return 1; }
    @Override public long getItemId(int position) { return position; }
    @Override public boolean hasStableIds() { return false; }

    private void load() {
        items.clear();
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;
        db.collection("events")
                .whereArrayContains("interessados", uid)
                .orderBy("dateTime", Query.Direction.ASCENDING)
                .limit(10)
                .get()
                .addOnSuccessListener(qs -> {
                    for (DocumentSnapshot d: qs) {
                        Event e = d.toObject(Event.class);
                        if (e != null) { e.id = d.getId(); items.add(e); }
                    }
                });
    }
}
