package com.example.alimentaacao.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.alimentaacao.R;
import com.example.alimentaacao.data.firebase.FirestoreService;
import com.example.alimentaacao.databinding.ActivityLoginBinding;
import com.example.alimentaacao.ui.MainActivity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding b;
    private GoogleSignInClient googleClient;
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirestoreService fs = new FirestoreService();

    private final ActivityResultLauncher<Intent> signInLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getData() == null) { show(false); toast("Cancelado."); return; }
                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                try {
                    GoogleSignInAccount acct = task.getResult(ApiException.class);
                    firebaseAuthWithGoogle(acct.getIdToken());
                } catch (ApiException e) {
                    show(false);
                    Log.e("LoginActivity", "Google sign-in error", e);
                    toast("Falha no Google Sign-In (código " + e.getStatusCode() + ").");
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleClient = GoogleSignIn.getClient(this, gso);

        b.btnGoogleSignIn.setOnClickListener(v -> {
            show(true);
            signInLauncher.launch(googleClient.getSignInIntent());
        });

        // Se já há usuário logado, roteie corretamente (tipo x main)
        if (auth.getCurrentUser() != null) {
            show(true);
            routeAfterLogin(auth.getCurrentUser().getUid());
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                com.google.firebase.auth.FirebaseUser fu = auth.getCurrentUser();
                fs.upsertUserFromAuthRespectingProfile(fu)
                        .addOnSuccessListener(v -> routeAfterLogin(fu.getUid()))
                        .addOnFailureListener(e -> { show(false); toast("Erro ao registrar usuário: " + e.getMessage()); });
            } else {
                show(false);
                toast("Falha na autenticação.");
            }
        });
    }

    /** Decide para onde ir: ChooseTypeActivity se faltar 'type'; senão MainActivity. */
    private void routeAfterLogin(String uid) {
        fs.userHasType(uid)
                .addOnSuccessListener(hasType -> {
                    show(false);
                    if (Boolean.TRUE.equals(hasType)) {
                        goToMain();
                    } else {
                        goToChooseType();
                    }
                })
                .addOnFailureListener(e -> {
                    show(false);
                    // Em caso de falha de rede, tente ir para ChooseType (cria type depois)
                    goToChooseType();
                });
    }

    private void goToChooseType() {
        Intent i = new Intent(this, ChooseTypeActivity.class);
        // não limpa o backstack aqui para o usuário poder voltar ao login se quiser
        startActivity(i);
        finish(); // evita voltar para login automaticamente
    }

    private void goToMain() {
        Intent i = new Intent(this, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
    }

    private void show(boolean loading) {
        b.progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        b.btnGoogleSignIn.setEnabled(!loading);
    }

    private void toast(String m) {
        android.widget.Toast.makeText(this, m, android.widget.Toast.LENGTH_SHORT).show();
    }
}
