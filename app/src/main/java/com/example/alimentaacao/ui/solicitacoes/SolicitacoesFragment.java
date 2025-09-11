package com.example.alimentaacao.ui.solicitacoes;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;

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

        adapter = new SolicitationListAdapter(s -> vm.concluir(s.id));
        b.recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.recycler.addItemDecoration(new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));
        b.recycler.setAdapter(adapter);

        // --- FAB sempre acima dos overlays + margem considerando status bar ---
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(b.getRoot(), (v, insets) -> {
            androidx.core.graphics.Insets sys = insets.getInsets(
                    androidx.core.view.WindowInsetsCompat.Type.systemBars());

            ViewGroup.MarginLayoutParams lp =
                    (ViewGroup.MarginLayoutParams) b.fabAdd.getLayoutParams();

            // 16dp extra além da altura da status bar
            int extraTopPx = (int) (16 * getResources().getDisplayMetrics().density);
            int extraEndPx = (int) (12 * getResources().getDisplayMetrics().density);

            lp.topMargin = sys.top + extraTopPx;
            lp.setMarginEnd(sys.right + extraEndPx);
            b.fabAdd.setLayoutParams(lp);

            // Conteúdo também respeita a status bar (opcional)
            b.recycler.setPadding(
                    b.recycler.getPaddingLeft(),
                    sys.top,
                    b.recycler.getPaddingRight(),
                    b.recycler.getPaddingBottom()
            );
            return insets;
        });

        b.fabAdd.bringToFront();
        b.fabAdd.setOnClickListener(v -> {
            if (getLifecycle().getCurrentState().isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)) {
                new DialogNovaSolicitacao().show(getParentFragmentManager(), "nova_solicitacao");
            }
        });

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


    @Override
    public void onStart() {
        super.onStart();
        vm.start();
    }

    @Override
    public void onStop() {
        vm.stop();
        super.onStop();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true); // habilita menu do fragment
    }

    @Override
    public void onCreateOptionsMenu(@NonNull android.view.Menu menu, @NonNull android.view.MenuInflater inflater) {
        inflater.inflate(com.example.alimentaacao.R.menu.menu_solicitacoes, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull android.view.MenuItem item) {
        if (item.getItemId() == com.example.alimentaacao.R.id.action_nova_solicitacao) {
            new DialogNovaSolicitacao().show(getChildFragmentManager(), "nova_solicitacao");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
