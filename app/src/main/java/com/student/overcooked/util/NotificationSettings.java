package com.student.overcooked.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

public final class NotificationSettings {

    private static final String PREFS = "notification_settings";
    private static final String KEY_ENABLED = "enabled";

    private NotificationSettings() {
    }

    public static boolean areNotificationsEnabled(@NonNull Context context) {
        return prefs(context).getBoolean(KEY_ENABLED, false);
    }

    public static void setNotificationsEnabled(@NonNull Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply();
    }

    private static SharedPreferences prefs(@NonNull Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
