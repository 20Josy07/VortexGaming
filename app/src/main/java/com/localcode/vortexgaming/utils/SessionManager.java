package com.localcode.vortexgaming.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREF_NAME = "vortex_session";
    private static final String KEY_IS_LOGGED_IN    = "is_logged_in";
    private static final String KEY_USERNAME        = "username";
    private static final String KEY_EMAIL           = "email";
    private static final String KEY_DOB             = "dob";
    private static final String KEY_TOKEN           = "auth_token";
    private static final String KEY_THEME_DARK      = "theme_dark";
    private static final String KEY_NOTIFICATIONS   = "notifications_enabled";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void setLoggedIn(boolean v) { prefs.edit().putBoolean(KEY_IS_LOGGED_IN, v).apply(); }
    public boolean isLoggedIn()        { return prefs.getBoolean(KEY_IS_LOGGED_IN, false); }

    public void saveUsername(String name)  { prefs.edit().putString(KEY_USERNAME, name).apply(); }
    public String getUsername()            { return prefs.getString(KEY_USERNAME, "Gamer"); }

    public void saveEmail(String email)    { prefs.edit().putString(KEY_EMAIL, email).apply(); }
    public String getEmail()               { return prefs.getString(KEY_EMAIL, "player@vortex.gg"); }

    public void saveDob(String dob)        { prefs.edit().putString(KEY_DOB, dob).apply(); }
    public String getDob()                 { return prefs.getString(KEY_DOB, ""); }

    public void saveToken(String token)    { prefs.edit().putString(KEY_TOKEN, token).apply(); }
    public String getToken()               { return prefs.getString(KEY_TOKEN, ""); }
    public String getBearerToken()         { return "Bearer " + prefs.getString(KEY_TOKEN, ""); }

    public void setThemeDark(boolean dark) { prefs.edit().putBoolean(KEY_THEME_DARK, dark).apply(); }
    public boolean isThemeDark()           { return prefs.getBoolean(KEY_THEME_DARK, true); }

    public void setNotificationsEnabled(boolean v) { prefs.edit().putBoolean(KEY_NOTIFICATIONS, v).apply(); }
    public boolean isNotificationsEnabled()        { return prefs.getBoolean(KEY_NOTIFICATIONS, true); }

    public void clearSession() { prefs.edit().clear().apply(); }
}
