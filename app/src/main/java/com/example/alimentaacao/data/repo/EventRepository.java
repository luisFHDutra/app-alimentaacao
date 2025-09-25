package com.example.alimentaacao.data.repo;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.alimentaacao.data.firebase.FirestoreService;
import com.example.alimentaacao.data.model.Event;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Repository de eventos (escuta em tempo real) */
public class EventRepository {

    private final FirestoreService fs = new FirestoreService();
    private final MutableLiveData<List<Event>> list = new MutableLiveData<>(new ArrayList<>());
    private ListenerRegistration reg;

    public LiveData<List<Event>> list() { return list; }

    /** Eventos da ONG logada */
    public void listenMine(String ownerUid) {
        stop();
        reg = fs.listenEventsByOwner(ownerUid, (QuerySnapshot snap, com.google.firebase.firestore.FirebaseFirestoreException err) -> {
            if (err != null) { return; }
            List<Event> out = new ArrayList<>();
            if (snap != null) {
                for (DocumentSnapshot ds : snap.getDocuments()) {
                    try {
                        Event e = ds.toObject(Event.class);
                        if (e != null) {
                            e.id = ds.getId();
                            if (e.interessados == null) e.interessados = new ArrayList<>();
                            if (e.confirmados == null) e.confirmados = new ArrayList<>();
                            out.add(e);
                        }
                    } catch (Exception ignored) { }
                }
            }
            // ordena por dateTime (ou createdAt) desc
            Collections.sort(out, new Comparator<Event>() {
                @Override public int compare(Event a, Event b) {
                    long ta = a.dateTime != null ? a.dateTime.getTime() :
                            (a.createdAt != null ? a.createdAt.getTime() : 0L);
                    long tb = b.dateTime != null ? b.dateTime.getTime() :
                            (b.createdAt != null ? b.createdAt.getTime() : 0L);
                    return Long.compare(tb, ta);
                }
            });
            list.postValue(out);
        });
    }

    public void toggleInteresse(String eventId, String uid, boolean add) {
        fs.toggleInteresse(eventId, uid, add);
    }

    public void toggleConfirmado(String eventId, String uid, boolean add) {
        fs.toggleConfirmado(eventId, uid, add);
    }

    public void stop() {
        if (reg != null) { reg.remove(); reg = null; }
    }

    public void listenAll() {
        stop();
        reg = fs.listenAllEvents((QuerySnapshot snap, com.google.firebase.firestore.FirebaseFirestoreException err) -> {
            if (err != null) {
                // Se quiser propagar isso para a UI, veja a opção "Extra" abaixo.
                android.util.Log.w("EventRepository", "listenAll error", err);
                return;
            }

            List<Event> out = new ArrayList<>();
            if (snap != null) {
                for (DocumentSnapshot ds : snap.getDocuments()) {
                    try {
                        Event e = ds.toObject(Event.class);
                        if (e != null) {
                            e.id = ds.getId();
                            if (e.interessados == null) e.interessados = new ArrayList<>();
                            if (e.confirmados == null)  e.confirmados  = new ArrayList<>();
                            out.add(e);
                        }
                    } catch (Exception ignored) { }
                }
            }

            // Ordena por dateTime (fallback para createdAt) - mais recentes primeiro
            Collections.sort(out, new Comparator<Event>() {
                @Override public int compare(Event a, Event b) {
                    long ta = a.dateTime != null ? a.dateTime.getTime()
                            : (a.createdAt != null ? a.createdAt.getTime() : 0L);
                    long tb = b.dateTime != null ? b.dateTime.getTime()
                            : (b.createdAt != null ? b.createdAt.getTime() : 0L);
                    return Long.compare(tb, ta);
                }
            });

            list.postValue(out);
        });
    }

}
