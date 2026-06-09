package com.localcode.vortexgaming.views.auth;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.material.card.MaterialCardView;
import com.localcode.vortexgaming.MainActivity;
import com.localcode.vortexgaming.R;
import com.localcode.vortexgaming.utils.SessionManager;

public class SplashActivity extends AppCompatActivity {

    private static final int TOTAL_DURATION_MS = 2400;

    private ObjectAnimator pulseAnim;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Edge-to-edge: transparent bars with dark (light) icons
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);

        WindowInsetsControllerCompat wic =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        wic.setAppearanceLightStatusBars(false);
        wic.setAppearanceLightNavigationBars(false);

        setContentView(R.layout.activity_splash);

        // ── View references ───────────────────────────────────────────
        View             glowOuter     = findViewById(R.id.glowOuter);
        View             glowMid       = findViewById(R.id.glowMid);
        MaterialCardView logoCard      = findViewById(R.id.logoCard);
        View             tvAppName     = findViewById(R.id.tvAppName);
        View             tvTagline     = findViewById(R.id.tvTagline);
        View             progressTrack = findViewById(R.id.progressTrack);
        View             progressFill  = findViewById(R.id.progressFill);

        // ── Initial states (everything hidden / collapsed) ─────────────
        glowOuter.setAlpha(0f);
        glowOuter.setScaleX(0.35f);
        glowOuter.setScaleY(0.35f);

        glowMid.setAlpha(0f);
        glowMid.setScaleX(0.35f);
        glowMid.setScaleY(0.35f);

        logoCard.setAlpha(0f);
        logoCard.setScaleX(0.1f);
        logoCard.setScaleY(0.1f);

        tvAppName.setAlpha(0f);
        tvAppName.setTranslationY(32f);

        tvTagline.setAlpha(0f);
        tvTagline.setTranslationY(20f);

        // ── Animation sequence ────────────────────────────────────────

        // 1 ── Outer glow ring expands (t=0 ms)
        glowOuter.animate()
                .alpha(1f).scaleX(1f).scaleY(1f)
                .setDuration(720)
                .setInterpolator(new DecelerateInterpolator(2.2f))
                .start();

        // 2 ── Mid glow ring expands (t=80 ms)
        glowMid.animate()
                .alpha(1f).scaleX(1f).scaleY(1f)
                .setDuration(620).setStartDelay(80)
                .setInterpolator(new DecelerateInterpolator(2f))
                .start();

        // 3 ── Logo card pops in with overshoot (t=180 ms)
        logoCard.animate()
                .alpha(1f).scaleX(1f).scaleY(1f)
                .setDuration(540).setStartDelay(180)
                .setInterpolator(new OvershootInterpolator(1.9f))
                .start();

        // 4 ── App name slides up (t=490 ms)
        tvAppName.animate()
                .alpha(1f).translationY(0f)
                .setDuration(430).setStartDelay(490)
                .setInterpolator(new DecelerateInterpolator(1.5f))
                .start();

        // 5 ── Tagline fades in (t=640 ms)
        tvTagline.animate()
                .alpha(1f).translationY(0f)
                .setDuration(380).setStartDelay(640)
                .setInterpolator(new DecelerateInterpolator())
                .start();

        // 6 ── Outer ring breathes / pulses (starts at t=780 ms, loops)
        pulseAnim = ObjectAnimator.ofPropertyValuesHolder(glowOuter,
                PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.10f, 1f),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.10f, 1f),
                PropertyValuesHolder.ofFloat(View.ALPHA,   1f, 0.55f, 1f));
        pulseAnim.setDuration(1900);
        pulseAnim.setStartDelay(780);
        pulseAnim.setRepeatCount(ObjectAnimator.INFINITE);
        pulseAnim.setRepeatMode(ObjectAnimator.RESTART);
        pulseAnim.setInterpolator(new AccelerateDecelerateInterpolator());
        pulseAnim.start();

        // 7 ── Progress bar fills from left to right (t=320 ms → 1700 ms)
        progressTrack.post(() -> {
            int maxW = progressTrack.getMeasuredWidth();
            if (maxW == 0) {
                // Fallback: 200dp in pixels
                maxW = (int)(200 * getResources().getDisplayMetrics().density);
            }
            final int finalMaxW = maxW;
            ValueAnimator pa = ValueAnimator.ofInt(0, finalMaxW);
            pa.setDuration(1680);
            pa.setStartDelay(320);
            pa.setInterpolator(new DecelerateInterpolator(1.3f));
            pa.addUpdateListener(va -> {
                ViewGroup.LayoutParams lp = progressFill.getLayoutParams();
                lp.width = (int) va.getAnimatedValue();
                progressFill.setLayoutParams(lp);
            });
            pa.start();
        });

        // 8 ── Navigate when everything has settled
        new Handler(Looper.getMainLooper()).postDelayed(this::navigateNext, TOTAL_DURATION_MS);
    }

    private void navigateNext() {
        if (isFinishing()) return;
        if (pulseAnim != null) pulseAnim.cancel();

        SessionManager session = new SessionManager(this);
        Class<?> dest = session.isLoggedIn() ? MainActivity.class : LoginActivity.class;

        startActivity(new Intent(this, dest));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (pulseAnim != null) pulseAnim.cancel();
    }
}
