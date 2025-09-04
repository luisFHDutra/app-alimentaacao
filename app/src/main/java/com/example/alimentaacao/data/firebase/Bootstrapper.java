package com.example.alimentaacao.data.firebase;

import android.net.Uri;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

/**
 * Responsável por:
 *  - garantir o documento do usuário (users/{uid})
 *  - "semear" coleções base (ongs, events, solicitations, volunteers)
 *  - definir/atualizar o tipo do usuário (ONG | VOLUNTARIO)
 *  - criar o documento inicial por tipo (ex.: ongs/{doc} do dono)
 */
public class Bootstrapper {

    private static final String TAG = "Bootstrapper";

    private static FirebaseAuth auth() { return FirebaseAuth.getInstance(); }
    private static FirebaseFirestore db() { return FirebaseFirestore.getInstance(); }

    /** Garante a existência de users/{uid} com dados básicos do FirebaseUser. */
    public static Task<Void> ensureUserDocument() {
        FirebaseUser fu = auth().getCurrentUser();
        if (fu == null) return Tasks.forResult(null);

        String uid = fu.getUid();
        String name = fu.getDisplayName();
        String email = fu.getEmail();
        Uri photo = fu.getPhotoUrl();

        return db().collection("users").document(uid).get()
                .continueWithTask(t -> {
                    boolean creating = !t.isSuccessful() || t.getResult() == null || !t.getResult().exists();

                    Map<String, Object> payload = new HashMap<>();
                    if (name != null)  payload.put("name", name);
                    if (email != null) payload.put("email", email);
                    if (photo != null) payload.put("photoUrl", photo.toString());
                    if (creating) payload.put("createdAt", FieldValue.serverTimestamp());
                    payload.put("updatedAt", FieldValue.serverTimestamp());

                    return db().collection("users").document(uid)
                            .set(payload, SetOptions.merge());
                });
    }


    /** Lê o campo type do users/{uid}. Retorna null se não existir/sem tipo. */
    public static Task<String> fetchUserType() {
        TaskCompletionSource<String> tcs = new TaskCompletionSource<>();
        FirebaseUser fu = auth().getCurrentUser();
        if (fu == null) {
            tcs.setResult(null);
            return tcs.getTask();
        }
        db().collection("users").document(fu.getUid()).get()
                .addOnSuccessListener(snap -> {
                    if (snap != null && snap.exists()) {
                        Object t = snap.get("type");
                        tcs.setResult(t == null ? null : String.valueOf(t));
                    } else {
                        tcs.setResult(null);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "fetchUserType: ", e);
                    tcs.setResult(null);
                });
        return tcs.getTask();
    }

    /** Define/atualiza o type do usuário atual (ONG | VOLUNTARIO). */
    public static Task<Void> setUserType(String type) {
        FirebaseUser fu = auth().getCurrentUser();
        if (fu == null) return Tasks.forResult(null);
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", type);
        payload.put("updatedAt", FieldValue.serverTimestamp());
        return db().collection("users").document(fu.getUid())
                .set(payload, SetOptions.merge());
    }

    /** Cria doc inicial conforme o tipo (somente se ainda não existir nenhum do dono). */
    public static Task<Void> ensureTypeDocuments(String type) {
        FirebaseUser fu = auth().getCurrentUser();
        if (fu == null) return Tasks.forResult(null);
        String uid = fu.getUid();

        if ("ONG".equalsIgnoreCase(type)) {
            // cria um doc em "ongs" do ownerUid (se não houver)
            return db().collection("ongs")
                    .whereEqualTo("ownerUid", uid)
                    .limit(1)
                    .get()
                    .continueWithTask(t -> {
                        if (t.isSuccessful() && t.getResult() != null && !t.getResult().isEmpty()) {
                            return Tasks.forResult(null); // já existe
                        }
                        Map<String, Object> ong = new HashMap<>();
                        ong.put("ownerUid", uid);
                        ong.put("name", fu.getDisplayName() != null ? fu.getDisplayName() : "Minha ONG");
                        ong.put("createdAt", FieldValue.serverTimestamp());
                        return db().collection("ongs").add(ong).continueWith(task -> null);
                    });
        } else {
            // VOLUNTARIO: cria/atualiza doc em "volunteers/{uid}"
            Map<String, Object> vol = new HashMap<>();
            vol.put("ownerUid", uid);
            vol.put("name", fu.getDisplayName());
            vol.put("createdAt", FieldValue.serverTimestamp());
            return db().collection("volunteers").document(uid)
                    .set(vol, SetOptions.merge());
        }
    }

    /** "Semeia" coleções base com um doc _seed se estiverem vazias (evita listas nulas na 1ª execução). */
    public static Task<Void> ensureBaseCollections() {
        Task<Void> t1 = seedIfEmpty("ongs");
        Task<Void> t2 = seedIfEmpty("events");
        Task<Void> t3 = seedIfEmpty("solicitations");
        Task<Void> t4 = seedIfEmpty("volunteers");
        return Tasks.whenAll(t1, t2, t3, t4);
    }

    private static Task<Void> seedIfEmpty(String col) {
        return db().collection(col).limit(1).get()
                .continueWithTask(t -> {
                    if (t.isSuccessful() && t.getResult() != null && !t.getResult().isEmpty()) {
                        return Tasks.forResult(null); // já tem algo
                    }
                    Map<String, Object> seed = new HashMap<>();
                    seed.put("_seed", true);
                    seed.put("createdAt", FieldValue.serverTimestamp());
                    return db().collection(col).document("_seed")
                            .set(seed, SetOptions.merge())
                            .continueWith(task -> null);
                });
    }
}
