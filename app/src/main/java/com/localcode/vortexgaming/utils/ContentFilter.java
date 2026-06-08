package com.localcode.vortexgaming.utils;

import android.content.Context;
import android.content.SharedPreferences;
import com.localcode.vortexgaming.models.Game;
import java.util.ArrayList;
import java.util.List;

public class ContentFilter {

    private static final String PREF_NAME = "vortex_settings";
    private static final String KEY_ENABLED = "content_filter_enabled";

    private static final String[] BLOCKED = {
        "porn", "sex", "hentai", "adult", "erotic", "nsfw", "xxx", "nude", "strip", "playboy"
    };

    public static boolean isEnabled(Context ctx) {
        return prefs(ctx).getBoolean(KEY_ENABLED, true);
    }

    public static void setEnabled(Context ctx, boolean enabled) {
        prefs(ctx).edit().putBoolean(KEY_ENABLED, enabled).apply();
    }

    /** Returns a new list with adult-named and no-cover games removed. */
    public static List<Game> apply(Context ctx, List<Game> games) {
        boolean filterActive = isEnabled(ctx);
        List<Game> result = new ArrayList<>();
        for (Game g : games) {
            if (g.background_image == null || g.background_image.isEmpty()) continue;
            if (filterActive && isBlocked(g.name)) continue;
            result.add(g);
        }
        return result;
    }

    private static boolean isBlocked(String name) {
        if (name == null) return true;
        String lower = name.toLowerCase();
        for (String word : BLOCKED) {
            if (lower.contains(word)) return true;
        }
        return false;
    }

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
}
