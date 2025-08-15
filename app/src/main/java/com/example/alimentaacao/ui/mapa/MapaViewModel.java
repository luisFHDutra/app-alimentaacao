package com.example.alimentaacao.ui.mapa;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

/**
 * ViewModel do Mapa. Carrega marcadores de ONGs e Eventos do Firestore.
 * Assumimos que os documentos possuem campos: title (String), lat (double), lng (double).
 */
public class MapaViewModel extends ViewModel {

    public static class MapMarker {
        public String id;
        public String title;
        public String snippet;
        public double lat;
        public double lng;
        public String type; // "ONG" ou "EVENTO"
    }

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final MutableLiveData<List<MapMarker>> markersLive = new MutableLiveData<>(new ArrayList<>());

    public LiveData<List<MapMarker>> markers() {
        return markersLive;
    }

    /** Carrega ONGs e Eventos (consulta simples; ajuste conforme seu schema). */
    public void load() {
        List<MapMarker> buffer = new ArrayList<>();

        db.collection("ongs")
                .orderBy("name", Query.Direction.ASCENDING)
                .limit(100)
                .get()
                .addOnSuccessListener(snaps -> {
                    for (DocumentSnapshot d : snaps) {
                        Double lat = d.getDouble("lat");
                        Double lng = d.getDouble("lng");
                        if (lat == null || lng == null) continue;
                        MapMarker m = new MapMarker();
                        m.id = d.getId();
                        m.title = d.getString("name");
                        m.snippet = "ONG";
                        m.lat = lat;
                        m.lng = lng;
                        m.type = "ONG";
                        buffer.add(m);
                    }
                    // Depois eventos
                    db.collection("events")
                            .orderBy("title", Query.Direction.ASCENDING)
                            .limit(100)
                            .get()
                            .addOnSuccessListener(evSnaps -> {
                                for (DocumentSnapshot d : evSnaps) {
                                    Double lat = d.getDouble("lat");
                                    Double lng = d.getDouble("lng");
                                    if (lat == null || lng == null) continue;
                                    MapMarker m = new MapMarker();
                                    m.id = d.getId();
                                    m.title = d.getString("title");
                                    m.snippet = "Evento";
                                    m.lat = lat;
                                    m.lng = lng;
                                    m.type = "EVENTO";
                                    buffer.add(m);
                                }
                                markersLive.postValue(buffer);
                            })
                            .addOnFailureListener(e -> markersLive.postValue(buffer));
                })
                .addOnFailureListener(e -> markersLive.postValue(buffer));
    }
}
