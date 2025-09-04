package com.example.alimentaacao.data.repo;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.alimentaacao.data.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

/**
 * Repositório do usuário logado.
 * Expõe LiveData<User> e permite leitura única (loadMe) e escuta em tempo real (listenMe).
 */
public class UserRepository {

    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final MutableLiveData<User> meLive = new MutableLiveData<>();
    private ListenerRegistration meRegistration;

    private DocumentReference meRef() {
        String uid = auth.getUid();
        if (uid == null) return null;
        return db.collection("users").document(uid);
    }

    public LiveData<User> me() {
        return meLive;
    }

    /** Leitura única do perfil (útil para ViewModels que só precisam pegar o snapshot atual). */
    public void loadMe() {
        DocumentReference ref = meRef();
        if (ref == null) {
            meLive.postValue(null);
            return;
        }
        ref.get()
                .addOnSuccessListener(this::applySnapshot)
                .addOnFailureListener(e -> meLive.postValue(null));
    }

    /** Inicia listener em tempo real para o documento do usuário. */
    public void listenMe() {
        if (meRegistration != null) return;
        DocumentReference ref = meRef();
        if (ref == null) return;
        meRegistration = ref.addSnapshotListener((snap, err) -> {
            if (err != null) {
                meLive.postValue(null);
                return;
            }
            applySnapshot(snap);
        });
    }

    /** Cancela a escuta em tempo real (chame em onCleared/onDestroy). */
    public void removeMeListener() {
        if (meRegistration != null) {
            meRegistration.remove();
            meRegistration = null;
        }
    }

    private void applySnapshot(DocumentSnapshot snap) {
        if (snap == null || !snap.exists()) {
            meLive.postValue(null);
            return;
        }
        User u = snap.toObject(User.class);
        if (u != null) {
            u.id = snap.getId();
        }
        meLive.postValue(u);
    }
}
