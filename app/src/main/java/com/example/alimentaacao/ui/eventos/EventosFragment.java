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

import java.util.ArrayList;

public class EventosFragment extends Fragment {

    private FragmentEventosBinding b;
    private EventViewModel vm;
    private EventListAdapter adapter;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        b = FragmentEventosBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        vm = new ViewModelProvider(this).get(EventViewModel.class);

        adapter = new EventListAdapter(new ArrayList<>(), new EventListAdapter.Listener() {
            @Override public void onInteresse(Event e, boolean add) { /* ONG não usa aqui */ }
            @Override public void onConfirmar(Event e, boolean add) { /* ONG não usa aqui */ }

            @Override public void onEditar(Event e) {
                DialogNovoEvento.newEditDialog(e)
                        .show(getParentFragmentManager(), "editar_evento");
            }

            @Override public void onExcluir(Event e) {
                new AlertDialog.Builder(requireContext())
                        .setTitle("Excluir evento")
                        .setMessage("Deseja realmente excluir este evento?")
                        .setPositiveButton("Excluir", (d, w) -> {
                            new FirestoreService().deleteEvent(e.id)
                                    .addOnSuccessListener(v2 ->
                                            Toast.makeText(requireContext(), "Evento excluído.", Toast.LENGTH_SHORT).show())
                                    .addOnFailureListener(err ->
                                            Toast.makeText(requireContext(), "Erro ao excluir: " + err.getMessage(), Toast.LENGTH_LONG).show());
                        })
                        .setNegativeButton("Cancelar", null)
                        .show();
            }
        }, /*isOng=*/ true);

        b.recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.recycler.addItemDecoration(new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));
        b.recycler.setAdapter(adapter);
        b.recycler.setClipToPadding(false);

        // FAB ação + garantir que fique por cima
        b.fabAdd.bringToFront();
        b.fabAdd.setOnClickListener(v ->
                new DialogNovoEvento().show(getParentFragmentManager(), "novo_evento"));

        // Ajusta margens do FAB e padding do Recycler conforme altura do BottomNav
        adjustForBottomNav();

        // Observers
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
        String uid = FirebaseAuth.getInstance().getUid();
        vm.startMine(uid);
    }

    @Override public void onStop() {
        vm.stop();
        super.onStop();
    }

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

            // Padding inferior no Recycler para não colidir com o FAB/BottomNav
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
