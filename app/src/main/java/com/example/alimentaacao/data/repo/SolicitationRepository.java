package com.example.alimentaacao.data.repo;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.alimentaacao.data.firebase.FirestoreService;
import com.example.alimentaacao.data.model.Solicitation;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.*;

import java.util.ArrayList;
import java.util.List;

public class SolicitationRepository {
    private final FirestoreService fs = new FirestoreService();
    private ListenerRegistration reg;
    private final MutableLiveData<List<Solicitation>> listLive = new MutableLiveData<>(new ArrayList<>());

    public LiveData<List<Solicitation>> list() { return listLive; }

    public void listenAll() {
        if (reg != null) reg.remove();
        reg = fs.solicitacoes().orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((qs, e) -> {
                    if (qs == null) return;
                    List<Solicitation> res = new ArrayList<>();
                    for (DocumentSnapshot d : qs.getDocuments()) {
                        Solicitation s = d.toObject(Solicitation.class);
                        if (s != null) { s.id = d.getId(); res.add(s); }
                    }
                    listLive.setValue(res);
                });
    }

    public Task<DocumentReference> add(Solicitation s) { return fs.addSolicitation(s); }
    public Task<Void> marcarAndamento(String id, String uid) { return fs.updateSolicitationStatus(id, "EM_ANDAMENTO", uid); }
    public Task<Void> concluir(String id) { return fs.updateSolicitationStatus(id, "CONCLUIDA", null); }
}
