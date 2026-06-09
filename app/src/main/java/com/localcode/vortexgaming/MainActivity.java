package com.localcode.vortexgaming;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;
import com.localcode.vortexgaming.utils.NotificationHelper;
import eightbitlab.com.blurview.BlurView;
import eightbitlab.com.blurview.RenderEffectBlur;
import eightbitlab.com.blurview.RenderScriptBlur;

public class MainActivity extends AppCompatActivity {

    private NavController navController;
    private BlurView customBottomNav;

    private FrameLayout pillHome, pillRequest, pillProfile;
    private ImageView iconHome, iconRequest, iconProfile;
    private TextView labelHome, labelRequest, labelProfile;

    // Fragments that should hide the bottom nav
    private static final int[] HIDDEN_NAV_DESTINATIONS = {
            R.id.navigation_search
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        navController = navHostFragment.getNavController();

        customBottomNav = findViewById(R.id.custom_bottom_nav);
        setupNavBlur();
        setupInsets();
        NotificationHelper.createChannel(this);
        requestNotificationPermission();
        pillHome    = findViewById(R.id.nav_home_pill);
        pillRequest = findViewById(R.id.nav_request_pill);
        pillProfile = findViewById(R.id.nav_profile_pill);
        iconHome    = findViewById(R.id.nav_home_icon);
        iconRequest = findViewById(R.id.nav_request_icon);
        iconProfile = findViewById(R.id.nav_profile_icon);
        labelHome   = findViewById(R.id.nav_home_label);
        labelRequest = findViewById(R.id.nav_request_label);
        labelProfile = findViewById(R.id.nav_profile_label);

        findViewById(R.id.nav_home).setOnClickListener(v -> selectTab(R.id.navigation_home));
        findViewById(R.id.nav_request).setOnClickListener(v -> selectTab(R.id.navigation_request));
        findViewById(R.id.nav_profile).setOnClickListener(v -> selectTab(R.id.navigation_profile));

        // Hide/show nav based on current destination
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            boolean hide = false;
            for (int id : HIDDEN_NAV_DESTINATIONS) {
                if (destination.getId() == id) { hide = true; break; }
            }
            customBottomNav.setVisibility(hide ? View.GONE : View.VISIBLE);
        });

        selectTab(R.id.navigation_home);
    }

    private void setupInsets() {
        // Push fragment content below the status bar
        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.nav_host_fragment),
                (v, insets) -> {
                    int top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
                    v.setPadding(0, top, 0, 0);
                    return insets;
                }
        );
        // Keep floating nav above the gesture navigation bar
        ViewCompat.setOnApplyWindowInsetsListener(
                customBottomNav,
                (v, insets) -> {
                    int bottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
                    ConstraintLayout.LayoutParams params =
                            (ConstraintLayout.LayoutParams) v.getLayoutParams();
                    params.bottomMargin = dp(10) + bottom;
                    v.setLayoutParams(params);
                    return insets;
                }
        );
    }

    @SuppressWarnings("deprecation")
    private void setupNavBlur() {
        ViewGroup rootView = (ViewGroup) getWindow().getDecorView().findViewById(android.R.id.content);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            customBottomNav.setupWith(rootView, new RenderEffectBlur())
                    .setBlurRadius(20f);
        } else {
            customBottomNav.setupWith(rootView, new RenderScriptBlur(this))
                    .setBlurRadius(20f);
        }
    }

    private void selectTab(int destinationId) {
        // Don't re-navigate if already there
        if (navController.getCurrentDestination() != null
                && navController.getCurrentDestination().getId() == destinationId) return;

        NavOptions options = new NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setPopUpTo(R.id.navigation_home, false)
                .build();
        navController.navigate(destinationId, null, options);

        // Active icon is always white (on the purple pill), inactive is theme-aware muted
        int activeIcon  = ContextCompat.getColor(this, R.color.chip_active_text);
        int inactiveIcon = ContextCompat.getColor(this, R.color.text_muted);

        pillHome.setBackgroundResource(destinationId == R.id.navigation_home ? R.drawable.bg_nav_item_active : 0);
        pillRequest.setBackgroundResource(destinationId == R.id.navigation_request ? R.drawable.bg_nav_item_active : 0);
        pillProfile.setBackgroundResource(destinationId == R.id.navigation_profile ? R.drawable.bg_nav_item_active : 0);

        iconHome.setColorFilter(destinationId == R.id.navigation_home ? activeIcon : inactiveIcon);
        iconRequest.setColorFilter(destinationId == R.id.navigation_request ? activeIcon : inactiveIcon);
        iconProfile.setColorFilter(destinationId == R.id.navigation_profile ? activeIcon : inactiveIcon);

        setLabelActive(labelHome,    destinationId == R.id.navigation_home);
        setLabelActive(labelRequest, destinationId == R.id.navigation_request);
        setLabelActive(labelProfile, destinationId == R.id.navigation_profile);
    }

    private void setLabelActive(TextView label, boolean active) {
        label.setTextColor(ContextCompat.getColor(this, active ? R.color.text_primary : R.color.text_muted));
        label.setTypeface(null, active ? Typeface.BOLD : Typeface.NORMAL);
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS}, 100);
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    public boolean onSupportNavigateUp() {
        return navController.navigateUp() || super.onSupportNavigateUp();
    }
}
