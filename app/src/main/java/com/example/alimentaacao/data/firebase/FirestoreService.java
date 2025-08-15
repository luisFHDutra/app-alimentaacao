package com.example.alimentaacao.data.firebase;

import com.example.alimentaacao.data.model.*;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.*;
import java.util.HashMap;
import java.util.Map;

public class FirestoreService {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public Task<Void> createOrMergeUser(User u) {
        return db.collection("users").document(u.uid).set(u, SetOptions.merge());
    }

    public DocumentReference userRef(String uid) {
        return db.collection("users").document(uid);
    }

    public CollectionReference ongs() { return db.collection("ongs"); }
    public CollectionReference voluntarios() { return db.collection("voluntarios"); }
    public CollectionReference eventos() { return db.collection("eventos"); }
    public CollectionReference solicitacoes() { return db.collection("solicitacoes"); }

    public Task<DocumentReference> addSolicitation(Solicitation s) {
        s.createdAt = System.currentTimeMillis();
        return solicitacoes().add(s);
    }

    public Task<Void> updateSolicitationStatus(String id, String status, String uid) {
        Map<String, Object> up = new HashMap<>();
        up.put("status", status);
        if (uid != null) up.put("atendidaPor", uid);
        return solicitacoes().document(id).set(up, SetOptions.merge());
    }

    public Task<DocumentReference> addEvent(Event e) {
        e.createdAt = System.currentTimeMillis();
        return eventos().add(e);
    }

    public Task<Void> toggleInteresse(String eventId, String uid, boolean add) {
        FieldValue val = add ? FieldValue.arrayUnion(uid) : FieldValue.arrayRemove(uid);
        return eventos().document(eventId).update("interessados", val);
    }

    public Task<Void> toggleConfirmado(String eventId, String uid, boolean add) {
        FieldValue val = add ? FieldValue.arrayUnion(uid) : FieldValue.arrayRemove(uid);
        return eventos().document(eventId).update("confirmados", val);
    }
}
