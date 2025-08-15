package com.example.alimentaacao.ui.solicitacoes;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import com.example.alimentaacao.data.model.Solicitation;
import com.example.alimentaacao.data.repo.SolicitationRepository;
import java.util.List;

public class SolicitationViewModel extends AndroidViewModel {
    private final SolicitationRepository repo = new SolicitationRepository();
    public SolicitationViewModel(@NonNull Application app) { super(app); repo.listenAll(); }
    public LiveData<List<Solicitation>> list() { return repo.list(); }
    public void marcarAndamento(String id, String uid) { repo.marcarAndamento(id, uid); }
    public void concluir(String id) { repo.concluir(id); }
}
