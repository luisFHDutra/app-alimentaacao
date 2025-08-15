package com.example.alimentaacao.data.model;

import com.google.firebase.firestore.GeoPoint;

public class Volunteer {
    public String id;
    public String name;
    public GeoPoint geo;
    public double radiusKm;

    public Volunteer() {}
}
