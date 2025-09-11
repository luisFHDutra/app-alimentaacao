package com.example.alimentaacao.ui.solicitacoes;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.alimentaacao.data.model.Solicitation;
import com.example.alimentaacao.databinding.ItemSolicitacaoBinding;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Adapter simples para listar solicitações da ONG com ação "Concluir".
 * IDs do layout: tvTitulo, tvResumoItens, tvStatus, tvData, btnConcluir
 */
public class SolicitationListAdapter extends RecyclerView.Adapter<SolicitationListAdapter.VH> {

    public interface Listener {
        void onConcluir(Solicitation s);
    }

    private final Listener listener;
    private final List<Solicitation> data = new ArrayList<>();

    public SolicitationListAdapter(Listener listener) {
        this.listener = listener;
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

    @Override
    public int getItemCount() { return data.size(); }

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

            boolean podeConcluir = !"ATENDIDA".equalsIgnoreCase(s.status);
            b.btnConcluir.setEnabled(podeConcluir);
            b.btnConcluir.setAlpha(podeConcluir ? 1f : 0.5f);
            b.btnConcluir.setOnClickListener(v -> {
                if (listener != null) listener.onConcluir(s);
            });
        }
    }
}
