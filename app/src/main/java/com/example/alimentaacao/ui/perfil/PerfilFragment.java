package com.example.alimentaacao.ui.perfil;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.*;
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

public class PerfilFragment extends Fragment {

    private FragmentPerfilBinding b;
    private PerfilViewModel vm;
    private Uri pendingPhoto = null;
    private boolean editMode = false;

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
        });
        b.btnSalvar.setOnClickListener(v -> {
            vm.saveProfile(b.etNome.getText() != null ? b.etNome.getText().toString().trim() : "", pendingPhoto);
            setEditMode(false);
            pendingPhoto = null;
        });
        b.btnChangePhoto.setOnClickListener(v -> {
            if (!editMode) { toast("Entre no modo de edição para alterar a foto."); return; }
            pickImage.launch("image/*");
        });

        // --- SAIR ---
        b.btnSair.setOnClickListener(v -> {
            // pare listeners para evitar PERMISSION_DENIED na transição
            if (vm != null) vm.stop();

            FirebaseAuth.getInstance().signOut();

            // Google sign-out (limpa conta escolhida no chooser)
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build();
            GoogleSignIn.getClient(requireContext(), gso).signOut();

            // volta para o login
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

    private void deleteAccountCascade() {
        setUiEnabled(false);

        FirebaseUser fu = FirebaseAuth.getInstance().getCurrentUser();
        if (fu == null) { toast("Nenhum usuário logado."); setUiEnabled(true); return; }
        String uid = fu.getUid();

        FirestoreService fs = new FirestoreService();
        // 1) apaga tudo que o dono criou (eventos + solicitações)
        fs.deleteAllOwnedContent(uid)
                // 2) apaga o doc do usuário
                .onSuccessTask(v -> fs.userRef(uid).delete())
                // 3) tenta apagar o usuário do Auth
                .addOnSuccessListener(v -> fu.delete()
                        .addOnSuccessListener(v2 -> {
                            toast("Conta excluída.");
                            goToLogin();
                        })
                        .addOnFailureListener(err -> {
                            // Geralmente precisa de reautenticação
                            toast("Entre novamente para excluir a conta.");
                            setUiEnabled(true);
                        })
                )
                .addOnFailureListener(err -> {
                    toast("Falha ao excluir dados: " + err.getMessage());
                    setUiEnabled(true);
                });
    }

    private void goToLogin() {
        Intent i = new Intent(requireContext(), LoginActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
    }

    private void renderUser(User u) {
        if (u == null) return;
        b.etNome.setText(u.name != null ? u.name : "");
        b.etEmail.setText(u.email != null ? u.email : "");
        b.etTipo.setText(u.type  != null ? u.type  : "");
        if (pendingPhoto == null) {
            if (u.photoUrl != null && !u.photoUrl.isEmpty()) {
                Glide.with(this).load(u.photoUrl).circleCrop().into(b.imgAvatar);
            } else {
                b.imgAvatar.setImageResource(R.drawable.ic_account_circle_24);
            }
        }
    }

    private void setEditMode(boolean on) {
        editMode = on;
        b.etNome.setEnabled(on);
        b.btnChangePhoto.setEnabled(on);
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
}
