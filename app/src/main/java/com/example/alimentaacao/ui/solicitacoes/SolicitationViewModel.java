package com.example.alimentaacao.ui.solicitacoes;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.alimentaacao.data.firebase.FirestoreService;
import com.example.alimentaacao.data.model.Solicitation;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Collections;
import java.util.Comparator;

import java.util.ArrayList;
import java.util.List;

public class SolicitationViewModel extends ViewModel {

    private final FirestoreService fs = new FirestoreService();
    private final MutableLiveData<List<Solicitation>> _items = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> _error = new MutableLiveData<>(null);
    private ListenerRegistration reg;

    public LiveData<List<Solicitation>> items() { return _items; }
    public LiveData<String> error() { return _error; }

    /** Inicia a escuta das solicitações da ONG corrente. */
    public void start() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            _error.postValue("Usuário não autenticado.");
            return;
        }
        stop(); // evita listener duplicado

        reg = fs.listenSolicitationsByOwner(uid, (QuerySnapshot snap, FirebaseFirestoreException err) -> {
            if (err != null) {
                _error.postValue(err.getMessage());
                return;
            }
            List<Solicitation> list = new ArrayList<>();
            if (snap != null) {
                for (DocumentSnapshot ds : snap.getDocuments()) {
                    try {
                        Solicitation s = ds.toObject(Solicitation.class);
                        if (s != null) {
                            s.id = ds.getId();
                            list.add(s);
                        }
                    } catch (Exception ex) {
                        // Ignora documento mal formado e reporta erro amistoso (não derruba a tela)
                        _error.postValue("Documento ignorado: " + ds.getId());
                    }
                }
            }

            Collections.sort(list, new Comparator<Solicitation>() {
                @Override public int compare(Solicitation a, Solicitation b) {
                    long ta = (a.createdAt != null) ? a.createdAt.getTime() : 0L;
                    long tb = (b.createdAt != null) ? b.createdAt.getTime() : 0L;
                    return Long.compare(tb, ta); // desc (mais recente primeiro)
                }
            });

            _items.postValue(list);
        });
    }

    public void stop() {
        if (reg != null) {
            reg.remove();
            reg = null;
        }
    }

    /** Concluir (ATENDIDA). */
    public void concluir(String solicitationId) {
        fs.concludeSolicitation(solicitationId)
                .addOnFailureListener(err -> _error.postValue(err.getMessage()));
    }

    @Override
    protected void onCleared() {
        stop();
        super.onCleared();
    }
}
