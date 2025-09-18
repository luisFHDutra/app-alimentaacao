package com.example.alimentaacao.ui.eventos;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;

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
            @Override public void onInteresse(Event e, boolean add) { /* não usado para ONG */ }
            @Override public void onConfirmar(Event e, boolean add) { /* não usado para ONG */ }

            @Override public void onEditar(Event e) {
                DialogNovoEvento.newEditDialog(e)
                        .show(getParentFragmentManager(), "editar_evento");
            }

            @Override public void onExcluir(Event e) {
                new FirestoreService().deleteEvent(e.id)
                        .addOnFailureListener(err -> Toast.makeText(requireContext(),
                                "Erro ao excluir: " + err.getMessage(), Toast.LENGTH_LONG).show());
            }
        }, /*isOng=*/ true);

        b.recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.recycler.addItemDecoration(new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));
        b.recycler.setAdapter(adapter);

        // Inset pro FAB ficar clicável
        ViewCompat.setOnApplyWindowInsetsListener(b.getRoot(), (v, ins) -> {
            Insets s = ins.getInsets(WindowInsetsCompat.Type.systemBars());
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) b.fabAdd.getLayoutParams();
            int extraTop = (int)(16 * getResources().getDisplayMetrics().density);
            int extraEnd = (int)(12 * getResources().getDisplayMetrics().density);
            lp.topMargin = s.top + extraTop; lp.setMarginEnd(s.right + extraEnd);
            b.fabAdd.setLayoutParams(lp); return ins;
        });

        b.fabAdd.bringToFront();
        b.fabAdd.setOnClickListener(v -> new DialogNovoEvento()
                .show(getParentFragmentManager(), "novo_evento"));

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
}
