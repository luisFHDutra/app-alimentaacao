package com.example.alimentaacao.ui.mapa;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.alimentaacao.data.model.Event;
import com.example.alimentaacao.databinding.FragmentMapaBinding;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapaFragment extends Fragment implements OnMapReadyCallback {

    private FragmentMapaBinding b;
    private GoogleMap map;
    private MapaViewModel vm;

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater i, @Nullable ViewGroup c, @Nullable Bundle s) {
        b = FragmentMapaBinding.inflate(i, c, false);
        b.mapView.onCreate(s);
        b.mapView.getMapAsync(this);
        return b.getRoot();
    }

    @Override public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        super.onViewCreated(v, s);
        vm = new ViewModelProvider(this).get(MapaViewModel.class);
        vm.events().observe(getViewLifecycleOwner(), list -> {
            if (map == null) return;
            map.clear();
            for (Event e : list) {
                if (e.lat != null && e.lng != null) {
                    LatLng p = new LatLng(e.lat, e.lng);
                    map.addMarker(new MarkerOptions().position(p).title(e.title));
                }
            }
        });
    }

    @Override public void onStart() { super.onStart(); b.mapView.onStart(); vm.start(); }
    @Override public void onResume() { super.onResume(); b.mapView.onResume(); }
    @Override public void onPause() { b.mapView.onPause(); super.onPause(); }
    @Override public void onStop() { vm.stop(); b.mapView.onStop(); super.onStop(); }
    @Override public void onDestroyView() { b.mapView.onDestroy(); b = null; super.onDestroyView(); }
    @Override public void onLowMemory() { super.onLowMemory(); if (b != null) b.mapView.onLowMemory(); }

    @Override public void onMapReady(GoogleMap gMap) {
        MapsInitializer.initialize(requireContext());
        map = gMap;
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(-23.55,-46.63), 11f));
    }
}
