package com.example.alimentaacao.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.alimentaacao.data.firebase.Bootstrapper;
import com.example.alimentaacao.databinding.ActivityChooseTypeBinding;
import com.example.alimentaacao.ui.MainActivity;

/**
 * O usuário escolhe o tipo. Gravamos em users/{uid}.type e criamos doc inicial por tipo.
 */
public class ChooseTypeActivity extends AppCompatActivity {

    private ActivityChooseTypeBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChooseTypeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnOng.setOnClickListener(v -> choose("ONG"));
        binding.btnVolunteer.setOnClickListener(v -> choose("VOLUNTARIO"));
    }

    private void choose(String type) {
        Bootstrapper.setUserType(type)
                .onSuccessTask(v -> Bootstrapper.ensureTypeDocuments(type))
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, "Perfil salvo: " + type, Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Falha ao salvar tipo: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }
}
