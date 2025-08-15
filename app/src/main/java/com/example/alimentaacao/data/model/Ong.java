package com.example.alimentaacao.data.model;

import com.google.firebase.firestore.GeoPoint;

public class Ong {
    public String id;
    public String ownerUid;
    public String name;
    public String description;
    public GeoPoint geo;
    public String address;

    public Ong() {}
}
