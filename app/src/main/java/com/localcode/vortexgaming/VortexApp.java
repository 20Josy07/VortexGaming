package com.localcode.vortexgaming;

import android.app.Application;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;

public class VortexApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Apply saved theme before any activity is inflated
        SharedPreferences prefs = getSharedPreferences("vortex_session", MODE_PRIVATE);
        boolean isDark = prefs.getBoolean("theme_dark", true);
        AppCompatDelegate.setDefaultNightMode(
                isDark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
    }
}
