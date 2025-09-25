package com.example.alimentaacao.ui.eventos;

import android.os.Bundle;
import android.util.TypedValue;
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
import com.example.alimentaacao.R;
import com.example.alimentaacao.data.firebase.FirestoreService;
import com.example.alimentaacao.data.model.Event;
import com.example.alimentaacao.databinding.FragmentEventosBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.ArrayList;

public class EventosFragment extends Fragment {

    private FragmentEventosBinding b;
    private EventViewModel vm;
    private EventListAdapter adapter;
    private Boolean isOng = null; // indefinido até descobrir no Firestore
    private String uid;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        b = FragmentEventosBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        vm = new ViewModelProvider(this).get(EventViewModel.class);

        uid = FirebaseAuth.getInstance().getUid();

        // Recycler base
        b.recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.recycler.addItemDecoration(new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));
        b.recycler.setClipToPadding(false);

        // Ajuste de layout (padding e FAB acima do bottomNav)
        adjustForBottomNav();

        // Resolve tipo do usuário e então configura UI e dados
        resolveUserTypeAndSetup();
    }

    private void resolveUserTypeAndSetup() {
        if (uid == null) {
            Toast.makeText(requireContext(), "Usuário não autenticado.", Toast.LENGTH_LONG).show();
            // fallback: voluntário (visualização) sem ações de escrita
            setupUi(false);
            vm.startAll();
            hookObservers();
            return;
        }
        new FirestoreService().getUserDoc(uid)
                .addOnSuccessListener(this::onUserDoc)
                .addOnFailureListener(e -> {
                    // Se falhar, trate como voluntário
                    setupUi(false);
                    vm.startAll();
                    hookObservers();
                });
    }

    private void onUserDoc(DocumentSnapshot ds) {
        String type = ds != null ? ds.getString("type") : null;
        boolean ong = "ONG".equalsIgnoreCase(type);
        setupUi(ong);
        if (ong) vm.startMine(uid);
        else     vm.startAll();
        hookObservers();
    }

    /** Monta Adapter, FAB e empty state conforme seja ONG/voluntário */
    private void setupUi(boolean ong) {
        this.isOng = ong;

        adapter = new EventListAdapter(new ArrayList<>(), new EventListAdapter.Listener() {
            @Override public void onInteresse(Event e, boolean add) {
                if (uid == null) {
                    Toast.makeText(requireContext(), "Faça login para interagir.", Toast.LENGTH_SHORT).show();
                    return;
                }
                vm.toggleInteresse(e.id, uid, add);
            }
            @Override public void onConfirmar(Event e, boolean add) {
                if (uid == null) {
                    Toast.makeText(requireContext(), "Faça login para interagir.", Toast.LENGTH_SHORT).show();
                    return;
                }
                vm.toggleConfirmado(e.id, uid, add);
            }
            @Override public void onEditar(Event e) {
                if (!Boolean.TRUE.equals(isOng)) return;
                DialogNovoEvento.newEditDialog(e).show(getParentFragmentManager(), "editar_evento");
            }
            @Override public void onExcluir(Event e) {
                if (!Boolean.TRUE.equals(isOng)) return;
                new AlertDialog.Builder(requireContext())
                        .setTitle("Excluir evento")
                        .setMessage("Deseja realmente excluir este evento?")
                        .setPositiveButton("Excluir", (d, w) ->
                                new FirestoreService().deleteEvent(e.id)
                                        .addOnFailureListener(err ->
                                                Toast.makeText(requireContext(), "Erro ao excluir: " + err.getMessage(), Toast.LENGTH_LONG).show()))
                        .setNegativeButton("Cancelar", null)
                        .show();
            }
        }, /*isOng=*/ ong, /*currentUid=*/ uid);

        b.recycler.setAdapter(adapter);

        // FAB: só ONG cria evento
        b.fabAdd.setVisibility(ong ? View.VISIBLE : View.GONE);
        b.fabAdd.bringToFront();
        b.fabAdd.setOnClickListener(ong
                ? v -> new DialogNovoEvento().show(getParentFragmentManager(), "novo_evento")
                : null);

        // Empty state: texto e clique variam
        if (ong) {
            b.tvEmptySubtitle.setText("Toque em “Nova” para criar seu primeiro evento.");
            b.emptyState.setClickable(true);
            b.emptyState.setOnClickListener(v ->
                    new DialogNovoEvento().show(getParentFragmentManager(), "novo_evento"));
        } else {
            b.tvEmptySubtitle.setText("Não há eventos disponíveis no momento.");
            b.emptyState.setClickable(false);
            b.emptyState.setOnClickListener(null);
        }
    }

    private void hookObservers() {
        vm.list().observe(getViewLifecycleOwner(), list -> {
            boolean empty = list == null || list.isEmpty();
            b.emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
            adapter.submit(list);
        });

        vm.error().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null && !msg.isEmpty()) {
                Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override public void onStop() { vm.stop(); super.onStop(); }

    // ---- Helpers ----
    private void adjustForBottomNav() {
        b.getRoot().post(() -> {
            View bottomNav = requireActivity().findViewById(R.id.bottomNav);
            int bottomNavHeight = bottomNav != null ? bottomNav.getHeight() : 0;

            // Margem inferior do FAB para ficar acima do BottomNav
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) b.fabAdd.getLayoutParams();
            int m16 = dp(16);
            lp.bottomMargin = bottomNavHeight + m16;
            lp.setMarginEnd(m16);
            b.fabAdd.setLayoutParams(lp);

            // Padding inferior no Recycler para não colidir com FAB/BottomNav
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
