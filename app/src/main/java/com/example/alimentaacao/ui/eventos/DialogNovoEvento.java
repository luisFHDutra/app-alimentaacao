package com.example.alimentaacao.ui.eventos;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.alimentaacao.data.firebase.FirestoreService;
import com.example.alimentaacao.data.model.Event;
import com.example.alimentaacao.databinding.DialogNovoEventoBinding;
import com.google.firebase.auth.FirebaseAuth;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Cria ou edita um evento.
 * Passe argumentos para editar:
 *   putString("id", ...), putString("title", ...), putString("desc", ...),
 *   putLong("dateMillis", ...), putDouble("lat"), putDouble("lng")
 */
public class DialogNovoEvento extends DialogFragment {
    private DialogNovoEventoBinding b;
    private final Calendar cal = Calendar.getInstance();
    private Double lat = null, lng = null;
    private String eventId = null; // se != null => modo edição

    // Map Picker result
    private final ActivityResultLauncher<Intent> pickLocation =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), res -> {
                if (res.getResultCode() == android.app.Activity.RESULT_OK && res.getData() != null) {
                    lat = res.getData().getDoubleExtra("lat", lat != null ? lat : 0d);
                    lng = res.getData().getDoubleExtra("lng", lng != null ? lng : 0d);
                    updateLocalLabel();
                }
            });

    @NonNull @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        b = DialogNovoEventoBinding.inflate(getLayoutInflater());

        // Carrega argumentos (edição)
        Bundle args = getArguments();
        if (args != null) {
            eventId = args.getString("id", null);
            b.etTitulo.setText(args.getString("title", ""));
            b.etDesc.setText(args.getString("desc", ""));
            long dm = args.getLong("dateMillis", -1);
            if (dm > 0) cal.setTimeInMillis(dm);
            if (args.containsKey("lat")) lat = args.getDouble("lat");
            if (args.containsKey("lng")) lng = args.getDouble("lng");
        }
        updateDateLabel();
        updateLocalLabel();

        b.btnPickDate.setOnClickListener(v -> pickDateTime());
        b.btnPickLocal.setOnClickListener(v -> {
            Intent i = new Intent(requireContext(), MapPickerActivity.class);
            if (lat != null && lng != null) {
                i.putExtra("lat", lat);
                i.putExtra("lng", lng);
            }
            pickLocation.launch(i);
        });

        return new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle(eventId == null ? "Novo evento" : "Editar evento")
                .setView(b.getRoot())
                .setPositiveButton("Salvar", (d,w)-> salvar())
                .setNegativeButton("Cancelar", (d,w)-> d.dismiss())
                .create();
    }

    private void pickDateTime() {
        final Calendar tmp = Calendar.getInstance();
        tmp.setTime(cal.getTime());
        new DatePickerDialog(requireContext(),
                (view, y, m, day) -> {
                    tmp.set(Calendar.YEAR, y);
                    tmp.set(Calendar.MONTH, m);
                    tmp.set(Calendar.DAY_OF_MONTH, day);
                    new TimePickerDialog(requireContext(),
                            (v, h, min) -> {
                                tmp.set(Calendar.HOUR_OF_DAY, h);
                                tmp.set(Calendar.MINUTE, min);
                                tmp.set(Calendar.SECOND, 0);
                                cal.setTimeInMillis(tmp.getTimeInMillis());
                                updateDateLabel();
                            },
                            cal.get(Calendar.HOUR_OF_DAY),
                            cal.get(Calendar.MINUTE),
                            true
                    ).show();
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void updateDateLabel() {
        String text = DateFormat.getDateTimeInstance().format(new Date(cal.getTimeInMillis()));
        b.tvChosenDate.setText(text);
    }

    private void updateLocalLabel() {
        b.tvChosenLocal.setText(lat != null && lng != null
                ? String.format(java.util.Locale.US, "Lat: %.5f, Lng: %.5f", lat, lng)
                : "-");
    }

    private void salvar() {
        if (!isAdded()) return;

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) { toastSafe("É necessário estar logado."); return; }

        String titulo = b.etTitulo.getText() != null ? b.etTitulo.getText().toString().trim() : "";
        String desc   = b.etDesc.getText() != null ? b.etDesc.getText().toString().trim()   : "";
        if (TextUtils.isEmpty(titulo)) { toastSafe("Informe um título."); return; }

        Date when = new Date(cal.getTimeInMillis());
        FirestoreService fs = new FirestoreService();

        if (eventId == null) {
            // criar
            Event e = new Event();
            e.ownerUid = uid;
            e.title = titulo;
            e.description = desc;
            e.dateTime = when;
            e.lat = (lat != null ? lat : -23.55);
            e.lng = (lng != null ? lng : -46.63);
            fs.addEvent(e)
                    .addOnSuccessListener(r -> toastSafe("Evento criado!"))
                    .addOnFailureListener(err -> toastSafe("Erro: " + err.getMessage()));
        } else {
            // editar
            fs.updateEvent(eventId, titulo, desc, when, lat, lng)
                    .addOnSuccessListener(r -> toastSafe("Evento atualizado!"))
                    .addOnFailureListener(err -> toastSafe("Erro: " + err.getMessage()));
        }
    }

    private void toastSafe(String msg) {
        Context c = getContext();
        if (c == null && getActivity()!=null) c = getActivity().getApplicationContext();
        if (c != null) Toast.makeText(c, msg, Toast.LENGTH_SHORT).show();
    }

    /** Helper p/ abrir em modo edição */
    public static DialogNovoEvento newEditDialog(Event e) {
        DialogNovoEvento d = new DialogNovoEvento();
        Bundle b = new Bundle();
        b.putString("id", e.id);
        b.putString("title", e.title);
        b.putString("desc", e.description);
        if (e.dateTime != null) b.putLong("dateMillis", e.dateTime.getTime());
        if (e.lat != null) b.putDouble("lat", e.lat);
        if (e.lng != null) b.putDouble("lng", e.lng);
        d.setArguments(b);
        return d;
    }
}
