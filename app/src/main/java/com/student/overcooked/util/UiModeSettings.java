package com.student.overcooked.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

public final class UiModeSettings {

    private static final String PREFS = "ui_settings";
    private static final String KEY_DARK_MODE = "dark_mode";

    private UiModeSettings() {
    }

    public static boolean isDarkModeEnabled(@NonNull Context context) {
        return prefs(context).getBoolean(KEY_DARK_MODE, false);
    }

    public static void setDarkModeEnabled(@NonNull Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_DARK_MODE, enabled).apply();
    }

    private static SharedPreferences prefs(@NonNull Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
