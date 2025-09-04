package com.example.alimentaacao.data.model;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

/**
 * Modelo do usuário do app.
 * Use Date para campos Timestamp do Firestore e @ServerTimestamp para preenchimento no servidor.
 */
public class User {
    public String id;
    public String name;
    public String email;
    public String type;
    public String photoUrl;

    @ServerTimestamp public Date createdAt;
    @ServerTimestamp public Date updatedAt;

    public User() {}

    public long createdAtMillis() { return createdAt != null ? createdAt.getTime() : 0L; }
    public long updatedAtMillis() { return updatedAt != null ? updatedAt.getTime() : 0L; }
}
