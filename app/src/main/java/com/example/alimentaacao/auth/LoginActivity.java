package com.example.alimentaacao.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.alimentaacao.R;
import com.example.alimentaacao.data.firebase.Bootstrapper;
import com.example.alimentaacao.databinding.ActivityLoginBinding;
import com.example.alimentaacao.ui.MainActivity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;

/**
 * Fluxo:
 *  - Google Sign-In
 *  - Firebase signInWithCredential
 *  - ensureUserDocument + ensureBaseCollections
 *  - se users/{uid}.type inexistente -> ChooseTypeActivity
 *    senão -> MainActivity
 */
public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private FirebaseAuth auth;
    private GoogleSignInClient googleClient;

    private final ActivityResultLauncher<Intent> googleSignInLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getData() == null) {
                    Toast.makeText(this, "Login cancelado.", Toast.LENGTH_SHORT).show();
                    return;
                }
                try {
                    GoogleSignInAccount account = GoogleSignIn.getSignedInAccountFromIntent(result.getData())
                            .getResult(ApiException.class);
                    if (account == null) throw new ApiException(null);

                    AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
                    auth.signInWithCredential(credential)
                            .addOnSuccessListener(r -> afterFirebaseLogin())
                            .addOnFailureListener(e -> {
                                Log.e("LoginActivity", "Firebase signIn failure", e);
                                Toast.makeText(this, "Falha no login Firebase", Toast.LENGTH_SHORT).show();
                            });
                } catch (Exception e) {
                    Log.e("LoginActivity", "Google signIn error", e);
                    Toast.makeText(this, "Erro no Google Sign-In", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth = FirebaseAuth.getInstance();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id)) // gerado via google-services.json
                .requestEmail()
                .build();
        googleClient = GoogleSignIn.getClient(this, gso);

        binding.btnGoogleSignIn.setOnClickListener(v ->
                googleSignInLauncher.launch(googleClient.getSignInIntent()));

        // já logado? vai direto
        if (auth.getCurrentUser() != null) {
            afterFirebaseLogin();
        }
    }

    private void afterFirebaseLogin() {
        // 1) garante users/{uid}
        Bootstrapper.ensureUserDocument()
                .onSuccessTask(v -> Bootstrapper.ensureBaseCollections())
                .onSuccessTask(v -> Bootstrapper.fetchUserType())
                .addOnSuccessListener(type -> {
                    if (type == null || type.isEmpty()) {
                        startActivity(new Intent(this, ChooseTypeActivity.class));
                    } else {
                        startActivity(new Intent(this, MainActivity.class));
                    }
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e("LoginActivity", "Bootstrap pós-login falhou", e);
                    Toast.makeText(this, "Erro ao preparar o app", Toast.LENGTH_SHORT).show();
                });
    }
}
