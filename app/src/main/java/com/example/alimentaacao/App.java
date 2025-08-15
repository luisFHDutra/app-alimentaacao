package com.example.alimentaacao;

import android.app.Application;
import android.util.Log;

import com.google.firebase.FirebaseApp;

/**
 * Application com init defensivo do Firebase.
 * Se não houver google-services.json, não deixamos o app quebrar.
 */
public class App extends Application {

    private static boolean firebaseReady = false;

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            FirebaseApp app = FirebaseApp.initializeApp(this);
            firebaseReady = (app != null);
            if (!firebaseReady) {
                Log.w("App", "Firebase NÃO inicializado (faltando google-services.json?)");
            }
        } catch (Throwable t) {
            firebaseReady = false;
            Log.e("App", "Falha ao inicializar Firebase", t);
        }
    }

    public static boolean isFirebaseReady() {
        return firebaseReady;
    }
}
