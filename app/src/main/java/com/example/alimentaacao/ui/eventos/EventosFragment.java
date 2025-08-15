package com.example.alimentaacao.ui.eventos;

import android.os.Bundle;
import android.view.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.alimentaacao.data.model.Event;
import com.example.alimentaacao.databinding.FragmentEventosBinding;
import com.google.firebase.auth.FirebaseAuth;
import java.util.ArrayList;

public class EventosFragment extends Fragment {
    private FragmentEventosBinding b;
    private EventViewModel vm;
    private EventListAdapter adapter;

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        b = FragmentEventosBinding.inflate(inflater, container, false);
        adapter = new EventListAdapter(new ArrayList<>(), new EventListAdapter.Listener() {
            @Override public void onInteresse(Event e, boolean add) {
                String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                vm.toggleInteresse(e.id, uid, add);
            }
            @Override public void onConfirmar(Event e, boolean add) {
                String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                vm.toggleConfirmado(e.id, uid, add);
            }
        });
        b.recycler.setLayoutManager(new LinearLayoutManager(getContext()));
        b.recycler.setAdapter(adapter);
        b.fabAdd.setOnClickListener(v -> new DialogNovoEvento().show(getChildFragmentManager(), "novo_evento"));
        return b.getRoot();
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        vm = new ViewModelProvider(this).get(EventViewModel.class);
        vm.list().observe(getViewLifecycleOwner(), adapter::submit);
    }
}
