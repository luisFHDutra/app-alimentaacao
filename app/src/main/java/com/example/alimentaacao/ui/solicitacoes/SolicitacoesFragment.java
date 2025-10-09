package com.example.alimentaacao.ui.solicitacoes;

import android.util.TypedValue;
import android.view.ViewGroup;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.alimentaacao.R;
import com.example.alimentaacao.data.firebase.FirestoreService;
import com.example.alimentaacao.data.model.Solicitation;
import com.example.alimentaacao.databinding.FragmentSolicitacoesBinding;
import com.google.firebase.auth.FirebaseAuth;

public class SolicitacoesFragment extends Fragment {

    private FragmentSolicitacoesBinding b;
    private SolicitationViewModel vm;
    private SolicitationListAdapter adapter;
    private Boolean isOng = null;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        b = FragmentSolicitacoesBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        vm = new ViewModelProvider(this).get(SolicitationViewModel.class);

        // Recycler
        b.recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.recycler.addItemDecoration(new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));
        b.recycler.setClipToPadding(false);

        // Observers
        vm.items().observe(getViewLifecycleOwner(), list -> {
            boolean empty = (list == null || list.isEmpty());
            b.emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
            if (adapter != null) adapter.submit(list);
        });

        vm.error().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null && !msg.isEmpty()) Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
        });

        // Descobrir tipo do usuário
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            // Sem login -> modo voluntário
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
                    // Falhou ler /users -> modo seguro (voluntário)
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
            @Override public void onConcluir(Solicitation s) {
                if (Boolean.TRUE.equals(isOng)) vm.concluir(s.id);
            }
            @Override public void onEditar(Solicitation s) {
                if (!Boolean.TRUE.equals(isOng)) return;
                DialogNovaSolicitacao.newEditDialog(s)
                        .show(getParentFragmentManager(), "editar_solicitacao");
            }
            @Override public void onExcluir(Solicitation s) {
                if (!Boolean.TRUE.equals(isOng)) return;
                new AlertDialog.Builder(requireContext())
                        .setTitle("Excluir solicitação")
                        .setMessage("Deseja realmente excluir esta solicitação?")
                        .setPositiveButton("Excluir", (d,w) ->
                                new FirestoreService().deleteSolicitation(s.id)
                                        .addOnSuccessListener(v -> Toast.makeText(requireContext(),"Solicitação excluída.", Toast.LENGTH_SHORT).show())
                                        .addOnFailureListener(err ->
                                                Toast.makeText(requireContext(),"Erro: " + err.getMessage(), Toast.LENGTH_LONG).show()))
                        .setNegativeButton("Cancelar", null)
                        .show();
            }
        }, ong);
        b.recycler.setAdapter(adapter);

        // FAB só para ONG
        b.fabAdd.setVisibility(ong ? View.VISIBLE : View.GONE);
        b.fabAdd.setOnClickListener(null);
        if (ong) {
            b.fabAdd.bringToFront();      // garante ficar por cima do emptyState/Recycler/BottomNav
            adjustForBottomNav();         // margem inferior (evita “invadir” o BottomNav)
            b.fabAdd.setOnClickListener(v ->
                    new DialogNovaSolicitacao().show(getParentFragmentManager(), "nova_solicitacao"));

            // emptyState clicável só p/ ONG
            b.emptyState.setClickable(true);
            b.emptyState.setOnClickListener(v ->
                    new DialogNovaSolicitacao().show(getParentFragmentManager(), "nova_solicitacao"));

            // Se seu layout tiver esse TextView, atualize a mensagem (senão, ignore)
            if (b.tvEmptySubtitle != null) {
                b.tvEmptySubtitle.setText("Toque em \"Nova\" para criar a primeira solicitação.");
            }
        } else {
            // Voluntário: sem FAB, sem clique no emptyState
            b.emptyState.setClickable(false);
            b.emptyState.setOnClickListener(null);
            if (b.tvEmptySubtitle != null) {
                b.tvEmptySubtitle.setText("Não há solicitações abertas no momento.");
            }
        }
    }

    // Ajusta FAB/padding considerando a altura do BottomNav
    private void adjustForBottomNav() {
        b.getRoot().post(() -> {
            View bottomNav = requireActivity().findViewById(R.id.bottomNav);
            int bottomNavHeight = bottomNav != null ? bottomNav.getHeight() : 0;

            // Margem do FAB para não colidir com o BottomNav
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) b.fabAdd.getLayoutParams();
            int m16 = dp(16);
            lp.bottomMargin = bottomNavHeight + m16;
            lp.setMarginEnd(m16);
            b.fabAdd.setLayoutParams(lp);

            // Padding do Recycler para não ficar atrás do FAB
            int fabFootprint = dp(72);
            int padBottom = bottomNavHeight + fabFootprint;
            b.recycler.setPadding(
                    b.recycler.getPaddingLeft(),
                    b.recycler.getPaddingTop(),
                    b.recycler.getPaddingRight(),
                    padBottom
            );
        });
    }

    private int dp(int v) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }
}
