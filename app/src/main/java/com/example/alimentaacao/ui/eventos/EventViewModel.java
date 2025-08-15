package com.example.alimentaacao.ui.eventos;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import com.example.alimentaacao.data.model.Event;
import com.example.alimentaacao.data.repo.EventRepository;
import java.util.List;

public class EventViewModel extends AndroidViewModel {
    private final EventRepository repo = new EventRepository();
    public EventViewModel(@NonNull Application app) { super(app); repo.listenAll(); }
    public LiveData<List<Event>> list() { return repo.list(); }
    public void toggleInteresse(String id, String uid, boolean add) { repo.toggleInteresse(id, uid, add); }
    public void toggleConfirmado(String id, String uid, boolean add) { repo.toggleConfirmado(id, uid, add); }
}
