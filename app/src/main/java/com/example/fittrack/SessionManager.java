package com.example.fittrack;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    private static final String PREF_NAME = "FitTrackSession";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USERNAME = "username";

    private final SharedPreferences pref;
    private final SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        pref = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    /**
     * Create login session.
     */
    public void createLoginSession(long userId, String username) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putLong(KEY_USER_ID, userId);
        editor.putString(KEY_USERNAME, username);
        editor.apply();
    }

    /**
     * Check if user is logged in.
     */
    public boolean isLoggedIn() {
        return pref.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    /**
     * Get logged-in user ID.
     */
    public long getUserId() {
        return pref.getLong(KEY_USER_ID, -1);
    }

    /**
     * Get logged-in username.
     */
    public String getUsername() {
        return pref.getString(KEY_USERNAME, "");
    }

    /**
     * Clear session details on logout.
     */
    public void logout() {
        editor.clear();
        editor.apply();
    }
}
