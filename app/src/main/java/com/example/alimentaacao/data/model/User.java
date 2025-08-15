package com.example.alimentaacao.data.model;

import com.google.firebase.firestore.GeoPoint;

public class User {
    public String uid;
    public String name;
    public String email;
    public String photoUrl;
    public String type; // "ONG" | "VOLUNTARIO"
    public GeoPoint geo;
    public long createdAt;

    public User() {}
    public User(String uid, String name, String email, String photoUrl, String type, GeoPoint geo, long createdAt) {
        this.uid = uid; this.name = name; this.email = email;
        this.photoUrl = photoUrl; this.type = type; this.geo = geo; this.createdAt = createdAt;
    }
}
