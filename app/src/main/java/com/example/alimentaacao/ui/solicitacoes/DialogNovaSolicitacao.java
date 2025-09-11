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

    @NonNull @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        b = DialogNovaSolicitacaoBinding.inflate(getLayoutInflater());
        return new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Nova solicitação")
                .setView(b.getRoot())
                .setPositiveButton("Salvar", (dlg, w) -> salvar())
                .setNegativeButton("Cancelar", (dlg, w) -> dlg.dismiss())
                .create();
    }

    private void salvar() {
        if (!isAdded()) return;

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            Toast.makeText(requireContext(), "É necessário estar logado.", Toast.LENGTH_SHORT).show();
            return;
        }

        String titulo = b.etTitulo.getText() != null ? b.etTitulo.getText().toString().trim() : "";
        String itemNome = b.etItem.getText() != null ? b.etItem.getText().toString().trim() : "";
        String qtdStr = b.etQtd.getText() != null ? b.etQtd.getText().toString().trim() : "";

        if (titulo.isEmpty() || itemNome.isEmpty()) {
            Toast.makeText(requireContext(), "Preencha título e item.", Toast.LENGTH_SHORT).show();
            return;
        }

        int qtd = 1;
        try { qtd = Integer.parseInt(qtdStr); } catch (Exception ignored) { /* usa 1 */ }

        Solicitation s = new Solicitation();
        s.ownerUid = uid;
        s.ongId = uid;
        s.title = titulo;
        s.status = "ABERTA";
        s.items = java.util.Arrays.asList(new Solicitation.Item(itemNome, qtd));
        s.geo = new com.google.firebase.firestore.GeoPoint(-23.55, -46.63); // TODO: geo real

        new com.example.alimentaacao.data.firebase.FirestoreService().addSolicitation(s)
                .addOnSuccessListener(dr -> {
                    if (isAdded()) {
                        Toast.makeText(requireContext(), "Solicitação criada!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        Toast.makeText(requireContext(), "Erro: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
        // O AlertDialog padrão já fecha no botão positivo; nada extra aqui.
    }

}
