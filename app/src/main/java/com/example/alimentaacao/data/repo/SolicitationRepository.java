package com.example.alimentaacao.data.repo;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.alimentaacao.data.firebase.FirestoreService;
import com.example.alimentaacao.data.model.Solicitation;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SolicitationRepository {
    private final FirestoreService fs = new FirestoreService();
    private ListenerRegistration reg;
    private final MutableLiveData<List<Solicitation>> listLive = new MutableLiveData<>(new ArrayList<>());

    public LiveData<List<Solicitation>> list() { return listLive; }

    /** Admin/depuração: escuta todas (sem filtro) */
    public void listenAll() {
        stop();
        reg = fs.solicitacoes()
                .addSnapshotListener((qs, e) -> {
                    if (e != null || qs == null) return;
                    List<Solicitation> res = new ArrayList<>();
                    for (DocumentSnapshot d : qs.getDocuments()) {
                        Solicitation s = d.toObject(Solicitation.class);
                        if (s != null) { s.id = d.getId(); res.add(s); }
                    }
                    // ordena por createdAt desc (em memória, evitando necessidade de índice)
                    Collections.sort(res, createdAtDesc());
                    listLive.postValue(res);
                });
    }

    /** ONG: somente minhas solicitações */
    public void listenMine(String ownerUid) {
        stop();
        reg = fs.solicitacoes()
                .whereEqualTo("ownerUid", ownerUid)
                .addSnapshotListener((qs, e) -> {
                    if (e != null || qs == null) return;
                    List<Solicitation> res = new ArrayList<>();
                    for (DocumentSnapshot d : qs.getDocuments()) {
                        Solicitation s = d.toObject(Solicitation.class);
                        if (s != null) { s.id = d.getId(); res.add(s); }
                    }
                    Collections.sort(res, createdAtDesc());
                    listLive.postValue(res);
                });
    }

    /**
     * VOLUNTÁRIO: somente abertas/andamento (esconde concluídas).
     * Ajuste os status conforme seu app. Aqui consideramos:
     *   "ABERTA" e "EM_ANDAMENTO" visíveis; "CONCLUIDA" escondida.
     */
    public void listenOpenSolicitacoes() {
        stop();
        // Sem orderBy p/ evitar índice; ordenamos em memória
        reg = fs.solicitacoes()
                .whereIn("status", java.util.Arrays.asList("ABERTA", "EM_ANDAMENTO"))
                .addSnapshotListener((qs, e) -> {
                    if (e != null || qs == null) return;
                    List<Solicitation> res = new ArrayList<>();
                    for (DocumentSnapshot d : qs.getDocuments()) {
                        Solicitation s = d.toObject(Solicitation.class);
                        if (s != null) { s.id = d.getId(); res.add(s); }
                    }
                    Collections.sort(res, createdAtDesc());
                    listLive.postValue(res);
                });
    }

    public Task<DocumentReference> add(Solicitation s) { return fs.addSolicitation(s); }
    public Task<Void> marcarAndamento(String id, String uid) { return fs.updateSolicitationStatus(id, "EM_ANDAMENTO", uid); }
    public Task<Void> concluir(String id) { return fs.updateSolicitationStatus(id, "CONCLUIDA", null); }

    /** Pare o listener atual (chame em onStop/onCleared) */
    public void stop() {
        if (reg != null) { reg.remove(); reg = null; }
    }

    private static Comparator<Solicitation> createdAtDesc() {
        return (a, b) -> {
            long ta = a.createdAt != null ? a.createdAt.getTime() : 0L;
            long tb = b.createdAt != null ? b.createdAt.getTime() : 0L;
            return Long.compare(tb, ta);
        };
    }
}
