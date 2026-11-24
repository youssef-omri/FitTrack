package com.example.fittrack;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

/**
 * Manages global Dark / Light theme preference via SharedPreferences and AppCompatDelegate.
 */
public final class ThemeManager {

    private static final String PREF_NAME = "FitTrackThemePrefs";
    private static final String KEY_DARK_MODE = "is_dark_mode";

    private ThemeManager() {
    }

    public static boolean isDarkMode(Context context) {
        SharedPreferences prefs = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_DARK_MODE, false);
    }

    public static void setDarkMode(Context context, boolean darkMode) {
        SharedPreferences prefs = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_DARK_MODE, darkMode).apply();
        applyNightMode(darkMode);
    }

    public static void applySavedTheme(Context context) {
        applyNightMode(isDarkMode(context));
    }

    private static void applyNightMode(boolean darkMode) {
        int desiredMode = darkMode
                ? AppCompatDelegate.MODE_NIGHT_YES
                : AppCompatDelegate.MODE_NIGHT_NO;
        if (AppCompatDelegate.getDefaultNightMode() != desiredMode) {
            AppCompatDelegate.setDefaultNightMode(desiredMode);
        }
    }
}
