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
        androidx.appcompat.app.AlertDialog.Builder d =
                new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle("Nova solicitação")
                        .setView(b.getRoot())
                        .setPositiveButton("Salvar", (dlg, w) -> salvar())
                        .setNegativeButton("Cancelar", (dlg, w) -> dlg.dismiss());
        return d.create();
    }

    private void salvar() {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        if (uid == null) {
            Toast.makeText(requireContext(), "É necessário estar logado.", Toast.LENGTH_SHORT).show();
            return;
        }

        String titulo = b.etTitulo.getText() != null ? b.etTitulo.getText().toString().trim() : "";
        String itemNome = b.etItem.getText() != null ? b.etItem.getText().toString().trim() : "";
        String qtdStr   = b.etQtd.getText() != null ? b.etQtd.getText().toString().trim() : "1";
        int qtd = 1;
        try {
            if (!TextUtils.isEmpty(qtdStr)) qtd = Integer.parseInt(qtdStr);
        } catch (NumberFormatException ignored) {}

        Solicitation s = new Solicitation();
        s.ownerUid = uid;     // dono do doc (para regras de segurança)
        s.ongId = uid;        // se você também quiser relacionar à ONG do mesmo uid
        s.title = titulo;
        s.status = "ABERTA";
        s.items = Arrays.asList(new Solicitation.Item(itemNome, qtd));
        s.geo = new GeoPoint(-23.55, -46.63); // TODO: pegar geo real

        new FirestoreService().addSolicitation(s)
                .addOnSuccessListener(dr -> Toast.makeText(requireContext(), "Solicitação criada!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(requireContext(), "Erro: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }
}
