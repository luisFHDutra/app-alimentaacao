package com.example.alimentaacao.ui.eventos;

import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.alimentaacao.data.firebase.FirestoreService;
import com.example.alimentaacao.data.model.Event;
import com.example.alimentaacao.databinding.DialogNovoEventoBinding;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Date;

public class DialogNovoEvento extends DialogFragment {
    private DialogNovoEventoBinding b;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        b = DialogNovoEventoBinding.inflate(getLayoutInflater());
        return new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Novo evento")
                .setView(b.getRoot())
                .setPositiveButton("Salvar", (d, w) -> salvar())
                .setNegativeButton("Cancelar", (d, w) -> d.dismiss())
                .create();
    }

    private void salvar() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(requireContext(), "É necessário estar logado.", Toast.LENGTH_SHORT).show();
            return;
        }
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        String titulo = b.etTitulo.getText() != null ? b.etTitulo.getText().toString().trim() : "";
        String desc   = b.etDesc.getText()   != null ? b.etDesc.getText().toString().trim()   : "";

        if (TextUtils.isEmpty(titulo)) {
            Toast.makeText(requireContext(), "Informe um título.", Toast.LENGTH_SHORT).show();
            return;
        }

        Event e = new Event();
        // FirestoreService.addEvent já garante ownerUid, mas não custa setar aqui também:
        e.ownerUid = uid;
        e.title = titulo;
        e.description = desc;

        // Exemplo simples: evento para amanhã (24h a partir de agora).
        long millis = System.currentTimeMillis() + 24L * 60L * 60L * 1000L;
        e.dateTime = new Date(millis);

        // Se o seu layout não tem campos de LAT/LNG ainda, use um valor padrão seguro.
        // (Depois troque para a localização real da ONG ou campos no diálogo.)
        e.lat = -23.55;
        e.lng = -46.63;

        new FirestoreService().addEvent(e)
                .addOnSuccessListener(r ->
                        Toast.makeText(requireContext(), "Evento criado!", Toast.LENGTH_SHORT).show()
                )
                .addOnFailureListener(err ->
                        Toast.makeText(requireContext(), "Erro ao criar: " + err.getMessage(), Toast.LENGTH_LONG).show()
                );
    }
}
