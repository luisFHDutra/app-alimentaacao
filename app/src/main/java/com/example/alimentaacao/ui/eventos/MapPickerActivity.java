package com.example.alimentaacao.ui.eventos;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.alimentaacao.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

/** Activity simples para escolher um ponto no mapa e retornar lat/lng via setResult */
public class MapPickerActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap map;
    private Marker marker;
    private double lat = -23.55, lng = -46.63;

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_picker);

        if (getIntent()!=null) {
            if (getIntent().hasExtra("lat")) lat = getIntent().getDoubleExtra("lat", lat);
            if (getIntent().hasExtra("lng")) lng = getIntent().getDoubleExtra("lng", lng);
        }

        SupportMapFragment frag = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapFragment);
        if (frag != null) frag.getMapAsync(this);

        ExtendedFloatingActionButton fab = findViewById(R.id.fabOk);
        fab.setOnClickListener(v -> {
            Intent data = new Intent();
            data.putExtra("lat", lat);
            data.putExtra("lng", lng);
            setResult(RESULT_OK, data);
            finish();
        });
    }

    @Override public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        LatLng start = new LatLng(lat, lng);
        marker = map.addMarker(new MarkerOptions().position(start).title("Local do evento"));
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(start, 14f));

        map.setOnMapClickListener(p -> {
            lat = p.latitude; lng = p.longitude;
            if (marker != null) marker.remove();
            marker = map.addMarker(new MarkerOptions().position(p).title("Local do evento"));
            map.animateCamera(CameraUpdateFactory.newLatLng(p));
        });
    }
}
