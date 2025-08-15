package com.example.alimentaacao.ui.perfil;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.example.alimentaacao.data.model.User;
import com.example.alimentaacao.data.repo.UserRepository;

public class PerfilViewModel extends ViewModel {

    private final UserRepository userRepository = new UserRepository();

    public LiveData<User> me() {
        return userRepository.me();
    }

    /** Carrega o snapshot atual do usuário. */
    public void init() {
        userRepository.loadMe();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        userRepository.removeMeListener();
    }
}
