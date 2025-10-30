package com.example.alimentaacao.ui.perfil;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.alimentaacao.R;
import com.example.alimentaacao.auth.LoginActivity;
import com.example.alimentaacao.data.firebase.FirestoreService;
import com.example.alimentaacao.data.model.User;
import com.example.alimentaacao.databinding.FragmentPerfilBinding;
import com.google.android.gms.auth.api.signin.*;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.*;

import java.util.Locale;

public class PerfilFragment extends Fragment {

    private FragmentPerfilBinding b;
    private PerfilViewModel vm;
    private Uri pendingPhoto = null;
    private boolean editMode = false;

    private Double pendingLat = null, pendingLng = null;

    private final ActivityResultLauncher<Intent> pickLocation =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), res -> {
                if (res.getResultCode() == android.app.Activity.RESULT_OK && res.getData() != null) {
                    double lat = res.getData().getDoubleExtra("lat", Double.NaN);
                    double lng = res.getData().getDoubleExtra("lng", Double.NaN);
                    if (!Double.isNaN(lat) && !Double.isNaN(lng)) {
                        setChosenLocation(lat, lng);
                    }
                }
            });

    private final ActivityResultLauncher<String> pickImage =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    pendingPhoto = uri;
                    Glide.with(this).load(uri).circleCrop().into(b.imgAvatar);
                }
            });

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        b = FragmentPerfilBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        vm = new ViewModelProvider(this).get(PerfilViewModel.class);

        vm.user().observe(getViewLifecycleOwner(), this::renderUser);
        vm.saving().observe(getViewLifecycleOwner(), s -> setSaving(s != null && s));
        vm.error().observe(getViewLifecycleOwner(), msg -> { if (msg!=null && !msg.isEmpty()) toast(msg); });

        b.btnEditar.setOnClickListener(v -> setEditMode(true));
        b.btnCancelar.setOnClickListener(v -> {
            setEditMode(false);
            vm.start();
            pendingPhoto = null;
            pendingLat = null;
            pendingLng = null;
        });
        b.btnSalvar.setOnClickListener(v -> {
            String nome   = b.etNome.getText()!=null ? b.etNome.getText().toString().trim() : "";
            String cidade = b.etCidade.getText()!=null ? b.etCidade.getText().toString().trim() : null;
            String uf     = b.etUf.getText()!=null ? b.etUf.getText().toString().trim() : null;
            vm.saveProfileEx(nome, pendingPhoto, cidade, uf, pendingLat, pendingLng);
            setEditMode(false);
            pendingPhoto = null;
        });

        b.btnChangePhoto.setOnClickListener(v -> {
            if (!editMode) { toast("Entre no modo de edição para alterar a foto."); return; }
            pickImage.launch("image/*");
        });

        b.btnDefinirLocalizacao.setOnClickListener(v -> {
            Intent i = new Intent(requireContext(), com.example.alimentaacao.ui.eventos.MapPickerActivity.class);
            if (pendingLat != null && pendingLng != null) {
                i.putExtra("lat", pendingLat);
                i.putExtra("lng", pendingLng);
            }
            pickLocation.launch(i);
        });

        // --- SAIR ---
        b.btnSair.setOnClickListener(v -> {
            if (vm != null) vm.stop();

            FirebaseAuth.getInstance().signOut();

            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build();
            GoogleSignIn.getClient(requireContext(), gso).signOut();

            Intent i = new Intent(requireContext(), LoginActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
        });

        // --- EXCLUIR CONTA ---
        b.btnExcluirConta.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Excluir conta")
                    .setMessage("Tem certeza? Seus eventos e solicitações serão removidos.")
                    .setPositiveButton("Excluir", (d,w) -> deleteAccountCascade())
                    .setNegativeButton("Cancelar", null)
                    .show();
        });

        setEditMode(false);
    }

    @Override public void onStart() { super.onStart(); vm.start(); }
    @Override public void onStop()  { vm.stop();  super.onStop(); }

    // ----------------- helpers -----------------

    private void setChosenLocation(double lat, double lng) {
        pendingLat = lat;
        pendingLng = lng;
        // Não exibimos mais Lat/Lng no UI. Apenas preenche Cidade/UF via geocoder.
        reverseGeocode(lat, lng);
    }

    private void reverseGeocode(double lat, double lng) {
        new Thread(() -> {
            try {
                android.location.Geocoder g = new android.location.Geocoder(requireContext(), Locale.getDefault());
                java.util.List<android.location.Address> res = g.getFromLocation(lat, lng, 1);
                if (res != null && !res.isEmpty()) {
                    android.location.Address a = res.get(0);
                    String city = safeCity(a);
                    String uf   = normalizeUfBR(a.getAdminArea(), a);
                    requireActivity().runOnUiThread(() -> {
                        if (b != null) {
                            if (city != null && !city.isEmpty()) b.etCidade.setText(city);
                            if (uf != null && !uf.isEmpty())     b.etUf.setText(uf);
                        }
                    });
                }
            } catch (Exception ignored) {}
        }).start();
    }

    private String safeCity(android.location.Address a) {
        if (a == null) return null;
        String city = a.getLocality();
        if (isBlank(city)) city = a.getSubAdminArea();
        if (isBlank(city)) city = a.getSubLocality();
        if (isBlank(city)) {
            String line = a.getAddressLine(0);
            if (!isBlank(line)) {
                String[] parts = line.split(" - ");
                if (parts.length >= 2) city = parts[parts.length - 2].trim();
            }
        }
        return city;
    }

    private String normalizeUfBR(String adminArea, android.location.Address a) {
        if (!isBlank(adminArea) && adminArea.length() == 2) {
            return adminArea.toUpperCase(Locale.ROOT);
        }
        String name = adminArea;
        if (isBlank(name) && a != null) name = a.getSubAdminArea();
        if (isBlank(name) && a != null) {
            String line = a.getAddressLine(0);
            if (!isBlank(line) && line.contains(" - ")) {
                String[] parts = line.split(" - ");
                String last = parts[parts.length - 1].trim();
                if (last.length() == 2) return last.toUpperCase(Locale.ROOT);
                name = last;
            }
        }
        if (isBlank(name)) return null;
        String n = name.trim().toLowerCase(Locale.ROOT);
        if (n.contains("acre")) return "AC";
        if (n.contains("alagoas")) return "AL";
        if (n.contains("amap")) return "AP";
        if (n.contains("amazonas")) return "AM";
        if (n.contains("bahia")) return "BA";
        if (n.contains("cear")) return "CE";
        if (n.contains("distrito federal")) return "DF";
        if (n.contains("espírito santo") || n.contains("espirito santo")) return "ES";
        if (n.contains("goi")) return "GO";
        if (n.contains("maranh")) return "MA";
        if (n.contains("mato grosso do sul")) return "MS";
        if (n.contains("mato grosso")) return "MT";
        if (n.contains("minas gerais")) return "MG";
        if (n.contains("pará") || n.contains("para\u00E1") || n.contains("para ")) return "PA";
        if (n.contains("paraíba") || n.contains("paraiba")) return "PB";
        if (n.contains("paran")) return "PR";
        if (n.contains("pernambuco")) return "PE";
        if (n.contains("piau")) return "PI";
        if (n.contains("rio de janeiro")) return "RJ";
        if (n.contains("rio grande do norte")) return "RN";
        if (n.contains("rio grande do sul")) return "RS";
        if (n.contains("rond")) return "RO";
        if (n.contains("roraim")) return "RR";
        if (n.contains("santa catarina")) return "SC";
        if (n.contains("são paulo") || n.contains("sao paulo")) return "SP";
        if (n.contains("sergip")) return "SE";
        if (n.contains("tocant")) return "TO";
        return name.length() >= 2 ? name.substring(0, 2).toUpperCase(Locale.ROOT) : name.toUpperCase(Locale.ROOT);
    }

    private boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }

    private void deleteAccountCascade() {
        setUiEnabled(false);

        FirebaseUser fu = FirebaseAuth.getInstance().getCurrentUser();
        if (fu == null) { toast("Nenhum usuário logado."); setUiEnabled(true); return; }
        String uid = fu.getUid();

        FirestoreService fs = new FirestoreService();
        fs.deleteAllOwnedContent(uid)
                .onSuccessTask(v -> fs.userRef(uid).delete())
                .addOnSuccessListener(v -> fu.delete()
                        .addOnSuccessListener(v2 -> {
                            toast("Conta excluída.");
                            Intent i = new Intent(requireContext(), LoginActivity.class);
                            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(i);
                        })
                        .addOnFailureListener(err -> {
                            toast("Entre novamente para excluir a conta.");
                            setUiEnabled(true);
                        })
                )
                .addOnFailureListener(err -> {
                    toast("Falha ao excluir dados: " + err.getMessage());
                    setUiEnabled(true);
                });
    }

    private void setEditMode(boolean on) {
        editMode = on;
        b.etNome.setEnabled(on);
        b.btnChangePhoto.setEnabled(on);

        b.etCidade.setEnabled(on);
        b.etUf.setEnabled(on);
        b.btnDefinirLocalizacao.setEnabled(on);

        b.btnEditar.setVisibility(on ? View.GONE : View.VISIBLE);
        b.btnSalvar.setVisibility(on ? View.VISIBLE : View.GONE);
        b.btnCancelar.setVisibility(on ? View.VISIBLE : View.GONE);
    }

    private void setUiEnabled(boolean enabled) {
        b.btnEditar.setEnabled(enabled);
        b.btnSalvar.setEnabled(enabled);
        b.btnCancelar.setEnabled(enabled);
        b.btnChangePhoto.setEnabled(enabled && editMode);
        b.btnSair.setEnabled(enabled);
        b.btnExcluirConta.setEnabled(enabled);
    }

    private void setSaving(boolean saving) {
        setUiEnabled(!saving);
    }

    private void toast(String m) {
        if (getContext()!=null) android.widget.Toast.makeText(getContext(), m, android.widget.Toast.LENGTH_SHORT).show();
    }

    private void renderUser(User u) {
        if (u == null) return;
        b.etNome.setText(u.name != null ? u.name : "");
        b.etEmail.setText(u.email != null ? u.email : "");
        b.etTipo.setText(u.type  != null ? u.type  : "");

        if (isBlank(b.etCidade.getText()!=null ? b.etCidade.getText().toString() : null)) {
            if (u.city != null) b.etCidade.setText(u.city);
        }
        if (isBlank(b.etUf.getText()!=null ? b.etUf.getText().toString() : null)) {
            if (u.uf != null) b.etUf.setText(u.uf);
        }

        if (pendingPhoto == null) {
            if (u.photoUrl != null && !u.photoUrl.isEmpty()) {
                Glide.with(this).load(u.photoUrl).circleCrop().into(b.imgAvatar);
            } else {
                b.imgAvatar.setImageResource(R.drawable.ic_account_circle_24);
            }
        }
    }
}
