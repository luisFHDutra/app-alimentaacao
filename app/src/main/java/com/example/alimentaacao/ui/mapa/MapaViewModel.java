package com.example.alimentaacao.ui.mapa;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.alimentaacao.data.firebase.FirestoreService;
import com.example.alimentaacao.data.model.Event;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class MapaViewModel extends ViewModel {
    private final FirestoreService fs = new FirestoreService();
    private final MutableLiveData<List<Event>> _events = new MutableLiveData<>(new ArrayList<>());
    private ListenerRegistration reg;

    public LiveData<List<Event>> events() { return _events; }

    public void start() {
        stop();
        reg = fs.listenAllEvents((QuerySnapshot snap, com.google.firebase.firestore.FirebaseFirestoreException err) -> {
            if (err != null) return;
            List<Event> list = new ArrayList<>();
            if (snap != null) {
                for (DocumentSnapshot ds : snap.getDocuments()) {
                    Event e = ds.toObject(Event.class);
                    if (e != null) { e.id = ds.getId(); list.add(e); }
                }
            }
            _events.postValue(list);
        });
    }

    public void stop() { if (reg != null) { reg.remove(); reg = null; } }

    @Override protected void onCleared() { stop(); super.onCleared(); }
}
