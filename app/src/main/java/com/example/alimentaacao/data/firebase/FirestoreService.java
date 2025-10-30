package com.example.alimentaacao.data.firebase;

import com.example.alimentaacao.data.model.Event;
import com.example.alimentaacao.data.model.Solicitation;
import com.example.alimentaacao.data.model.User;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

public class FirestoreService {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public CollectionReference users()         { return db.collection("users"); }
    public CollectionReference ongs()          { return db.collection("ongs"); }
    public CollectionReference volunteers()    { return db.collection("volunteers"); }
    public CollectionReference events()        { return db.collection("events"); }
    public CollectionReference solicitations() { return db.collection("solicitations"); }

    public CollectionReference voluntarios()   { return volunteers(); }
    public CollectionReference eventos()       { return events(); }
    public CollectionReference solicitacoes()  { return solicitations(); }

    public DocumentReference userRef(String uid) {
        return users().document(uid);
    }

    /** Cria/mescla users/{uid} com updatedAt. */
    public Task<Void> createOrMergeUser(User u) {
        String uid = (u != null && u.id != null) ? u.id : FirebaseAuth.getInstance().getUid();
        if (uid == null) return Tasks.forException(new IllegalStateException("Usuário não autenticado"));

        Map<String, Object> payload = new HashMap<>();
        if (u != null) {
            if (u.name != null)     payload.put("name", u.name);
            if (u.email != null)    payload.put("email", u.email);
            if (u.photoUrl != null) payload.put("photoUrl", u.photoUrl);
            if (u.type != null)     payload.put("type", u.type);
        }
        payload.put("updatedAt", FieldValue.serverTimestamp());
        return users().document(uid).set(payload, SetOptions.merge());
    }

    /** Adiciona solicitação já com dados denormalizados da ONG. */
    public Task<DocumentReference> addSolicitation(Solicitation s) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return Tasks.forException(new IllegalStateException("Usuário não autenticado"));

        // Normaliza sem capturar 's' diretamente
        Solicitation tmp = (s != null) ? s : new Solicitation();
        if (tmp.ownerUid == null) tmp.ownerUid = uid;
        if (tmp.status == null)   tmp.status   = "ABERTA";

        // Capturas finais para a lambda
        final String fUid = uid;
        final String fTitle = tmp.title;
        final java.util.List<Solicitation.Item> fItems = tmp.items;
        final com.google.firebase.firestore.GeoPoint fGeo = tmp.geo;
        final String fStatus = tmp.status;

        return users().document(fUid).get().continueWithTask(t -> {
            if (!t.isSuccessful()) throw t.getException();
            DocumentSnapshot u = t.getResult();

            Map<String, Object> payload = new HashMap<>();
            payload.put("ownerUid", fUid);
            if (fTitle != null) payload.put("title", fTitle);
            if (fItems != null) payload.put("items", fItems);
            if (fGeo != null)   payload.put("geo", fGeo);
            payload.put("status", fStatus);

            // denormalização da ONG
            payload.put("ownerName", (u != null) ? u.getString("name") : null);
            payload.put("ownerCity", (u != null) ? u.getString("city") : null);
            payload.put("ownerUf",   (u != null) ? u.getString("uf")   : null);
            Double lat = (u != null) ? u.getDouble("lat") : null;
            Double lng = (u != null) ? u.getDouble("lng") : null;
            if (lat != null && lng != null) {
                payload.put("ownerGeo", new com.google.firebase.firestore.GeoPoint(lat, lng));
            }

            payload.put("createdAt", FieldValue.serverTimestamp());
            payload.put("updatedAt", FieldValue.serverTimestamp());

            return solicitations().add(payload);
        });
    }

    /** Escuta em tempo real as solicitações de uma ONG (ownerUid=uid). */
    public ListenerRegistration listenSolicitationsByOwner(String uid, EventListener<QuerySnapshot> listener) {
        return solicitations().whereEqualTo("ownerUid", uid).addSnapshotListener(listener);
    }

    /** Conclui uma solicitação. */
    public Task<Void> concludeSolicitation(String solicitationId) {
        Map<String, Object> up = new HashMap<>();
        up.put("status", "ATENDIDA");
        up.put("updatedAt", FieldValue.serverTimestamp());
        return solicitations().document(solicitationId).set(up, SetOptions.merge());
    }

    /** Atualiza status/atendidaPor. */
    public Task<Void> updateSolicitationStatus(String id, String status, String uid) {
        Map<String, Object> up = new HashMap<>();
        if (status != null) up.put("status", status);
        if (uid != null)    up.put("atendidaPor", uid);
        up.put("updatedAt", FieldValue.serverTimestamp());
        return solicitations().document(id).set(up, SetOptions.merge());
    }

    /** Adiciona evento. */
    public Task<DocumentReference> addEvent(Event e) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return Tasks.forException(new IllegalStateException("Usuário não autenticado"));
        if (e == null) e = new Event();
        if (e.ownerUid == null) e.ownerUid = uid;
        return events().add(e);
    }

    /** Marca/desmarca interesse. */
    public Task<Void> toggleInteresse(String eventId, String uid, boolean add) {
        com.google.firebase.firestore.FieldValue val =
                add ? com.google.firebase.firestore.FieldValue.arrayUnion(uid)
                        : com.google.firebase.firestore.FieldValue.arrayRemove(uid);
        return events().document(eventId).update("interessados", val);
    }

    /** Confirma/desconfirma presença. */
    public Task<Void> toggleConfirmado(String eventId, String uid, boolean add) {
        com.google.firebase.firestore.FieldValue val =
                add ? com.google.firebase.firestore.FieldValue.arrayUnion(uid)
                        : com.google.firebase.firestore.FieldValue.arrayRemove(uid);
        return events().document(eventId).update("confirmados", val);
    }

    /** Escuta eventos do dono. */
    public ListenerRegistration listenEventsByOwner(
            String uid,
            EventListener<QuerySnapshot> listener
    ) {
        return events().whereEqualTo("ownerUid", uid).addSnapshotListener(listener);
    }

    /** Escuta todos os eventos (para o mapa). */
    public ListenerRegistration listenAllEvents(EventListener<QuerySnapshot> listener) {
        return events().addSnapshotListener(listener);
    }

    /** Atualiza evento (merge). */
    public Task<Void> updateEvent(
            String eventId, String title, String desc, java.util.Date dateTime, Double lat, Double lng
    ) {
        Map<String, Object> up = new HashMap<>();
        if (title != null)   up.put("title", title);
        if (desc  != null)   up.put("description", desc);
        if (dateTime != null) up.put("dateTime", dateTime);
        if (lat != null)     up.put("lat", lat);
        if (lng != null)     up.put("lng", lng);
        up.put("updatedAt", FieldValue.serverTimestamp());
        return events().document(eventId).set(up, SetOptions.merge());
    }

    /** Exclui evento. */
    public Task<Void> deleteEvent(String eventId) {
        return events().document(eventId).delete();
    }

    /** Atualiza solicitação (merge). */
    public Task<Void> updateSolicitation(
            String solicitationId,
            String title,
            java.util.List<Solicitation.Item> items,
            com.google.firebase.firestore.GeoPoint geo,
            String status
    ) {
        Map<String, Object> up = new HashMap<>();
        if (title != null) up.put("title", title);
        if (items != null) up.put("items", items);
        if (geo != null)   up.put("geo", geo);
        if (status != null) up.put("status", status);
        up.put("updatedAt", FieldValue.serverTimestamp());
        return solicitations().document(solicitationId).set(up, SetOptions.merge());
    }

    /** Exclui solicitação. */
    public Task<Void> deleteSolicitation(String solicitationId) {
        return solicitations().document(solicitationId).delete();
    }

    /** Atualiza nome/foto básicos do perfil. */
    public Task<Void> updateUserProfile(String uid, String name, String photoUrl) {
        Map<String, Object> up = new HashMap<>();
        if (name != null)     up.put("name", name);
        if (photoUrl != null) up.put("photoUrl", photoUrl);
        up.put("updatedAt", FieldValue.serverTimestamp());
        return users().document(uid).set(up, SetOptions.merge());
    }

    public Task<DocumentSnapshot> getUserDoc(String uid) {
        return users().document(uid).get();
    }

    public Task<Boolean> userHasType(String uid) {
        return getUserDoc(uid).continueWith(task -> {
            if (!task.isSuccessful()) throw task.getException();
            DocumentSnapshot ds = task.getResult();
            String t = (ds != null) ? ds.getString("type") : null;
            return ds != null && ds.exists() && t != null && !t.isEmpty();
        });
    }

    public Task<Void> setUserType(String uid, String type) {
        Map<String, Object> m = new HashMap<>();
        m.put("type", type);
        m.put("updatedAt", FieldValue.serverTimestamp());
        return users().document(uid).set(m, SetOptions.merge());
    }

    /** Retorna tipo do usuário. */
    public Task<String> getUserType(String uid) {
        if (uid == null) return Tasks.forResult(null);
        return users().document(uid).get().continueWith(t -> {
            if (!t.isSuccessful()) throw t.getException();
            DocumentSnapshot ds = t.getResult();
            return (ds != null) ? ds.getString("type") : null;
        });
    }

    /** Lista solicitações abertas (sem índice composto). */
    public ListenerRegistration listenSolicitationsOpen(EventListener<QuerySnapshot> listener) {
        return solicitations()
                .whereEqualTo("status", "ABERTA")
                // .orderBy("createdAt", Query.Direction.DESCENDING) // exigiria índice
                .addSnapshotListener(listener);
    }

    /** Upsert respeitando dados existentes do perfil. */
    public Task<Void> upsertUserFromAuthRespectingProfile(com.google.firebase.auth.FirebaseUser fu) {
        if (fu == null) return Tasks.forException(new IllegalStateException("Sem usuário"));
        String uid = fu.getUid();
        return users().document(uid).get().continueWithTask(task -> {
            if (!task.isSuccessful()) return Tasks.forException(task.getException());
            DocumentSnapshot ds = task.getResult();

            Map<String, Object> payload = new HashMap<>();
            if (fu.getEmail() != null) payload.put("email", fu.getEmail());
            if (fu.getPhotoUrl() != null) payload.put("photoUrl", fu.getPhotoUrl().toString());

            boolean hasNameInDb = ds != null && ds.contains("name") &&
                    ds.getString("name") != null && !ds.getString("name").isEmpty();
            if (!hasNameInDb && fu.getDisplayName() != null && !fu.getDisplayName().isEmpty()) {
                payload.put("name", fu.getDisplayName());
            }

            payload.put("updatedAt", FieldValue.serverTimestamp());
            if (ds == null || !ds.exists()) payload.put("createdAt", FieldValue.serverTimestamp());
            return users().document(uid).set(payload, SetOptions.merge());
        });
    }

    /** Atualiza perfil com cidade/UF/lat/lng. */
    public Task<Void> updateUserProfileEx(
            String uid, String name, String photoUrl,
            String city, String uf, Double lat, Double lng
    ) {
        Map<String, Object> up = new HashMap<>();
        if (name != null)     up.put("name", name);
        if (photoUrl != null) up.put("photoUrl", photoUrl);
        if (city != null)     up.put("city", city);
        if (uf != null)       up.put("uf", uf);
        if (lat != null)      up.put("lat", lat);
        if (lng != null)      up.put("lng", lng);
        up.put("updatedAt", FieldValue.serverTimestamp());
        return users().document(uid).set(up, SetOptions.merge());
    }

    /** Propaga dados da ONG para solicitações ABERTAS do mesmo ownerUid. */
    public Task<Void> propagateOngProfileToSolicitations(
            String ownerUid, String name, String city, String uf, Double lat, Double lng
    ) {
        com.google.android.gms.tasks.TaskCompletionSource<Void> tcs = new com.google.android.gms.tasks.TaskCompletionSource<>();
        solicitations()
                .whereEqualTo("ownerUid", ownerUid)
                .whereEqualTo("status", "ABERTA")
                .get()
                .addOnSuccessListener(qs -> {
                    FirebaseFirestore db = FirebaseFirestore.getInstance();
                    com.google.firebase.firestore.WriteBatch b = db.batch();
                    for (com.google.firebase.firestore.DocumentSnapshot ds : qs) {
                        Map<String,Object> up = new HashMap<>();
                        if (name != null) up.put("ownerName", name);
                        up.put("ownerCity", city);
                        up.put("ownerUf", uf);
                        if (lat != null && lng != null) {
                            up.put("ownerGeo", new com.google.firebase.firestore.GeoPoint(lat, lng));
                        }
                        up.put("updatedAt", FieldValue.serverTimestamp());
                        b.set(ds.getReference(), up, SetOptions.merge());
                    }
                    b.commit().addOnSuccessListener(v -> tcs.setResult(null))
                            .addOnFailureListener(tcs::setException);
                })
                .addOnFailureListener(tcs::setException);
        return tcs.getTask();
    }

    /** Encerra evento. */
    public Task<Void> closeEvent(String eventId) {
        Map<String, Object> up = new HashMap<>();
        up.put("status", "ENCERRADO");
        up.put("updatedAt", FieldValue.serverTimestamp());
        return events().document(eventId).set(up, SetOptions.merge());
    }

    /** Deleta em lote conteúdos do dono. */
    public Task<Void> deleteAllOwnedContent(String ownerUid) {
        com.google.android.gms.tasks.TaskCompletionSource<Void> tcs = new com.google.android.gms.tasks.TaskCompletionSource<>();

        java.util.List<String> evIds = new java.util.ArrayList<>();
        java.util.List<String> soIds = new java.util.ArrayList<>();

        events().whereEqualTo("ownerUid", ownerUid).get()
                .continueWithTask(te -> {
                    if (te.isSuccessful() && te.getResult() != null) {
                        for (com.google.firebase.firestore.DocumentSnapshot ds : te.getResult()) {
                            evIds.add(ds.getId());
                        }
                    }
                    return solicitations().whereEqualTo("ownerUid", ownerUid).get();
                })
                .continueWithTask(ts -> {
                    if (ts.isSuccessful() && ts.getResult() != null) {
                        for (com.google.firebase.firestore.DocumentSnapshot ds : ts.getResult()) {
                            soIds.add(ds.getId());
                        }
                    }
                    java.util.List<Task<Void>> batchTasks = new java.util.ArrayList<>();
                    for (int i = 0; i < evIds.size(); i += 400) {
                        com.google.firebase.firestore.WriteBatch b = FirebaseFirestore.getInstance().batch();
                        for (int j = i; j < Math.min(i + 400, evIds.size()); j++) {
                            b.delete(events().document(evIds.get(j)));
                        }
                        batchTasks.add(b.commit());
                    }
                    for (int i = 0; i < soIds.size(); i += 400) {
                        com.google.firebase.firestore.WriteBatch b = FirebaseFirestore.getInstance().batch();
                        for (int j = i; j < Math.min(i + 400, soIds.size()); j++) {
                            b.delete(solicitations().document(soIds.get(j)));
                        }
                        batchTasks.add(b.commit());
                    }
                    return Tasks.whenAll(batchTasks);
                })
                .addOnSuccessListener(v -> tcs.setResult(null))
                .addOnFailureListener(tcs::setException);

        return tcs.getTask();
    }
}
