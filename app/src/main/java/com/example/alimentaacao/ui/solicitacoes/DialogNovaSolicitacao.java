package com.example.alimentaacao.ui.solicitacoes;

import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.alimentaacao.data.firebase.FirestoreService;
import com.example.alimentaacao.data.model.Solicitation;
import com.example.alimentaacao.databinding.DialogNovaSolicitacaoBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.GeoPoint;

import java.util.Arrays;

public class DialogNovaSolicitacao extends DialogFragment {
    private DialogNovaSolicitacaoBinding b;
    private String solicitationId = null; // se != null => edição

    @NonNull @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        b = DialogNovaSolicitacaoBinding.inflate(getLayoutInflater());

        // Se veio com argumentos, estamos em modo edição
        Bundle args = getArguments();
        if (args != null) {
            solicitationId = args.getString("id", null);
            b.etTitulo.setText(args.getString("title", ""));
            b.etItem.setText(args.getString("firstItemName", ""));
            int qtd = args.getInt("firstItemQtd", 1);
            b.etQtd.setText(String.valueOf(qtd));
        }

        return new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle(solicitationId == null ? "Nova solicitação" : "Editar solicitação")
                .setView(b.getRoot())
                .setPositiveButton("Salvar", (dlg, w) -> salvar())
                .setNegativeButton("Cancelar", (dlg, w) -> dlg.dismiss())
                .create();
    }

    private void salvar() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) { toast("É necessário estar logado."); return; }

        String titulo = b.etTitulo.getText() != null ? b.etTitulo.getText().toString().trim() : "";
        String itemNome = b.etItem.getText() != null ? b.etItem.getText().toString().trim() : "";
        String qtdStr = b.etQtd.getText() != null ? b.etQtd.getText().toString().trim() : "1";

        if (TextUtils.isEmpty(titulo) || TextUtils.isEmpty(itemNome)) {
            toast("Preencha título e item.");
            return;
        }

        int qtd = 1;
        try { qtd = Integer.parseInt(qtdStr); } catch (NumberFormatException ignored) {}

        FirestoreService fs = new FirestoreService();

        if (solicitationId == null) {
            // Criar
            Solicitation s = new Solicitation();
            s.ownerUid = uid;
            s.ongId = uid;
            s.title = titulo;
            s.status = "ABERTA";
            s.items = Arrays.asList(new Solicitation.Item(itemNome, qtd));
            s.geo = new GeoPoint(-23.55, -46.63); // TODO: geo real
            fs.addSolicitation(s)
                    .addOnSuccessListener(dr -> toast("Solicitação criada!"))
                    .addOnFailureListener(e -> toast("Erro: " + e.getMessage()));
        } else {
            // Editar (somente campos simples)
            fs.updateSolicitation(
                            solicitationId,
                            titulo,
                            Arrays.asList(new Solicitation.Item(itemNome, qtd)),
                            null,
                            null
                    ).addOnSuccessListener(v -> toast("Solicitação atualizada!"))
                    .addOnFailureListener(e -> toast("Erro: " + e.getMessage()));
        }
    }

    private void toast(String msg) {
        if (getContext() != null) Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
    }

    /** Helper para abrir em modo edição */
    public static DialogNovaSolicitacao newEditDialog(Solicitation s) {
        DialogNovaSolicitacao d = new DialogNovaSolicitacao();
        Bundle b = new Bundle();
        b.putString("id", s.id);
        b.putString("title", s.title);
        if (s.items != null && !s.items.isEmpty()) {
            b.putString("firstItemName", s.items.get(0).nome);
            b.putInt("firstItemQtd", s.items.get(0).qtd);
        }
        d.setArguments(b);
        return d;
    }
}
