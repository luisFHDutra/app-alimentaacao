package com.example.alimentaacao.ui.solicitacoes;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.alimentaacao.data.firebase.FirestoreService;
import com.example.alimentaacao.data.model.Solicitation;
import com.example.alimentaacao.databinding.FragmentSolicitacoesBinding;
import com.google.firebase.auth.FirebaseAuth;

public class SolicitacoesFragment extends Fragment {

    private FragmentSolicitacoesBinding b;
    private SolicitationViewModel vm;
    private SolicitationListAdapter adapter;
    private Boolean isOng = null; // definido após buscar tipo

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        b = FragmentSolicitacoesBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        vm = new ViewModelProvider(this).get(SolicitationViewModel.class);

        b.recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.recycler.addItemDecoration(new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));
        b.recycler.setAdapter(null); // definiremos após saber o tipo
        b.recycler.setClipToPadding(false);

        // Observers (podem ser registrados já)
        vm.items().observe(getViewLifecycleOwner(), list -> {
            boolean empty = (list == null || list.isEmpty());
            b.emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
            if (adapter != null) adapter.submit(list);
        });

        vm.error().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null && !msg.isEmpty()) {
                Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
            }
        });

        // Descobrir o tipo do usuário e configurar a UI
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            Toast.makeText(requireContext(), "Usuário não autenticado.", Toast.LENGTH_SHORT).show();
            // Default: voluntário (somente leitura)
            setupUi(false);
            vm.startOpen();
            return;
        }

        new FirestoreService().getUserType(uid)
                .addOnSuccessListener(type -> {
                    boolean ong = "ONG".equalsIgnoreCase(type != null ? type : "");
                    setupUi(ong);
                    if (ong) vm.startMine(uid); else vm.startOpen();
                })
                .addOnFailureListener(e -> {
                    // Falhou ao ler doc — tratar como voluntário (seguro por padrão)
                    setupUi(false);
                    vm.startOpen();
                });
    }

    @Override public void onStop() { vm.stop(); super.onStop(); }

    // ----------------------
    // UI por tipo
    // ----------------------
    private void setupUi(boolean ong) {
        this.isOng = ong;

        adapter = new SolicitationListAdapter(new SolicitationListAdapter.Listener() {
            @Override public void onConcluir(Solicitation s) { if (Boolean.TRUE.equals(isOng)) vm.concluir(s.id); }
            @Override public void onEditar(Solicitation s) {
                if (!Boolean.TRUE.equals(isOng)) return;
                DialogNovaSolicitacao.newEditDialog(s)
                        .show(getParentFragmentManager(), "editar_solicitacao");
            }
            @Override public void onExcluir(Solicitation s) {
                if (!Boolean.TRUE.equals(isOng)) return;
                new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle("Excluir solicitação")
                        .setMessage("Deseja realmente excluir esta solicitação?")
                        .setPositiveButton("Excluir", (d,w) ->
                                new com.example.alimentaacao.data.firebase.FirestoreService().deleteSolicitation(s.id)
                                        .addOnFailureListener(err ->
                                                android.widget.Toast.makeText(requireContext(),
                                                        "Erro: " + err.getMessage(),
                                                        android.widget.Toast.LENGTH_LONG).show()))
                        .setNegativeButton("Cancelar", null)
                        .show();
            }
        }, ong);
        b.recycler.setAdapter(adapter);

        // FAB só para ONG
        b.fabAdd.setVisibility(ong ? View.VISIBLE : View.GONE);
        if (ong) {
            b.fabAdd.setOnClickListener(v ->
                    new DialogNovaSolicitacao().show(getParentFragmentManager(), "nova_solicitacao"));
        } else {
            b.fabAdd.setOnClickListener(null);
        }

        // Estado vazio: texto e clique variam por tipo
        if (ong) {
            b.tvEmptySubtitle.setText("Toque em \"Nova\" para criar a primeira solicitação.");
            b.emptyState.setClickable(true);
            b.emptyState.setOnClickListener(v ->
                    new DialogNovaSolicitacao().show(getParentFragmentManager(), "nova_solicitacao"));
        } else {
            // VOLUNTÁRIO não cria solicitações
            b.tvEmptySubtitle.setText("Não há solicitações abertas no momento.");
            b.emptyState.setClickable(false);
            b.emptyState.setOnClickListener(null);
        }
    }
}
