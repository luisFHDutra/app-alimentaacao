package com.example.alimentaacao.ui.eventos;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.example.alimentaacao.data.firebase.FirestoreService;
import com.example.alimentaacao.data.model.Event;
import com.example.alimentaacao.databinding.ItemEventoBinding;
import com.google.firebase.firestore.DocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/** Adapter da lista de eventos (exibe nome da ONG dona do evento). */
public class EventListAdapter extends RecyclerView.Adapter<EventListAdapter.VH> {

    interface Listener {
        void onInteresse(Event e, boolean add);
        void onConfirmar(Event e, boolean add);
        void onEditar(Event e);
        void onExcluir(Event e);
    }

    private final Listener l;
    private final boolean isOng;                 // ONG vê Editar/Excluir; voluntário vê RSVP
    private final @Nullable String myUid;        // reservado p/ usos futuros (ex.: destacar meus RSVPs)
    private final List<Event> data = new ArrayList<>();

    // Cache simples (ownerUid -> ownerName) para evitar ler o mesmo usuário várias vezes
    private final HashMap<String, String> ownerNameCache = new HashMap<>();
    private final FirestoreService fs = new FirestoreService();

    // Construtor retrocompatível (3 parâmetros)
    public EventListAdapter(List<Event> initial, Listener l, boolean isOng) {
        this(initial, l, isOng, null);
    }

    // Construtor “completo”
    public EventListAdapter(List<Event> initial, Listener l, boolean isOng, @Nullable String myUid) {
        if (initial != null) data.addAll(initial);
        this.l = l;
        this.isOng = isOng;
        this.myUid = myUid;
    }

    public void submit(List<Event> list) {
        data.clear();
        if (list != null) data.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(ItemEventoBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Event e = data.get(position);

        h.b.tvTitle.setText(e.title != null ? e.title : "(sem título)");
        h.b.tvDesc.setText(e.description != null ? e.description : "-");

        String dt = e.dateTime != null
                ? new SimpleDateFormat("dd/MM/yyyy HH:mm", new Locale("pt", "BR")).format(e.dateTime)
                : "-";
        h.b.tvDate.setText(dt);

        int ni = (e.interessados != null) ? e.interessados.size() : 0;
        int nc = (e.confirmados != null) ? e.confirmados.size() : 0;
        h.b.tvCounters.setText(ni + " interessados • " + nc + " confirmados");

        // === Nome da ONG (dono do evento) ===
        setOwnerNameAsync(h, e.ownerUid);

        // === Ações ===
        if (isOng) {
            h.b.rowActions.setVisibility(View.VISIBLE);
            h.b.rowRsvp.setVisibility(View.GONE);

            h.b.btnEdit.setOnClickListener(v -> l.onEditar(e));
            h.b.btnDelete.setOnClickListener(v -> l.onExcluir(e));
        } else {
            h.b.rowActions.setVisibility(View.GONE);
            h.b.rowRsvp.setVisibility(View.VISIBLE);

            h.b.btnInteresse.setOnClickListener(v -> {
                boolean add = (e.interessados == null) || !e.interessados.contains(myUid);
                l.onInteresse(e, add);
            });
            h.b.btnConfirmar.setOnClickListener(v -> {
                boolean add = (e.confirmados == null) || !e.confirmados.contains(myUid);
                l.onConfirmar(e, add);
            });
        }
    }

    @Override public int getItemCount() { return data.size(); }

    /** Preenche tvOwnerName buscando em cache ou no Firestore (com proteção a reciclagem). */
    private void setOwnerNameAsync(@NonNull VH h, @Nullable String ownerUid) {
        if (h.b.tvOwnerName == null) return; // caso o layout antigo ainda esteja em algum build
        if (ownerUid == null || ownerUid.isEmpty()) {
            h.b.tvOwnerName.setText("-");
            h.b.tvOwnerName.setTag(null);
            return;
        }

        // se está no cache, usa direto
        if (ownerNameCache.containsKey(ownerUid)) {
            h.b.tvOwnerName.setText(ownerNameCache.get(ownerUid));
            h.b.tvOwnerName.setTag(ownerUid);
            return;
        }

        // placeholder e tag para checar reciclagem depois
        h.b.tvOwnerName.setText("Carregando…");
        h.b.tvOwnerName.setTag(ownerUid);

        fs.getUserDoc(ownerUid).addOnSuccessListener((DocumentSnapshot ds) -> {
            String name = (ds != null) ? ds.getString("name") : null;
            if (name == null || name.trim().isEmpty()) name = "ONG";
            ownerNameCache.put(ownerUid, name);

            // Só aplica se o holder ainda está representando o mesmo ownerUid
            Object tag = h.b.tvOwnerName.getTag();
            if (tag != null && ownerUid.equals(tag.toString())) {
                h.b.tvOwnerName.setText(name);
            }
        }).addOnFailureListener(err -> {
            Object tag = h.b.tvOwnerName.getTag();
            if (tag != null && ownerUid.equals(tag.toString())) {
                h.b.tvOwnerName.setText("ONG");
            }
        });
    }

    static class VH extends RecyclerView.ViewHolder {
        final ItemEventoBinding b;
        VH(ItemEventoBinding binding) {
            super(binding.getRoot());
            this.b = binding;
        }
    }
}
