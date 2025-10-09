package com.example.alimentaacao.ui.eventos;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.alimentaacao.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.io.IOException;
import java.util.List;
import java.util.Locale;


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

        // --- Barra de pesquisa ---
        EditText et = findViewById(R.id.etSearchLocation);
        ImageButton btn = findViewById(R.id.btnSearchLocation);

        et.setOnEditorActionListener((tv, actionId, ev) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String q = et.getText() != null ? et.getText().toString().trim() : "";
                if (!q.isEmpty()) geocodeAndCenter(q);
                return true;
            }
            return false;
        });

        btn.setOnClickListener(v -> {
            String q = et.getText() != null ? et.getText().toString().trim() : "";
            if (!q.isEmpty()) geocodeAndCenter(q);
        });

        View container = findViewById(R.id.searchContainer);
        if (container != null) {
            container.bringToFront();
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root), (v, insets) -> {
                Insets status = insets.getInsets(WindowInsetsCompat.Type.statusBars());
                ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) container.getLayoutParams();
                int extraPx = (int) (12 * getResources().getDisplayMetrics().density);
                lp.topMargin = status.top + extraPx;
                container.setLayoutParams(lp);
                return insets;
            });
        }
    }

    private void geocodeAndCenter(String query) {
        if (map == null) return;
        new Thread(() -> {
            try {
                Geocoder g = new Geocoder(this, Locale.getDefault());
                List<Address> res = g.getFromLocationName(query, 1);
                if (res != null && !res.isEmpty()) {
                    Address a = res.get(0);
                    double la = a.getLatitude();
                    double ln = a.getLongitude();
                    runOnUiThread(() -> {
                        lat = la; lng = ln;
                        LatLng p = new LatLng(lat, lng);
                        if (marker != null) marker.remove();
                        marker = map.addMarker(new MarkerOptions().position(p).title("Local do evento"));
                        map.animateCamera(CameraUpdateFactory.newLatLngZoom(p, 14f));
                    });
                } else {
                    runOnUiThread(() ->
                            Toast.makeText(this, "Local não encontrado", Toast.LENGTH_SHORT).show());
                }
            } catch (IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(this, "Erro ao buscar: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
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
