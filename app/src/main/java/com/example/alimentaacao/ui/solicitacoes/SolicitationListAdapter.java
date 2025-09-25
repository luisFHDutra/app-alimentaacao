package com.example.alimentaacao.ui.solicitacoes;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.alimentaacao.data.model.Solicitation;
import com.example.alimentaacao.databinding.ItemSolicitacaoBinding;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;

public class SolicitationListAdapter extends RecyclerView.Adapter<SolicitationListAdapter.VH> {

    public interface Listener {
        void onConcluir(Solicitation s);
        void onEditar(Solicitation s);
        void onExcluir(Solicitation s);
    }

    private final Listener listener;
    private final boolean isOng;
    private final List<Solicitation> data = new ArrayList<>();

    public SolicitationListAdapter(Listener listener, boolean isOng) {
        this.listener = listener;
        this.isOng = isOng;
    }

    public void submit(List<Solicitation> list) {
        data.clear();
        if (list != null) data.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSolicitacaoBinding b = ItemSolicitacaoBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new VH(b);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        holder.bind(data.get(position));
    }

    @Override public int getItemCount() { return data.size(); }

    class VH extends RecyclerView.ViewHolder {
        final ItemSolicitacaoBinding b;
        VH(ItemSolicitacaoBinding binding) {
            super(binding.getRoot());
            this.b = binding;
        }

        void bind(Solicitation s) {
            b.tvTitulo.setText(s.title != null ? s.title : "(sem título)");

            if (s.items != null && !s.items.isEmpty()) {
                Solicitation.Item it = s.items.get(0);
                b.tvResumoItens.setText(it.nome + " x" + it.qtd + (s.items.size() > 1 ? " (+)" : ""));
            } else {
                b.tvResumoItens.setText("-");
            }

            b.tvStatus.setText(s.status != null ? s.status : "-");
            b.tvData.setText(s.createdAt != null
                    ? DateFormat.getDateTimeInstance().format(s.createdAt) : "-");

            // Ações variam por tipo
            if (isOng) {
                boolean podeConcluir = s.status == null || !"ATENDIDA".equalsIgnoreCase(s.status);
                b.btnConcluir.setVisibility(View.VISIBLE);
                b.btnEditar.setVisibility(View.VISIBLE);
                b.btnExcluir.setVisibility(View.VISIBLE);

                b.btnConcluir.setEnabled(podeConcluir);
                b.btnConcluir.setAlpha(podeConcluir ? 1f : 0.5f);
                b.btnConcluir.setOnClickListener(v -> { if (listener != null) listener.onConcluir(s); });
                b.btnEditar.setOnClickListener(v -> { if (listener != null) listener.onEditar(s); });
                b.btnExcluir.setOnClickListener(v -> { if (listener != null) listener.onExcluir(s); });
            } else {
                // VOLUNTÁRIO: sem ações de editar/excluir/concluir
                b.btnConcluir.setVisibility(View.GONE);
                b.btnEditar.setVisibility(View.GONE);
                b.btnExcluir.setVisibility(View.GONE);

                b.btnConcluir.setOnClickListener(null);
                b.btnEditar.setOnClickListener(null);
                b.btnExcluir.setOnClickListener(null);
            }
        }
    }
}
