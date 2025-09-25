package com.example.alimentaacao.data.firebase;

import com.example.alimentaacao.data.model.Event;
import com.example.alimentaacao.data.model.Solicitation;
import com.example.alimentaacao.data.model.User;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
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

    /**
     * Cria ou mescla o documento do usuário logado em users/{uid}.
     * Usa merge e atualiza updatedAt. createdAt é gerido por @ServerTimestamp no model User
     * (quando setado pela primeira vez).
     */
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
        // Não setamos createdAt aqui; @ServerTimestamp no model cobre na criação.
        payload.put("updatedAt", com.google.firebase.firestore.FieldValue.serverTimestamp());

        return users().document(uid).set(payload, SetOptions.merge());
    }

    /** Adiciona uma solicitação, garantindo ownerUid e status padrão. */
    public Task<DocumentReference> addSolicitation(Solicitation s) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return Tasks.forException(new IllegalStateException("Usuário não autenticado"));

        if (s == null) s = new Solicitation();
        if (s.ownerUid == null) s.ownerUid = uid;
        if (s.status == null)   s.status   = "ABERTA";

        // createdAt/updatedAt serão preenchidos pelo @ServerTimestamp no model
        return solicitations().add(s);
    }

    /** Escuta em tempo real as solicitações da ONG (ownerUid = uid). */
    public ListenerRegistration listenSolicitationsByOwner(String uid, EventListener<QuerySnapshot> listener) {
        return solicitations()
                .whereEqualTo("ownerUid", uid)
                .addSnapshotListener(listener);
    }

    /** Conclui (ATENDIDA) uma solicitação. */
    public Task<Void> concludeSolicitation(String solicitationId) {
        Map<String, Object> up = new HashMap<>();
        up.put("status", "ATENDIDA");
        up.put("updatedAt", com.google.firebase.firestore.FieldValue.serverTimestamp());
        return solicitations().document(solicitationId).set(up, SetOptions.merge());
    }

    /** Atualiza status/atendidaPor da solicitação. */
    public Task<Void> updateSolicitationStatus(String id, String status, String uid) {
        Map<String, Object> up = new HashMap<>();
        if (status != null) up.put("status", status);
        if (uid != null)    up.put("atendidaPor", uid);
        up.put("updatedAt", com.google.firebase.firestore.FieldValue.serverTimestamp());
        return solicitations().document(id).set(up, SetOptions.merge());
    }

    /** Adiciona um evento (createdAt por @ServerTimestamp no model Event). */
    public Task<DocumentReference> addEvent(Event e) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return Tasks.forException(new IllegalStateException("Usuário não autenticado"));
        if (e == null) e = new Event();
        if (e.ownerUid == null) e.ownerUid = uid;
        return events().add(e);
    }

    /** Marca/desmarca interesse em um evento. */
    public Task<Void> toggleInteresse(String eventId, String uid, boolean add) {
        com.google.firebase.firestore.FieldValue val =
                add ? com.google.firebase.firestore.FieldValue.arrayUnion(uid)
                        : com.google.firebase.firestore.FieldValue.arrayRemove(uid);
        return events().document(eventId).update("interessados", val);
    }

    /** Confirma/desconfirma presença em um evento. */
    public Task<Void> toggleConfirmado(String eventId, String uid, boolean add) {
        com.google.firebase.firestore.FieldValue val =
                add ? com.google.firebase.firestore.FieldValue.arrayUnion(uid)
                        : com.google.firebase.firestore.FieldValue.arrayRemove(uid);
        return events().document(eventId).update("confirmados", val);
    }

    // Escuta eventos cujo ownerUid == uid (sem orderBy para não exigir índice)
    public com.google.firebase.firestore.ListenerRegistration listenEventsByOwner(
            String uid,
            com.google.firebase.firestore.EventListener<com.google.firebase.firestore.QuerySnapshot> listener
    ) {
        return events()
                .whereEqualTo("ownerUid", uid)
                .addSnapshotListener(listener);
    }

    // LISTENER: todos os eventos (para aba Mapa)
    public ListenerRegistration listenAllEvents(
            com.google.firebase.firestore.EventListener<com.google.firebase.firestore.QuerySnapshot> listener
    ) {
        return events().addSnapshotListener(listener); // filtra em memória depois
    }

    // ATUALIZAR campos do evento (merge)
    public com.google.android.gms.tasks.Task<Void> updateEvent(
            String eventId, String title, String desc, java.util.Date dateTime, Double lat, Double lng
    ) {
        java.util.Map<String, Object> up = new java.util.HashMap<>();
        if (title != null) up.put("title", title);
        if (desc  != null) up.put("description", desc);
        if (dateTime != null) up.put("dateTime", dateTime);
        if (lat != null) up.put("lat", lat);
        if (lng != null) up.put("lng", lng);
        up.put("updatedAt", com.google.firebase.firestore.FieldValue.serverTimestamp());
        return events().document(eventId).set(up, com.google.firebase.firestore.SetOptions.merge());
    }

    // EXCLUIR evento
    public com.google.android.gms.tasks.Task<Void> deleteEvent(String eventId) {
        return events().document(eventId).delete();
    }

    // Atualiza campos da solicitação (merge)
    public com.google.android.gms.tasks.Task<Void> updateSolicitation(
            String solicitationId,
            String title,
            java.util.List<com.example.alimentaacao.data.model.Solicitation.Item> items,
            com.google.firebase.firestore.GeoPoint geo,
            String status // opcional (pode ser null)
    ) {
        java.util.Map<String, Object> up = new java.util.HashMap<>();
        if (title != null) up.put("title", title);
        if (items != null) up.put("items", items);
        if (geo != null) up.put("geo", geo);
        if (status != null) up.put("status", status);
        up.put("updatedAt", com.google.firebase.firestore.FieldValue.serverTimestamp());
        return solicitations().document(solicitationId)
                .set(up, com.google.firebase.firestore.SetOptions.merge());
    }

    // Exclui solicitação
    public com.google.android.gms.tasks.Task<Void> deleteSolicitation(String solicitationId) {
        return solicitations().document(solicitationId).delete();
    }

    // Atualiza nome e/ou photoUrl do usuário autenticado (merge)
    public com.google.android.gms.tasks.Task<Void> updateUserProfile(String uid, String name, String photoUrl) {
        java.util.Map<String, Object> up = new java.util.HashMap<>();
        if (name != null)     up.put("name", name);
        if (photoUrl != null) up.put("photoUrl", photoUrl);
        up.put("updatedAt", com.google.firebase.firestore.FieldValue.serverTimestamp());
        return users().document(uid).set(up, com.google.firebase.firestore.SetOptions.merge());
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
        java.util.Map<String, Object> m = new java.util.HashMap<>();
        m.put("type", type);
        m.put("updatedAt", FieldValue.serverTimestamp());
        return users().document(uid).set(m, SetOptions.merge());
    }

    // Retorna o tipo do usuário ("ONG" ou "VOLUNTARIO")
    public com.google.android.gms.tasks.Task<String> getUserType(String uid) {
        if (uid == null) return com.google.android.gms.tasks.Tasks.forException(
                new IllegalStateException("uid nulo"));
        return getUserDoc(uid).continueWith(t -> {
            com.google.firebase.firestore.DocumentSnapshot ds = t.getResult();
            return (ds != null) ? ds.getString("type") : null;
        });
    }

    // Escuta todas as solicitações com status ABERTA (para voluntário)
    public com.google.firebase.firestore.ListenerRegistration listenSolicitationsOpen(
            com.google.firebase.firestore.EventListener<com.google.firebase.firestore.QuerySnapshot> listener
    ) {
        return solicitations()
                .whereEqualTo("status", "ABERTA")
                .addSnapshotListener(listener);
    }

    public Task<Void> upsertUserFromAuthRespectingProfile(com.google.firebase.auth.FirebaseUser fu) {
        if (fu == null) return Tasks.forException(new IllegalStateException("Sem usuário"));
        String uid = fu.getUid();
        return users().document(uid).get().continueWithTask(task -> {
            if (!task.isSuccessful()) return Tasks.forException(task.getException());
            DocumentSnapshot ds = task.getResult();

            java.util.Map<String, Object> payload = new java.util.HashMap<>();
            if (fu.getEmail() != null) payload.put("email", fu.getEmail());
            if (fu.getPhotoUrl() != null) payload.put("photoUrl", fu.getPhotoUrl().toString());

            boolean hasNameInDb = ds != null && ds.contains("name") &&
                    ds.getString("name") != null && !ds.getString("name").isEmpty();
            if (!hasNameInDb && fu.getDisplayName() != null && !fu.getDisplayName().isEmpty()) {
                payload.put("name", fu.getDisplayName());
            }

            payload.put("updatedAt", FieldValue.serverTimestamp());
            if (ds == null || !ds.exists()) {
                payload.put("createdAt", FieldValue.serverTimestamp());
            }
            // NÃO mexa em 'type' aqui!
            return users().document(uid).set(payload, SetOptions.merge());
        });
    }

}
