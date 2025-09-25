package com.example.alimentaacao.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.alimentaacao.data.firebase.FirestoreService;
import com.example.alimentaacao.databinding.ActivityChooseTypeBinding;
import com.example.alimentaacao.ui.MainActivity;
import com.google.firebase.auth.FirebaseAuth;

public class ChooseTypeActivity extends AppCompatActivity {

    private ActivityChooseTypeBinding b;
    private final FirestoreService fs = new FirestoreService();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityChooseTypeBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            finish();
            return;
        }

        b.btnOng.setOnClickListener(v -> saveType("ONG"));
        b.btnVolunteer.setOnClickListener(v -> saveType("VOLUNTARIO"));
    }

    private void saveType(String type) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) { Toast.makeText(this, "Faça login novamente.", Toast.LENGTH_SHORT).show(); return; }

        fs.setUserType(uid, type)
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, "Tipo definido: " + type, Toast.LENGTH_SHORT).show();
                    Intent i = new Intent(this, MainActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(i);
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Erro: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }
}
