package com.example.alimentaacao.ui.solicitacoes;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.alimentaacao.data.model.Solicitation;
import com.example.alimentaacao.databinding.ItemSolicitacaoBinding;
import java.util.List;

public class SolicitationListAdapter extends RecyclerView.Adapter<SolicitationListAdapter.VH> {
    public interface ActionListener { void onMarcarDoacao(Solicitation s); void onConcluir(Solicitation s); }
    private List<Solicitation> data;
    private final ActionListener listener;

    public SolicitationListAdapter(List<Solicitation> data, ActionListener l) { this.data = data; this.listener = l; }

    public void submit(List<Solicitation> list) { this.data = list; notifyDataSetChanged(); }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(ItemSolicitacaoBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }
    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        Solicitation s = data.get(pos);
        h.b.tvTitle.setText(s.title);
        h.b.tvStatus.setText(s.status);
        h.b.btnDoar.setOnClickListener(v -> listener.onMarcarDoacao(s));
        h.b.btnConcluir.setOnClickListener(v -> listener.onConcluir(s));
    }
    @Override public int getItemCount() { return data != null ? data.size() : 0; }

    static class VH extends RecyclerView.ViewHolder {
        ItemSolicitacaoBinding b; VH(ItemSolicitacaoBinding b) { super(b.getRoot()); this.b = b; }
    }
}
