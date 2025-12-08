package com.student.overcooked.util;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Persists lightweight session metadata (current Firebase user id) so we can
 * clear local caches when a different user signs in on the same device.
 */
public class SessionManager {

    private static final String PREF_NAME = "session_prefs";
    private static final String KEY_LAST_USER_ID = "last_user_id";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        this.prefs = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public String getLastUserId() {
        return prefs.getString(KEY_LAST_USER_ID, null);
    }

    public void setLastUserId(String userId) {
        prefs.edit().putString(KEY_LAST_USER_ID, userId).apply();
    }

    public void clear() {
        prefs.edit().remove(KEY_LAST_USER_ID).apply();
    }
}
