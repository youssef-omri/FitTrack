package com.example.fittrack;

import android.app.Application;

/**
 * Application class that applies the persisted Light/Dark mode globally on startup.
 */
public class FitTrackApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        ThemeManager.applySavedTheme(this);
    }
}
