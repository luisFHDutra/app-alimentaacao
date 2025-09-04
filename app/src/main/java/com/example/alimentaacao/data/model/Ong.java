package com.example.alimentaacao.data.model;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

/** Documento de ONG. */
public class Ong {
    public String id;
    public String ownerUid;
    public String name;
    public Double lat;   // opcional
    public Double lng;   // opcional

    @ServerTimestamp public Date createdAt;
    @ServerTimestamp public Date updatedAt;

    public Ong() {}
}
