package com.example.alimentaacao.ui.eventos;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.alimentaacao.data.model.Event;
import com.example.alimentaacao.databinding.ItemEventoBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class EventListAdapter extends RecyclerView.Adapter<EventListAdapter.VH> {
    interface Listener {
        void onInteresse(Event e, boolean add);
        void onConfirmar(Event e, boolean add);
        void onEditar(Event e);
        void onExcluir(Event e);
    }

    private final Listener l;
    private final boolean isOng; // se true, mostra Editar/Excluir e esconde RSVP
    private final List<Event> data = new ArrayList<>();

    public EventListAdapter(List<Event> initial, Listener l, boolean isOng) {
        if (initial != null) data.addAll(initial);
        this.l = l; this.isOng = isOng;
    }

    public void submit(List<Event> list) {
        data.clear(); if (list != null) data.addAll(list); notifyDataSetChanged();
    }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int v) {
        return new VH(ItemEventoBinding.inflate(LayoutInflater.from(p.getContext()), p, false));
    }

    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        Event e = data.get(pos);
        h.b.tvTitle.setText(e.title != null ? e.title : "(sem título)");
        h.b.tvDesc.setText(e.description != null ? e.description : "-");

        String dt = e.dateTime != null
                ? new SimpleDateFormat("dd/MM/yyyy HH:mm", new Locale("pt","BR")).format(e.dateTime)
                : "-";
        h.b.tvDate.setText(dt);

        int ni = e.interessados != null ? e.interessados.size() : 0;
        int nc = e.confirmados != null ? e.confirmados.size() : 0;
        h.b.tvCounters.setText(ni + " interessados • " + nc + " confirmados");

        if (isOng) {
            h.b.rowActions.setVisibility(View.VISIBLE);
            h.b.rowRsvp.setVisibility(View.GONE);
            h.b.btnEdit.setOnClickListener(v -> l.onEditar(e));
            h.b.btnDelete.setOnClickListener(v -> l.onExcluir(e));
        } else {
            h.b.rowActions.setVisibility(View.GONE);
            h.b.rowRsvp.setVisibility(View.VISIBLE);
            h.b.btnInteresse.setOnClickListener(v -> l.onInteresse(e, true));
            h.b.btnConfirmar.setOnClickListener(v -> l.onConfirmar(e, true));
        }
    }

    @Override public int getItemCount() { return data.size(); }

    static class VH extends RecyclerView.ViewHolder {
        final ItemEventoBinding b;
        VH(ItemEventoBinding b){ super(b.getRoot()); this.b=b; }
    }
}
