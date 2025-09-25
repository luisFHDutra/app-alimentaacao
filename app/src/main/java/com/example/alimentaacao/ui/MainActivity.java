package com.example.alimentaacao.ui;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.alimentaacao.R;
import com.example.alimentaacao.auth.ChooseTypeActivity;
import com.example.alimentaacao.auth.LoginActivity;
import com.example.alimentaacao.data.firebase.FirestoreService;
import com.example.alimentaacao.databinding.ActivityMainBinding;
import com.example.alimentaacao.ui.eventos.EventosFragment;
import com.example.alimentaacao.ui.mapa.MapaFragment;
import com.example.alimentaacao.ui.perfil.PerfilFragment;
import com.example.alimentaacao.ui.solicitacoes.SolicitacoesFragment;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private final FirestoreService fsGuard = new FirestoreService();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Cria coleções/índices base na 1ª execução (idempotente)
        com.example.alimentaacao.data.firebase.Bootstrapper.ensureBaseCollections()
                .addOnFailureListener(e ->
                        android.util.Log.w("MainActivity", "ensureBaseCollections falhou", e)
                );

        binding.bottomNav.setOnItemSelectedListener(item -> {
            Fragment f;
            int id = item.getItemId();
            if (id == R.id.nav_eventos)      f = new EventosFragment();
            else if (id == R.id.nav_mapa)    f = new MapaFragment();
            else if (id == R.id.nav_perfil)  f = new PerfilFragment();
            else                             f = new SolicitacoesFragment();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, f)
                    .commit();
            return true;
        });

        // Só seleciona a aba inicial se for primeira criação (evita reposição após rotação)
        if (savedInstanceState == null) {
            binding.bottomNav.setSelectedItemId(R.id.nav_solicitacoes);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        // 1) Se não estiver logado, volta ao Login
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Intent i = new Intent(this, LoginActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            return;
        }

        // 2) Se estiver logado, exige 'type' antes de usar o app
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        fsGuard.userHasType(uid)
                .addOnSuccessListener(hasType -> {
                    if (!Boolean.TRUE.equals(hasType)) {
                        Intent i = new Intent(this, ChooseTypeActivity.class);
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(i);
                    }
                })
                .addOnFailureListener(e -> {
                    // Em falha de rede/perm, mande escolher tipo (fluxo seguro)
                    Intent i = new Intent(this, ChooseTypeActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(i);
                });
    }
}
