package com.example.alimentaacao.ui;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.IdRes;
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
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private final FirestoreService fsGuard = new FirestoreService();

    // Referência usada por handleIntent(...)
    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        bottomNav = binding.bottomNav;

        // Listener do BottomNav (precisa estar configurado ANTES de chamar handleIntent)
        bottomNav.setOnItemSelectedListener(item -> {
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

        // Cria coleções/índices base na 1ª execução (idempotente)
        com.example.alimentaacao.data.firebase.Bootstrapper.ensureBaseCollections()
                .addOnFailureListener(e ->
                        android.util.Log.w("MainActivity", "ensureBaseCollections falhou", e)
                );

        // Seleciona aba vinda por Intent (ex.: clique no widget) ou cai no default
        boolean openedByIntent = handleIntent(getIntent());
        if (savedInstanceState == null && !openedByIntent) {
            bottomNav.setSelectedItemId(R.id.nav_solicitacoes);
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

    /**
     * Processa o Intent para selecionar uma aba específica e, se for o mapa,
     * repassar coordenadas ao MapaFragment via FragmentResult.
     *
     * @return true se alguma aba foi selecionada via intent; false caso contrário.
     */
    private boolean handleIntent(Intent intent) {
        if (intent == null) return false;

        // 1) Seleciona a aba solicitada
        int openTab = intent.getIntExtra("open_tab", -1);
        if (openTab != -1 && bottomNav != null) {
            @IdRes int tab = openTab;
            bottomNav.setSelectedItemId(tab);
        }

        // 2) Se for a aba de mapa e tiver coordenadas, envia para o fragment via FragmentResult
        if (openTab == R.id.nav_mapa &&
                intent.hasExtra("focus_lat") && intent.hasExtra("focus_lng")) {

            double lat = intent.getDoubleExtra("focus_lat", Double.NaN);
            double lng = intent.getDoubleExtra("focus_lng", Double.NaN);

            Bundle b = new Bundle();
            b.putDouble("focus_lat", lat);
            b.putDouble("focus_lng", lng);

            getSupportFragmentManager().setFragmentResult("focus_on_map", b);
        }

        return openTab != -1;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }
}