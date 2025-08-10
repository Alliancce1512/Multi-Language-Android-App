package com.shellytask.app.legacy;

import android.content.Context;
import android.content.SharedPreferences;

public class UserSessionManager {

    private static final String PREFERENCES_NAME    = "legacy_session_prefs";
    private static final String KEY_USERNAME        = "key_username";
    private static final String KEY_IS_LOGGED_IN    = "key_is_logged_in";

    private final SharedPreferences preferences;

    public UserSessionManager(Context context) {
        this.preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    public void saveLogin(String username) {
        preferences.edit()
                .putString(KEY_USERNAME, username)
                .putBoolean(KEY_IS_LOGGED_IN, true)
                .apply();
    }

    public void logout() {
        preferences.edit().clear().apply();
    }

    public boolean isLoggedIn() {
        return preferences.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public String getUsername() {
        return preferences.getString(KEY_USERNAME, "Guest");
    }
}