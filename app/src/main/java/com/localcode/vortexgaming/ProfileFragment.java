package com.localcode.vortexgaming;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import com.google.android.material.transition.MaterialFadeThrough;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.localcode.vortexgaming.utils.ContentFilter;
import com.localcode.vortexgaming.utils.FavoritesManager;
import com.localcode.vortexgaming.utils.SessionManager;
import com.localcode.vortexgaming.views.auth.LoginActivity;

public class ProfileFragment extends Fragment {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setEnterTransition(new MaterialFadeThrough());
        setExitTransition(new MaterialFadeThrough());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        SessionManager session = new SessionManager(requireContext());
        FavoritesManager favMgr = new FavoritesManager(requireContext());

        // ── User info ────────────────────────────────────────────────────────
        String username = session.getUsername();
        TextView tvName    = view.findViewById(R.id.tvProfileName);
        TextView tvEmail   = view.findViewById(R.id.tvProfileEmail);
        TextView tvDob     = view.findViewById(R.id.tvProfileDob);
        TextView tvInitial = view.findViewById(R.id.tvAvatarInitial);

        tvName.setText(username);
        tvEmail.setText(session.getEmail());
        String dob = session.getDob();
        tvDob.setText(dob.isEmpty() ? "—" : dob);
        tvInitial.setText(username.isEmpty() ? "G" : String.valueOf(username.charAt(0)).toUpperCase());

        // ── Stats ────────────────────────────────────────────────────────────
        TextView tvFavCount    = view.findViewById(R.id.tvFavCount);
        TextView tvNotifStatus = view.findViewById(R.id.tvNotifStatus);

        tvFavCount.setText(String.valueOf(favMgr.getFavoritesCount()));
        tvNotifStatus.setText(session.isNotificationsEnabled() ? "Activas" : "Desactivadas");

        // ── Switches ─────────────────────────────────────────────────────────
        MaterialSwitch switchTheme         = view.findViewById(R.id.switch_theme);
        MaterialSwitch switchNotifications = view.findViewById(R.id.switch_notifications);
        MaterialSwitch switchContentFilter = view.findViewById(R.id.switch_content_filter);

        switchTheme.setChecked(session.isThemeDark());
        switchNotifications.setChecked(session.isNotificationsEnabled());
        switchContentFilter.setChecked(ContentFilter.isEnabled(requireContext()));

        switchTheme.setOnCheckedChangeListener((btn, isChecked) -> {
            session.setThemeDark(isChecked);
            AppCompatDelegate.setDefaultNightMode(
                    isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
            // Recreate after brief delay so the switch animation finishes
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (isAdded()) requireActivity().recreate();
            }, 200);
        });

        switchNotifications.setOnCheckedChangeListener((btn, isChecked) -> {
            session.setNotificationsEnabled(isChecked);
            tvNotifStatus.setText(isChecked ? "Activas" : "Desactivadas");
        });

        switchContentFilter.setOnCheckedChangeListener((btn, isChecked) ->
                ContentFilter.setEnabled(requireContext(), isChecked));

        // ── Logout ───────────────────────────────────────────────────────────
        Button btnLogout = view.findViewById(R.id.btn_logout);
        btnLogout.setOnClickListener(v -> {
            session.clearSession();
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            requireActivity().finish();
        });

        return view;
    }
}
