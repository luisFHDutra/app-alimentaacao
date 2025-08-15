package com.example.alimentaacao.ui;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.example.alimentaacao.R;
import com.example.alimentaacao.databinding.ActivityMainBinding;
import com.example.alimentaacao.ui.eventos.EventosFragment;
import com.example.alimentaacao.ui.mapa.MapaFragment;
import com.example.alimentaacao.ui.perfil.PerfilFragment;
import com.example.alimentaacao.ui.solicitacoes.SolicitacoesFragment;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.bottomNav.setOnItemSelectedListener(item -> {
            Fragment f;
            int id = item.getItemId();
            if (id == R.id.nav_eventos) f = new EventosFragment();
            else if (id == R.id.nav_mapa) f = new MapaFragment();
            else if (id == R.id.nav_perfil) f = new PerfilFragment();
            else f = new SolicitacoesFragment();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, f).commit();
            return true;
        });
        binding.bottomNav.setSelectedItemId(R.id.nav_solicitacoes);
    }
}
