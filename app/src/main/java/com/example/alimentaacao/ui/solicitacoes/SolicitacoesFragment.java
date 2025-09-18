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

public class SolicitacoesFragment extends Fragment {

    private FragmentSolicitacoesBinding b;
    private SolicitationViewModel vm;
    private SolicitationListAdapter adapter;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        b = FragmentSolicitacoesBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        vm = new ViewModelProvider(this).get(SolicitationViewModel.class);

        adapter = new SolicitationListAdapter(new SolicitationListAdapter.Listener() {
            @Override public void onConcluir(Solicitation s) { vm.concluir(s.id); }

            @Override public void onEditar(Solicitation s) {
                DialogNovaSolicitacao.newEditDialog(s)
                        .show(getParentFragmentManager(), "editar_solicitacao");
            }

            @Override public void onExcluir(Solicitation s) {
                new AlertDialog.Builder(requireContext())
                        .setTitle("Excluir solicitação")
                        .setMessage("Deseja realmente excluir esta solicitação?")
                        .setPositiveButton("Excluir", (d,w) ->
                                new FirestoreService().deleteSolicitation(s.id)
                                        .addOnFailureListener(err ->
                                                Toast.makeText(requireContext(),
                                                        "Erro: " + err.getMessage(),
                                                        Toast.LENGTH_LONG).show()))
                        .setNegativeButton("Cancelar", null)
                        .show();
            }
        });

        b.recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.recycler.addItemDecoration(new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));
        b.recycler.setAdapter(adapter);

        b.fabAdd.setOnClickListener(v ->
                new DialogNovaSolicitacao().show(getParentFragmentManager(), "nova_solicitacao"));

        b.emptyState.setOnClickListener(v ->
                new DialogNovaSolicitacao().show(getParentFragmentManager(), "nova_solicitacao"));

        vm.items().observe(getViewLifecycleOwner(), list -> {
            b.emptyState.setVisibility((list == null || list.isEmpty()) ? View.VISIBLE : View.GONE);
            adapter.submit(list);
        });

        vm.error().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null && !msg.isEmpty()) {
                Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override public void onStart() { super.onStart(); vm.start(); }

    @Override public void onStop() { vm.stop(); super.onStop(); }
}
