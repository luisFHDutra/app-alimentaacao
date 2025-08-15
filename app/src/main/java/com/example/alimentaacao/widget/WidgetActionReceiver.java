package com.example.alimentaacao.widget;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

public class WidgetActionReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (FeedWidgetFactory.ACTION_TOGGLE_CONFIRM.equals(intent.getAction())) {
            String eventId = intent.getStringExtra(FeedWidgetFactory.EXTRA_EVENT_ID);
            String uid = FirebaseAuth.getInstance().getUid();
            if (eventId == null || uid == null) return;
            FirebaseFirestore.getInstance().collection("events").document(eventId)
                    .update("confirmados", FieldValue.arrayUnion(uid));
        }
    }
}
