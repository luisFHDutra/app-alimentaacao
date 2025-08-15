package com.example.alimentaacao.ui.perfil;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.alimentaacao.databinding.FragmentPerfilBinding;
import com.example.alimentaacao.data.model.User;
import com.example.alimentaacao.data.repo.UserRepository;
import com.google.firebase.auth.FirebaseAuth;

/**
 * Tela de perfil do usuário logado.
 */
public class PerfilFragment extends Fragment {

    private FragmentPerfilBinding binding;
    private final UserRepository userRepo = new UserRepository();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentPerfilBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        userRepo.me().observe(getViewLifecycleOwner(), this::bindUser);
        // Agora existe em UserRepository:
        userRepo.listenMe();

        binding.btnSignOut.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            requireActivity().finish();
        });
    }

    private void bindUser(@Nullable User u) {
        if (u == null) return;
        binding.tvName.setText(u.name);
        binding.tvEmail.setText(u.email);
        binding.tvType.setText(u.type != null ? u.type.toUpperCase() : "");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        userRepo.removeMeListener();
    }
}
