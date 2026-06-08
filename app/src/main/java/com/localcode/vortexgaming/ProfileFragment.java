package com.localcode.vortexgaming;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.localcode.vortexgaming.utils.SessionManager;
import com.localcode.vortexgaming.views.auth.LoginActivity;

public class ProfileFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        
        MaterialSwitch switchTheme = view.findViewById(R.id.switch_theme);
        Button btnLogout = view.findViewById(R.id.btn_logout);

        switchTheme.setOnCheckedChangeListener((buttonView, isChecked) -> 
            Toast.makeText(getContext(), isChecked ? "Tema oscuro activado" : "Tema claro activado", Toast.LENGTH_SHORT).show()
        );

        btnLogout.setOnClickListener(v -> {
            new SessionManager(requireContext()).clearSession();
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            requireActivity().finish();
        });
        
        return view;
    }
}