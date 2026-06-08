package com.localcode.vortexgaming;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class RequestFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_request, container, false);
        
        Spinner spinner = view.findViewById(R.id.spinner_expansions);
        Button btnRequest = view.findViewById(R.id.btn_request);

        String[] options = {"Seleccione una expansión...", "Expansion 1: Cyberpunk", "Expansion 2: Retro"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, options);
        spinner.setAdapter(adapter);

        btnRequest.setOnClickListener(v -> {
            if (spinner.getSelectedItemPosition() == 0) {
                Toast.makeText(getContext(), "¡Error! Por favor, seleccione una expansión válida.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Solicitud enviada correctamente", Toast.LENGTH_SHORT).show();
            }
        });
        
        return view;
    }
}