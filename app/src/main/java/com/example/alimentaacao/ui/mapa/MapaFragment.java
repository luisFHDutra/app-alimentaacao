package com.example.alimentaacao.ui.mapa;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.alimentaacao.R;
import com.example.alimentaacao.databinding.FragmentMapaBinding;
import com.example.alimentaacao.ui.mapa.MapaViewModel.MapMarker;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

/**
 * Fragment do mapa (Google Maps).
 * Requer chave em meta-data do AndroidManifest e permissões de localização.
 */
public class MapaFragment extends Fragment {

    private FragmentMapaBinding binding;
    private MapaViewModel viewModel;
    private GoogleMap googleMap;
    private FusedLocationProviderClient fused;

    private final ActivityResultLauncher<String> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (googleMap != null) {
                    updateMyLocationLayer(granted);
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentMapaBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(MapaViewModel.class);
        fused = LocationServices.getFusedLocationProviderClient(requireContext());

        // Injeta o SupportMapFragment no container (R.id.mapContainer)
        SupportMapFragment mapFragment = (SupportMapFragment)
                getChildFragmentManager().findFragmentByTag("map");
        if (mapFragment == null) {
            mapFragment = SupportMapFragment.newInstance();
            getChildFragmentManager()
                    .beginTransaction()
                    .replace(R.id.mapContainer, mapFragment, "map")
                    .commitNow();
        }

        mapFragment.getMapAsync(map -> {
            googleMap = map;
            boolean granted = ContextCompat.checkSelfPermission(
                    requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED;
            updateMyLocationLayer(granted);
            if (!granted) {
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
            } else {
                moveCameraToLastLocation();
            }
            // Observa marcadores do ViewModel
            viewModel.markers().observe(getViewLifecycleOwner(), list -> {
                if (googleMap == null || list == null) return;
                googleMap.clear();
                for (MapMarker m : list) {
                    googleMap.addMarker(new MarkerOptions()
                            .position(new LatLng(m.lat, m.lng))
                            .title(m.title)
                            .snippet(m.snippet));
                }
            });
            // Carrega dados
            viewModel.load();
        });

        binding.fabMyLocation.setOnClickListener(v -> moveCameraToLastLocation());
    }

    private void updateMyLocationLayer(boolean granted) {
        try {
            googleMap.setMyLocationEnabled(granted);
        } catch (SecurityException ignored) {}
    }

    private void moveCameraToLastLocation() {
        try {
            fused.getLastLocation().addOnSuccessListener(loc -> {
                if (loc != null && googleMap != null) {
                    LatLng me = new LatLng(loc.getLatitude(), loc.getLongitude());
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(me, 14f));
                }
            });
        } catch (SecurityException ignored) {}
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
