package com.localcode.vortexgaming.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.localcode.vortexgaming.models.UserModel;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class UserService {
    private static final String PREFS   = "vortex_users";
    private static final String KEY     = "users_list";
    private static final Gson GSON      = new Gson();

    public enum LoginResult { SUCCESS, WRONG_PASSWORD, NOT_FOUND }
    public enum RegisterResult { SUCCESS, EMAIL_TAKEN }

    private final SharedPreferences prefs;

    public UserService(Context ctx) {
        prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private List<UserModel> getAll() {
        String json = prefs.getString(KEY, null);
        if (json == null) return new ArrayList<>();
        Type type = new TypeToken<List<UserModel>>() {}.getType();
        List<UserModel> list = GSON.fromJson(json, type);
        return list != null ? list : new ArrayList<>();
    }

    private void saveAll(List<UserModel> users) {
        prefs.edit().putString(KEY, GSON.toJson(users)).apply();
    }

    public RegisterResult register(UserModel user) {
        List<UserModel> users = getAll();
        for (UserModel u : users) {
            if (u.email.equalsIgnoreCase(user.email)) return RegisterResult.EMAIL_TAKEN;
        }
        users.add(user);
        saveAll(users);
        return RegisterResult.SUCCESS;
    }

    public LoginResult login(String email, String password) {
        for (UserModel u : getAll()) {
            if (u.email.equalsIgnoreCase(email)) {
                return u.password.equals(password) ? LoginResult.SUCCESS : LoginResult.WRONG_PASSWORD;
            }
        }
        return LoginResult.NOT_FOUND;
    }

    public UserModel findByEmail(String email) {
        for (UserModel u : getAll()) {
            if (u.email.equalsIgnoreCase(email)) return u;
        }
        return null;
    }
}
