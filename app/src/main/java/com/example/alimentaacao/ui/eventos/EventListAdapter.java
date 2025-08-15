package com.example.alimentaacao.ui.eventos;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.alimentaacao.data.model.Event;
import com.example.alimentaacao.databinding.ItemEventoBinding;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class EventListAdapter extends RecyclerView.Adapter<EventListAdapter.VH> {
    interface Listener { void onInteresse(Event e, boolean add); void onConfirmar(Event e, boolean add); }
    private List<Event> data; private final Listener l;
    public EventListAdapter(List<Event> d, Listener l) { this.data = d; this.l = l; }
    public void submit(List<Event> d) { data = d; notifyDataSetChanged(); }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int v) {
        return new VH(ItemEventoBinding.inflate(LayoutInflater.from(p.getContext()), p, false));
    }
    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        Event e = data.get(pos);
        h.b.tvTitle.setText(e.title);
        h.b.tvDesc.setText(e.description);
        String dt = new SimpleDateFormat("dd/MM/yyyy HH:mm", new Locale("pt","BR")).format(e.dateTime);
        h.b.tvDate.setText(dt);
        h.b.btnInteresse.setOnClickListener(v -> l.onInteresse(e, true));
        h.b.btnConfirmar.setOnClickListener(v -> l.onConfirmar(e, true));
    }
    @Override public int getItemCount() { return data!=null?data.size():0; }
    static class VH extends RecyclerView.ViewHolder { ItemEventoBinding b; VH(ItemEventoBinding b){ super(b.getRoot()); this.b=b; }}
}
