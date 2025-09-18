package com.example.alimentaacao.data.firebase;

import android.net.Uri;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

/** Upload/remoção de foto de perfil em users/{uid}/profile.jpg */
public class StorageService {
    private final StorageReference root = FirebaseStorage.getInstance().getReference();

    private StorageReference profileRef(String uid) {
        return root.child("users").child(uid).child("profile.jpg");
    }

    /** Faz upload da foto e retorna Task com a URL de download. */
    public Task<Uri> uploadProfilePhoto(String uid, Uri localUri) {
        if (uid == null || localUri == null) {
            return Tasks.forException(new IllegalArgumentException("uid/localUri inválidos"));
        }
        return profileRef(uid).putFile(localUri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) throw task.getException();
                    return profileRef(uid).getDownloadUrl();
                });
    }

    public Task<Void> deleteProfilePhoto(String uid) {
        if (uid == null) return Tasks.forResult(null);
        return profileRef(uid).delete();
    }
}
