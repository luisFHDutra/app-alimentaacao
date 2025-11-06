package com.example.alimentaacao.ui.mapa;

import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.alimentaacao.data.model.Event;
import com.example.alimentaacao.databinding.FragmentMapaBinding;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MapaFragment extends Fragment implements OnMapReadyCallback {

    private FragmentMapaBinding b;
    private GoogleMap map;
    private MapaViewModel vm;

    // foco vindo do widget (pode chegar antes do mapa estar pronto)
    private LatLng focoPendente = null;
    private Marker marcadorFoco = null;
    private static final float ZOOM_FOCO = 14f;

    // ---------- view/binding ----------
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        b = FragmentMapaBinding.inflate(inflater, container, false);

        // Ciclo de vida do MapView precisa ser iniciado aqui
        b.mapView.onCreate(savedInstanceState);
        b.mapView.getMapAsync(this);

        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        super.onViewCreated(v, s);
        vm = new ViewModelProvider(this).get(MapaViewModel.class);

        // 1) Markers vindos do Firestore
        vm.events().observe(getViewLifecycleOwner(), list -> {
            if (map == null) return;
            map.clear();
            marcadorFoco = null; // será recriado se houver foco
            if (list != null) {
                for (Event e : list) {
                    if (e.lat != null && e.lng != null) {
                        LatLng p = new LatLng(e.lat, e.lng);
                        map.addMarker(new MarkerOptions().position(p).title(e.title));
                    }
                }
            }
            // re-aplica foco visual se existir um foco pendente
            if (focoPendente != null) {
                aplicarFoco(focoPendente);
            }
        });

        // 2) Recebe foco vindo da MainActivity (widget)
        getParentFragmentManager().setFragmentResultListener(
                "focus_on_map",
                getViewLifecycleOwner(),
                (requestKey, bundle) -> {
                    double lat = bundle.getDouble("focus_lat", Double.NaN);
                    double lng = bundle.getDouble("focus_lng", Double.NaN);
                    if (!Double.isNaN(lat) && !Double.isNaN(lng)) {
                        LatLng p = new LatLng(lat, lng);
                        if (map != null) aplicarFoco(p);
                        else focoPendente = p;
                    }
                }
        );

        // 3) Barra de busca
        setupSearchBar();

        // 4) Garantir que a barra fique acima do mapa
        b.searchContainer.bringToFront();

        // 5) Empurra a barra para baixo do status bar
        ViewCompat.setOnApplyWindowInsetsListener(b.getRoot(), (view, insets) -> {
            Insets status = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            ViewGroup.MarginLayoutParams lp =
                    (ViewGroup.MarginLayoutParams) b.searchContainer.getLayoutParams();
            int extraPx = (int) (12 * getResources().getDisplayMetrics().density);
            lp.topMargin = status.top + extraPx;
            b.searchContainer.setLayoutParams(lp);
            return insets;
        });
    }

    // Centraliza câmera e coloca/atualiza um marcador de foco
    private void aplicarFoco(@NonNull LatLng alvo) {
        if (map == null) {
            focoPendente = alvo;
            return;
        }
        focoPendente = null;
        if (marcadorFoco != null) marcadorFoco.remove();
        marcadorFoco = map.addMarker(new MarkerOptions()
                .position(alvo)
                .title("Próximo evento"));
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(alvo, ZOOM_FOCO));
    }

    private void setupSearchBar() {
        final EditText et = b.etSearch;
        final ImageButton btn = b.btnSearch;

        et.setOnEditorActionListener((tv, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String q = et.getText() != null ? et.getText().toString().trim() : "";
                if (!q.isEmpty()) geocodificarECentralizar(q);
                return true;
            }
            return false;
        });

        btn.setOnClickListener(v -> {
            String q = et.getText() != null ? et.getText().toString().trim() : "";
            if (!q.isEmpty()) geocodificarECentralizar(q);
        });
    }

    private void geocodificarECentralizar(String query) {
        if (map == null) return;
        new Thread(() -> {
            try {
                Geocoder g = new Geocoder(requireContext(), Locale.getDefault());
                List<Address> res = g.getFromLocationName(query, 1);
                if (res != null && !res.isEmpty()) {
                    Address a = res.get(0);
                    double lat = a.getLatitude();
                    double lng = a.getLongitude();
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() -> {
                            LatLng p = new LatLng(lat, lng);
                            aplicarFoco(p);
                        });
                    }
                } else if (isAdded()) {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(), "Local não encontrado", Toast.LENGTH_SHORT).show());
                }
            } catch (IOException e) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(), "Erro ao buscar: " + e.getMessage(), Toast.LENGTH_LONG).show());
                }
            }
        }).start();
    }

    // ---------- MapView lifecycle ----------
    @Override public void onStart()   { super.onStart();   if (b != null) b.mapView.onStart();   if (vm != null) vm.start(); }
    @Override public void onResume()  { super.onResume();  if (b != null) b.mapView.onResume(); }
    @Override public void onPause()   { if (b != null) b.mapView.onPause();  super.onPause(); }
    @Override public void onStop()    { if (vm != null) vm.stop(); if (b != null) b.mapView.onStop(); super.onStop(); }
    @Override public void onLowMemory(){ super.onLowMemory(); if (b != null) b.mapView.onLowMemory(); }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (b != null) b.mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroyView() {
        if (b != null) {
            b.mapView.onDestroy();
            b = null;
        }
        super.onDestroyView();
    }

    // ---------- Google Map ----------
    @Override
    public void onMapReady(GoogleMap gMap) {
        MapsInitializer.initialize(requireContext());
        map = gMap;

        // Configuração básica da câmera (fallback)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(-23.55, -46.63), 11f));

        // Se já havia um foco pendente (ex.: veio do widget antes de onMapReady)
        if (focoPendente != null) {
            aplicarFoco(focoPendente);
        }
    }
}