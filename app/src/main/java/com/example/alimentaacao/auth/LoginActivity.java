package com.example.alimentaacao.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.alimentaacao.App;
import com.example.alimentaacao.R;
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
 * Login com Google. Sem chaves, não quebra: desabilita o botão e mostra dica.
 */
public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    @Nullable private FirebaseAuth auth;
    @Nullable private GoogleSignInClient googleClient;

    private final ActivityResultLauncher<Intent> googleSignInLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getData() == null) {
                    Toast.makeText(this, "Cancelado", Toast.LENGTH_SHORT).show();
                    return;
                }
                try {
                    GoogleSignInAccount account = GoogleSignIn.getSignedInAccountFromIntent(result.getData())
                            .getResult(ApiException.class);
                    if (account == null) {
                        Toast.makeText(this, "Conta Google nula", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (auth == null) {
                        Toast.makeText(this, "Firebase não configurado.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
                    auth.signInWithCredential(credential)
                            .addOnSuccessListener(r -> {
                                // Se quiser forçar seleção de tipo após login:
                                startActivity(new Intent(this, ChooseTypeActivity.class));
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                Log.e("LoginActivity", "Firebase signIn failure", e);
                                Toast.makeText(this, "Falha no login Firebase", Toast.LENGTH_SHORT).show();
                            });
                } catch (ApiException e) {
                    Log.e("LoginActivity", "Google signIn error", e);
                    Toast.makeText(this, "Erro no Google Sign-In", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 1) Verifica se temos Firebase pronto (google-services.json)
        if (App.isFirebaseReady()) {
            try {
                auth = FirebaseAuth.getInstance();
            } catch (Throwable t) {
                Log.e("LoginActivity", "FirebaseAuth init error", t);
                auth = null;
            }
        }

        // 2) Prepara GoogleSignInClient apenas se houver clientId válido
        String clientId = getString(R.string.default_web_client_id);
        boolean hasValidClientId = !TextUtils.isEmpty(clientId) && clientId.contains(".apps.googleusercontent.com");

        if (hasValidClientId) {
            GoogleSignInOptions.Builder b = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestEmail();
            // Só requisita IdToken se o clientId for válido
            b.requestIdToken(clientId);

            try {
                googleClient = GoogleSignIn.getClient(this, b.build());
            } catch (Throwable t) {
                Log.e("LoginActivity", "Erro criando GoogleSignInClient", t);
                googleClient = null;
            }
        } else {
            Log.w("LoginActivity", "default_web_client_id inválido ou ausente");
        }

        // 3) UI: botão de login
        binding.btnGoogleSignIn.setOnClickListener(v -> {
            if (googleClient == null) {
                Toast.makeText(this, "Configure o Google Sign-In (client_id).", Toast.LENGTH_LONG).show();
                return;
            }
            if (auth == null) {
                Toast.makeText(this, "Firebase não configurado (adicione google-services.json).", Toast.LENGTH_LONG).show();
                return;
            }
            Intent signInIntent = googleClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });

        // 4) Se já está logado E Firebase existe, entra no app
        if (auth != null && auth.getCurrentUser() != null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }

        // 5) Se não há chaves, informativo no botão para evitar crash silencioso
        if (googleClient == null || auth == null) {
            binding.btnGoogleSignIn.setEnabled(true); // mantém clicável para exibir mensagem
            binding.btnGoogleSignIn.setAlpha(1f);
        }
    }
}
