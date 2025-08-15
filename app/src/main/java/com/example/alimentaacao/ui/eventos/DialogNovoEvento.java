package com.example.alimentaacao.ui.eventos;

import android.app.Dialog;
import android.os.Bundle;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import com.example.alimentaacao.data.firebase.FirestoreService;
import com.example.alimentaacao.data.model.Event;
import com.example.alimentaacao.databinding.DialogNovoEventoBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.GeoPoint;

public class DialogNovoEvento extends DialogFragment {
    private DialogNovoEventoBinding b;

    @NonNull @Override public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        b = DialogNovoEventoBinding.inflate(getLayoutInflater());
        return new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Novo evento")
                .setView(b.getRoot())
                .setPositiveButton("Salvar", (d,w)->salvar())
                .setNegativeButton("Cancelar", (d,w)->d.dismiss()).create();
    }

    private void salvar() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Event e = new Event();
        e.ongId = uid;
        e.title = b.etTitulo.getText().toString();
        e.description = b.etDesc.getText().toString();
        e.dateTime = System.currentTimeMillis() + 86400000L;
        e.geo = new GeoPoint(-23.55, -46.63); // TODO geo real
        new FirestoreService().addEvent(e).addOnSuccessListener(r ->
                Toast.makeText(requireContext(), "Evento criado!", Toast.LENGTH_SHORT).show()
        );
    }
}
