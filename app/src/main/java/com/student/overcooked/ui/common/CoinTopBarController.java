package com.student.overcooked.ui.common;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import com.student.overcooked.R;
import com.student.overcooked.data.LocalCoinStore;
import com.student.overcooked.data.repository.UserRepository;
import com.student.overcooked.ui.ShopActivity;

/**
 * Handles the shared "coin top bar" UI:
 * - Sets initial coin text
 * - Opens Shop when the card is tapped
 * - Keeps the displayed coin balance in sync with LocalCoinStore + remote user coins
 */
public final class CoinTopBarController implements DefaultLifecycleObserver {

    private final Fragment fragment;
    private final LocalCoinStore localCoinStore;
    private final @Nullable UserRepository userRepository;

    private @Nullable TextView coinScoreText;
    private int lastRemoteCoins;

    private @Nullable SharedPreferences.OnSharedPreferenceChangeListener prefsListener;

    public CoinTopBarController(
            @NonNull Fragment fragment,
            @NonNull LocalCoinStore localCoinStore,
            @Nullable UserRepository userRepository
    ) {
        this.fragment = fragment;
        this.localCoinStore = localCoinStore;
        this.userRepository = userRepository;
        this.lastRemoteCoins = localCoinStore.getBalance();
    }

    public void bind(@NonNull View root) {
        coinScoreText = root.findViewById(R.id.coinScoreText);
        View coinScoreCard = root.findViewById(R.id.coinScoreCard);

        if (coinScoreCard != null) {
            coinScoreCard.setOnClickListener(v -> {
                Context ctx = fragment.requireContext();
                fragment.startActivity(new Intent(ctx, ShopActivity.class));
            });
        }

        fragment.getViewLifecycleOwner().getLifecycle().addObserver(this);

        if (userRepository != null) {
            userRepository.getCurrentUser().observe(fragment.getViewLifecycleOwner(), user -> {
                if (user == null) return;
                lastRemoteCoins = user.getCoins();
                if (localCoinStore.getPendingDelta() == 0) {
                    localCoinStore.setBalanceFromServer(lastRemoteCoins);
                }
                updateCoinText();
            });
        }

        updateCoinText();
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        if (prefsListener == null) {
            prefsListener = (prefs, key) -> updateCoinText();
        }

        fragment.requireContext()
                .getSharedPreferences(LocalCoinStore.PREFS, Context.MODE_PRIVATE)
                .registerOnSharedPreferenceChangeListener(prefsListener);

        updateCoinText();
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        if (prefsListener != null) {
            fragment.requireContext()
                    .getSharedPreferences(LocalCoinStore.PREFS, Context.MODE_PRIVATE)
                    .unregisterOnSharedPreferenceChangeListener(prefsListener);
        }
    }

    private void updateCoinText() {
        if (coinScoreText == null) return;

        int display;
        if (userRepository == null) {
            display = localCoinStore.getBalance();
        } else {
            display = localCoinStore.getPendingDelta() != 0
                    ? localCoinStore.getBalance()
                    : lastRemoteCoins;
        }

        coinScoreText.setText(String.valueOf(Math.max(0, display)));
    }
}
