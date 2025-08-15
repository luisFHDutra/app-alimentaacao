package com.example.alimentaacao.ui.solicitacoes;

import android.app.Dialog;
import android.os.Bundle;
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
    @NonNull @Override public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        b = DialogNovaSolicitacaoBinding.inflate(getLayoutInflater());
        androidx.appcompat.app.AlertDialog.Builder d = new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Nova solicitação")
                .setView(b.getRoot())
                .setPositiveButton("Salvar", (dlg, w) -> salvar())
                .setNegativeButton("Cancelar", (dlg, w) -> dlg.dismiss());
        return d.create();
    }

    private void salvar() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Solicitation s = new Solicitation();
        s.ongId = uid;
        s.title = b.etTitulo.getText().toString();
        s.status = "ABERTA";
        s.items = Arrays.asList(new Solicitation.Item(b.etItem.getText().toString(), Integer.parseInt(b.etQtd.getText().toString())));
        s.geo = new GeoPoint(-23.55, -46.63); // TODO pegar geo real do usuário/ONG
        new FirestoreService().addSolicitation(s).addOnSuccessListener(dr -> {
            Toast.makeText(requireContext(), "Criada!", Toast.LENGTH_SHORT).show();
        });
    }
}
