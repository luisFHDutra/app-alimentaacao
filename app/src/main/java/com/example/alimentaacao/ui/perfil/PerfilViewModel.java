package com.example.alimentaacao.ui.perfil;

import android.net.Uri;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.alimentaacao.data.firebase.FirestoreService;
import com.example.alimentaacao.data.firebase.StorageService;
import com.example.alimentaacao.data.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;

public class PerfilViewModel extends ViewModel {

    private final MutableLiveData<User> _user = new MutableLiveData<>(null);
    private final MutableLiveData<Boolean> _saving = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> _deleting = new MutableLiveData<>(false);
    private final MutableLiveData<String> _error = new MutableLiveData<>(null);
    private ListenerRegistration reg;

    public LiveData<User> user() { return _user; }
    public LiveData<Boolean> saving() { return _saving; }
    public LiveData<Boolean> deleting() { return _deleting; }
    public LiveData<String> error() { return _error; }

    private final FirestoreService fs = new FirestoreService();
    private final StorageService ss = new StorageService();

    public void start() {
        stop();
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) { _error.postValue("Usuário não autenticado."); return; }
        reg = fs.userRef(uid).addSnapshotListener((snap, err) -> {
            if (err != null) { _error.postValue(err.getMessage()); return; }
            if (snap != null && snap.exists()) {
                _user.postValue(snap.toObject(User.class));
            }
        });
    }

    public void stop() {
        if (reg != null) { reg.remove(); reg = null; }
    }

    /** Salva nome e opcionalmente faz upload de nova foto. */
    public void saveProfile(String newName, Uri newPhotoUri) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) { _error.postValue("Usuário não autenticado."); return; }
        _saving.postValue(true);

        if (newPhotoUri != null) {
            ss.uploadProfilePhoto(uid, newPhotoUri)
                    .continueWithTask(t -> fs.updateUserProfile(uid, newName, t.getResult().toString()))
                    .addOnCompleteListener(t -> _saving.postValue(false))
                    .addOnFailureListener(e -> _error.postValue(e.getMessage()));
        } else {
            fs.updateUserProfile(uid, newName, null)
                    .addOnCompleteListener(t -> _saving.postValue(false))
                    .addOnFailureListener(e -> _error.postValue(e.getMessage()));
        }
    }

    /** Exclui conta e dados básicos. */
    public void deleteAccount(java.util.function.Consumer<Boolean> onResult) {
        _deleting.postValue(true);
        new com.example.alimentaacao.data.repo.UserRepository()
                .deleteAccountAndData()
                .addOnSuccessListener(v -> { _deleting.postValue(false); onResult.accept(true); })
                .addOnFailureListener(e -> {
                    _deleting.postValue(false);
                    _error.postValue(e.getMessage());
                    onResult.accept(false);
                });
    }

    @Override protected void onCleared() { stop(); super.onCleared(); }

    public void saveProfileEx(String newName, android.net.Uri newPhotoUri,
                              String city, String uf, Double lat, Double lng) {
        String uid = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();
        if (uid == null) { _error.postValue("Usuário não autenticado."); return; }
        _saving.postValue(true);

        com.google.android.gms.tasks.Task<String> photoTask;
        if (newPhotoUri != null) {
            photoTask = new com.example.alimentaacao.data.firebase.StorageService()
                    .uploadProfilePhoto(uid, newPhotoUri)
                    .continueWith(t -> t.getResult().toString());
        } else {
            photoTask = com.google.android.gms.tasks.Tasks.forResult(null);
        }

        photoTask
                .continueWithTask(t -> new com.example.alimentaacao.data.firebase.FirestoreService()
                        .updateUserProfileEx(uid, newName, t.getResult(), city, uf, lat, lng))
                .continueWithTask(t -> new com.example.alimentaacao.data.firebase.FirestoreService()
                        .propagateOngProfileToSolicitations(uid, newName, city, uf, lat, lng))
                .addOnCompleteListener(t -> _saving.postValue(false))
                .addOnFailureListener(e -> _error.postValue(e.getMessage()));
    }

}
