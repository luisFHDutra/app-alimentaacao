package com.example.alimentaacao.data.model;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;
import java.util.List;

/** Evento público que aceita voluntários. */
public class Event {
    public String id;
    public String ownerUid;
    public String title;
    public String description;

    /** Horário do evento (Timestamp no Firestore → Date aqui) */
    public Date dateTime;

    public Double lat;
    public Double lng;

    public List<String> interessados;
    public List<String> confirmados;

    public String status;

    @ServerTimestamp public Date createdAt;
    @ServerTimestamp public Date updatedAt;

    public Event() {}
}
