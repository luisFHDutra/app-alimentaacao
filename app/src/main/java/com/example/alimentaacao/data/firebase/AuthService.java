package com.example.alimentaacao.data.firebase;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class AuthService {
    public FirebaseUser current() { return FirebaseAuth.getInstance().getCurrentUser(); }
    public boolean isLogged() { return current() != null; }
    public void signOut() { FirebaseAuth.getInstance().signOut(); }
}
