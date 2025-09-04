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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

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
}
