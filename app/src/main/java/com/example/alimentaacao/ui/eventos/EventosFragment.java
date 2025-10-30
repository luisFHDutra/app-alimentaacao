package com.example.alimentaacao.ui.eventos;

import android.os.Bundle;
import android.util.TypedValue;
import android.view.*;
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
    private boolean isOng = false; // começa como voluntário p/ evitar “piscar”

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        b = FragmentEventosBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        super.onViewCreated(v, s);
        vm = new ViewModelProvider(this).get(EventViewModel.class);

        // Adapter no modo *voluntário* inicialmente (sem ações e sem FAB)
        setEmptyHint(false);
        adapter = buildAdapter(false);
        b.recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.recycler.addItemDecoration(new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));
        b.recycler.setAdapter(adapter);
        b.recycler.setClipToPadding(false);

        // FAB ação
        b.fabAdd.setOnClickListener(v2 -> new DialogNovoEvento().show(getParentFragmentManager(), "novo_evento"));
        b.fabAdd.bringToFront();

        adjustForBottomNav();

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

    @Override public void onStart() {
        super.onStart();
        decideModeAndStart();
    }

    @Override public void onStop() {
        vm.stop();
        super.onStop();
    }

    // ---------- helpers ----------

    private EventListAdapter buildAdapter(boolean asOng) {
        return new EventListAdapter(new java.util.ArrayList<>(), new EventListAdapter.Listener() {
            @Override public void onInteresse(com.example.alimentaacao.data.model.Event e, boolean ignored) {
                if (asOng) return; // ONG não usa RSVP aqui
                String uid = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();
                if (uid == null) {
                    android.widget.Toast.makeText(requireContext(), "Faça login para registrar interesse.", android.widget.Toast.LENGTH_SHORT).show();
                    return;
                }
                boolean add = (e.interessados == null) || !e.interessados.contains(uid);
                // opcional: UI otimista (incrementa localmente até o snapshot chegar)
                if (e.interessados != null) {
                    java.util.List<String> tmp = new java.util.ArrayList<>(e.interessados);
                    if (add) { tmp.add(uid); } else { tmp.remove(uid); }
                    e.interessados = tmp;
                    b.recycler.getAdapter().notifyItemChanged(getAdapterPositionOf(e));
                }
                // efetiva no Firestore
                vm.toggleInteresse(e.id, uid, add);
            }

            @Override public void onConfirmar(com.example.alimentaacao.data.model.Event e, boolean ignored) {
                if (asOng) return;
                String uid = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();
                if (uid == null) {
                    android.widget.Toast.makeText(requireContext(), "Faça login para confirmar presença.", android.widget.Toast.LENGTH_SHORT).show();
                    return;
                }
                boolean add = (e.confirmados == null) || !e.confirmados.contains(uid);
                // opcional: UI otimista
                if (e.confirmados != null) {
                    java.util.List<String> tmp = new java.util.ArrayList<>(e.confirmados);
                    if (add) { tmp.add(uid); } else { tmp.remove(uid); }
                    e.confirmados = tmp;
                    b.recycler.getAdapter().notifyItemChanged(getAdapterPositionOf(e));
                }
                vm.toggleConfirmado(e.id, uid, add);
            }

            @Override public void onEditar(com.example.alimentaacao.data.model.Event e) {
                DialogNovoEvento.newEditDialog(e).show(getParentFragmentManager(), "editar_evento");
            }

            @Override public void onExcluir(com.example.alimentaacao.data.model.Event e) {
                new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle("Excluir evento")
                        .setMessage("Deseja realmente excluir este evento?")
                        .setPositiveButton("Excluir", (d, w) -> {
                            new com.example.alimentaacao.data.firebase.FirestoreService().deleteEvent(e.id)
                                    .addOnSuccessListener(v ->
                                            android.widget.Toast.makeText(requireContext(), "Evento excluído.", android.widget.Toast.LENGTH_SHORT).show())
                                    .addOnFailureListener(err ->
                                            android.widget.Toast.makeText(requireContext(), "Erro ao excluir: " + err.getMessage(), android.widget.Toast.LENGTH_LONG).show());
                        })
                        .setNegativeButton("Cancelar", null)
                        .show();
            }
        }, asOng);
    }

    private int getAdapterPositionOf(com.example.alimentaacao.data.model.Event e) {
        androidx.recyclerview.widget.RecyclerView.Adapter a = b.recycler.getAdapter();
        if (a instanceof com.example.alimentaacao.ui.eventos.EventListAdapter) {
            // percorrer rapidamente os itens já carregados
            // (se preferir, mova a lista para o VM/Repo e consulte de lá)
            // aqui, como não temos acesso direto, volte -1 e deixe o snapshot atualizar
        }
        return -1;
    }

    private void decideModeAndStart() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            // Continua no modo voluntário (sem FAB)
            vm.startAllOpen();
            return;
        }

        new FirestoreService().getUserDoc(uid)
                .addOnSuccessListener((DocumentSnapshot ds) -> {
                    String type = ds != null ? ds.getString("type") : null;
                    boolean nowOng = "ONG".equalsIgnoreCase(type);
                    if (nowOng != isOng) {
                        // troca a UI apenas se mudou
                        isOng = nowOng;
                        setEmptyHint(isOng);
                        b.fabAdd.setVisibility(isOng ? View.VISIBLE : View.GONE);
                        adapter = buildAdapter(isOng);
                        b.recycler.setAdapter(adapter);
                    }
                    if (isOng) vm.startMine(uid); else vm.startAllOpen();
                })
                .addOnFailureListener(e -> {
                    // fallback voluntário
                    isOng = false;
                    setEmptyHint(false);
                    b.fabAdd.setVisibility(View.GONE);
                    adapter = buildAdapter(false);
                    b.recycler.setAdapter(adapter);
                    vm.startAllOpen();
                });
    }

    private void setEmptyHint(boolean asOng) {
        if (b == null) return;
        b.tvEmptyTitle.setText("Nenhum evento ainda");
        b.tvEmptyHint.setText(asOng
                ? "Toque em “Nova” para criar seu primeiro evento."
                : "Nenhum evento disponível no momento.");
    }

    private void adjustForBottomNav() {
        b.getRoot().post(() -> {
            View bottomNav = requireActivity().findViewById(R.id.bottomNav);
            int bottomNavHeight = bottomNav != null ? bottomNav.getHeight() : 0;

            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) b.fabAdd.getLayoutParams();
            int m16 = dp(16);
            lp.bottomMargin = bottomNavHeight + m16;
            lp.setMarginEnd(m16);
            b.fabAdd.setLayoutParams(lp);

            int fabFootprint = dp(72);
            int padBottom = bottomNavHeight + fabFootprint;
            b.recycler.setPadding(b.recycler.getPaddingLeft(), b.recycler.getPaddingTop(),
                    b.recycler.getPaddingRight(), padBottom);
        });
    }

    private int dp(int v) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }
}
