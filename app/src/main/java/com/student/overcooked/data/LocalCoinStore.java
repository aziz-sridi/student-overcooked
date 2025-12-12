package com.student.overcooked.data;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Simple local coin storage for offline-first behavior.
 * Stores current coin balance mirror and a pending delta to sync when online.
 */
public class LocalCoinStore {
    public static final String PREFS = "local_coins";
    public static final String KEY_BALANCE = "balance";
    public static final String KEY_PENDING_DELTA = "pending_delta";

    private final SharedPreferences prefs;

    public LocalCoinStore(Context context) {
        this.prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public int getBalance() { return prefs.getInt(KEY_BALANCE, 0); }

    public int getPendingDelta() { return prefs.getInt(KEY_PENDING_DELTA, 0); }

    public void addCoins(int delta) {
        if (delta == 0) return;
        int newBalance = Math.max(0, getBalance() + delta);
        int newPending = getPendingDelta() + delta;
        prefs.edit()
                .putInt(KEY_BALANCE, newBalance)
                .putInt(KEY_PENDING_DELTA, newPending)
                .apply();
    }

    public void setBalanceFromServer(int balance) {
        prefs.edit()
                .putInt(KEY_BALANCE, Math.max(0, balance))
                .putInt(KEY_PENDING_DELTA, 0)
                .apply();
    }

    public void clearPendingDelta() {
        prefs.edit().putInt(KEY_PENDING_DELTA, 0).apply();
    }
}
