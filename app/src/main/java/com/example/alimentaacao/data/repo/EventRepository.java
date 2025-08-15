package com.example.alimentaacao.data.repo;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.alimentaacao.data.firebase.FirestoreService;
import com.example.alimentaacao.data.model.Event;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.*;

import java.util.ArrayList;
import java.util.List;

public class EventRepository {
    private final FirestoreService fs = new FirestoreService();
    private ListenerRegistration reg;
    private final MutableLiveData<List<Event>> listLive = new MutableLiveData<>(new ArrayList<>());

    public LiveData<List<Event>> list() { return listLive; }

    public void listenAll() {
        if (reg != null) reg.remove();
        reg = fs.eventos().orderBy("dateTime", Query.Direction.ASCENDING)
                .addSnapshotListener((qs, e) -> {
                    if (qs == null) return;
                    List<Event> res = new ArrayList<>();
                    for (DocumentSnapshot d : qs.getDocuments()) {
                        Event ev = d.toObject(Event.class);
                        if (ev != null) { ev.id = d.getId(); res.add(ev); }
                    }
                    listLive.setValue(res);
                });
    }

    public Task<DocumentReference> add(Event e) { return fs.addEvent(e); }
    public Task<Void> toggleInteresse(String id, String uid, boolean add) { return fs.toggleInteresse(id, uid, add); }
    public Task<Void> toggleConfirmado(String id, String uid, boolean add) { return fs.toggleConfirmado(id, uid, add); }
}
