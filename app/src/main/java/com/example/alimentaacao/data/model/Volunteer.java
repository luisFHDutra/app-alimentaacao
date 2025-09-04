package com.example.alimentaacao.data.model;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

/** Perfil adicional do voluntário (coleção volunteers). */
public class Volunteer {
    public String ownerUid;
    public String name;

    @ServerTimestamp public Date createdAt;
    @ServerTimestamp public Date updatedAt;

    public Volunteer() {}
}
