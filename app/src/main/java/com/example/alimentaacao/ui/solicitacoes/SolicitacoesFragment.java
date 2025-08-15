package com.example.alimentaacao.ui.solicitacoes;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.alimentaacao.data.model.Solicitation;
import com.example.alimentaacao.databinding.FragmentSolicitacoesBinding;
import com.google.firebase.auth.FirebaseAuth;
import java.util.ArrayList;
import java.util.List;

public class SolicitacoesFragment extends Fragment {
    private FragmentSolicitacoesBinding binding;
    private SolicitationViewModel vm;
    private SolicitationListAdapter adapter;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSolicitacoesBinding.inflate(inflater, container, false);

        adapter = new SolicitationListAdapter(new ArrayList<>(), new SolicitationListAdapter.ActionListener() {
            @Override public void onMarcarDoacao(Solicitation s) {
                String uid = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
                if (uid != null) vm.marcarAndamento(s.id, uid);
            }
            @Override public void onConcluir(Solicitation s) { vm.concluir(s.id); }
        });

        binding.recycler.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recycler.setAdapter(adapter);

        binding.fabAdd.setOnClickListener(v -> new DialogNovaSolicitacao().show(getChildFragmentManager(), "nova"));

        return binding.getRoot();
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        vm = new ViewModelProvider(this).get(SolicitationViewModel.class);
        vm.list().observe(getViewLifecycleOwner(), list -> adapter.submit(list));
    }
}
