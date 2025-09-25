package com.example.alimentaacao.ui.solicitacoes;

import android.view.ViewGroup;
import android.view.View;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.util.TypedValue;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.alimentaacao.R;
import com.example.alimentaacao.data.firebase.FirestoreService;
import com.example.alimentaacao.data.model.Solicitation;
import com.example.alimentaacao.databinding.FragmentSolicitacoesBinding;

public class SolicitacoesFragment extends Fragment {

    private FragmentSolicitacoesBinding b;
    private SolicitationViewModel vm;
    private SolicitationListAdapter adapter;

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

        adapter = new SolicitationListAdapter(new SolicitationListAdapter.Listener() {
            @Override public void onConcluir(Solicitation s) {
                vm.concluir(s.id);
            }

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
        b.recycler.setClipToPadding(false); // para o conteúdo não ficar por baixo do FAB

        // FAB - ação
        b.fabAdd.setOnClickListener(v ->
                new DialogNovaSolicitacao().show(getParentFragmentManager(), "nova_solicitacao"));

        // FAB sempre por cima
        b.fabAdd.bringToFront();

        // Ajusta FAB e Recycler para não colidirem com o BottomNavigationView (que está na Activity)
        adjustForBottomNavAndInsets();

        // Observers
        vm.items().observe(getViewLifecycleOwner(), list -> {
            boolean empty = (list == null || list.isEmpty());
            b.emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
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

    // ----------------------
    // Helpers de layout
    // ----------------------

    private void adjustForBottomNavAndInsets() {
        // Executa após layout para obter alturas corretas
        b.getRoot().post(() -> {
            View bottomNav = requireActivity().findViewById(R.id.bottomNav);
            int bottomNavHeight = (bottomNav != null) ? bottomNav.getHeight() : 0;

            // Ajuste de margens do FAB (bottom/end)
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) b.fabAdd.getLayoutParams();
            int dp16 = dp(16);
            lp.bottomMargin = bottomNavHeight + dp16; // fica acima do BottomNav
            lp.setMarginEnd(dp16);
            b.fabAdd.setLayoutParams(lp);

            // Padding extra no Recycler para não ficar escondido atrás do FAB
            int fabFootprint = dp(72); // “sombra” do FAB + margem
            int padBottom = bottomNavHeight + fabFootprint;
            b.recycler.setPadding(
                    b.recycler.getPaddingLeft(),
                    b.recycler.getPaddingTop(),
                    b.recycler.getPaddingRight(),
                    padBottom
            );
        });

        // (Opcional) se quiser considerar também system bars (gestures/nav bar)
        ViewCompat.setOnApplyWindowInsetsListener(b.getRoot(), (v, insets) -> {
            // aqui você poderia somar insets.getInsets(Type.systemBars()).bottom ao cálculo acima
            return insets;
        });
    }

    private int dp(int v) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }
}
