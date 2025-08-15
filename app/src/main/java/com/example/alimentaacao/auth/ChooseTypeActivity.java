package com.example.alimentaacao.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.example.alimentaacao.databinding.ActivityChooseTypeBinding;
import com.example.alimentaacao.data.firebase.FirestoreService;
import com.example.alimentaacao.data.model.User;
import com.example.alimentaacao.ui.MainActivity;
import com.google.firebase.auth.FirebaseAuth;

public class ChooseTypeActivity extends AppCompatActivity {
    private ActivityChooseTypeBinding binding;
    private FirestoreService fs;

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChooseTypeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        fs = new FirestoreService();

        binding.btnOng.setOnClickListener(v -> saveType("ONG"));
        binding.btnVolunteer.setOnClickListener(v -> saveType("VOLUNTARIO"));
    }

    private void saveType(String type) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        User u = new User(uid,
                FirebaseAuth.getInstance().getCurrentUser().getDisplayName(),
                FirebaseAuth.getInstance().getCurrentUser().getEmail(),
                FirebaseAuth.getInstance().getCurrentUser().getPhotoUrl() != null
                        ? FirebaseAuth.getInstance().getCurrentUser().getPhotoUrl().toString() : null,
                type, null, System.currentTimeMillis());

        fs.createOrMergeUser(u).addOnSuccessListener(a -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }).addOnFailureListener(e -> Toast.makeText(this, "Erro ao salvar: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }
}
