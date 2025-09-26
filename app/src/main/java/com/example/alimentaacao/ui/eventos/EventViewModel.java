package com.example.alimentaacao.ui.eventos;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.alimentaacao.data.model.Event;
import com.example.alimentaacao.data.repo.EventRepository;
import java.util.List;

public class EventViewModel extends ViewModel {
    private final EventRepository repo = new EventRepository();
    private final MutableLiveData<String> _error = new MutableLiveData<>(null);

    public LiveData<List<Event>> list() { return repo.list(); }
    public LiveData<String> error() { return _error; }

    public void startMine(String ownerUid) {
        if (ownerUid == null || ownerUid.isEmpty()) {
            _error.postValue("Usuário não autenticado.");
            return;
        }
        repo.listenMine(ownerUid);
    }

    /** Para voluntário: ou para listar públicos em geral */
    public void startAll() { repo.listenAll(); }

    public void stop() { repo.stop(); }

    public void toggleInteresse(String id, String uid, boolean add) { repo.toggleInteresse(id, uid, add); }
    public void toggleConfirmado(String id, String uid, boolean add) { repo.toggleConfirmado(id, uid, add); }

    public void startAllOpen() {
        repo.listenAllOpen();
    }

    @Override protected void onCleared() { stop(); super.onCleared(); }
}
