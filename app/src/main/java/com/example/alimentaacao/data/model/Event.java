package com.example.alimentaacao.data.model;

import com.google.firebase.firestore.GeoPoint;
import java.util.List;

public class Event {
    public String id;
    public String ongId;
    public String title;
    public String description;
    public long dateTime; // epoch millis
    public GeoPoint geo;
    public List<String> interessados;
    public List<String> confirmados;
    public long createdAt;

    public Event() {}
}
