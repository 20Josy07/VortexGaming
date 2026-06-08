package com.localcode.vortexgaming.utils;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.HashSet;
import java.util.Set;

public class FavoritesManager {
    private static final String PREF_NAME = "vortex_favorites";
    private static final String KEY_FAVORITES = "favorites_list";
    private final SharedPreferences sharedPreferences;

    public FavoritesManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void toggleFavorite(String gameId) {
        Set<String> favorites = getFavorites();
        if (favorites.contains(gameId)) {
            favorites.remove(gameId);
        } else {
            favorites.add(gameId);
        }
        sharedPreferences.edit().putStringSet(KEY_FAVORITES, favorites).apply();
    }

    public boolean isFavorite(String gameId) {
        return getFavorites().contains(gameId);
    }

    public int getFavoritesCount() {
        return getFavorites().size();
    }

    private Set<String> getFavorites() {
        return new HashSet<>(sharedPreferences.getStringSet(KEY_FAVORITES, new HashSet<>()));
    }
}
