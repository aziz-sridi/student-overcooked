package com.student.overcooked.ui;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.student.overcooked.OvercookedApplication;
import com.student.overcooked.R;
import com.student.overcooked.data.LocalCoinStore;
import com.student.overcooked.data.model.Mascot;
import com.student.overcooked.data.model.ShopItem;
import com.student.overcooked.data.model.User;
import com.student.overcooked.data.repository.UserRepository;
import com.student.overcooked.ui.adapter.ShopAdapter;
import com.student.overcooked.ui.dialog.MascotPreviewDialog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ShopActivity extends AppCompatActivity {

    private TextView coinBalanceText;
    private RecyclerView shopItemsRecycler;
    private ShopAdapter adapter;
    private UserRepository userRepository;
    private User currentUser;
    private List<Mascot> mascots = new ArrayList<>();

    private LocalCoinStore localCoinStore;
    private int lastRemoteCoins = 0;
    private SharedPreferences.OnSharedPreferenceChangeListener coinPrefsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shop);

        userRepository = ((OvercookedApplication) getApplication()).getUserRepository();
        localCoinStore = new LocalCoinStore(this);

        initializeViews();
        setupAdapter();
        observeUser();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (coinPrefsListener == null) {
            coinPrefsListener = (prefs, key) -> updateCoinText();
        }
        getSharedPreferences(LocalCoinStore.PREFS, MODE_PRIVATE)
                .registerOnSharedPreferenceChangeListener(coinPrefsListener);
        updateCoinText();
    }

    @Override
    protected void onStop() {
        if (coinPrefsListener != null) {
            getSharedPreferences(LocalCoinStore.PREFS, MODE_PRIVATE)
                    .unregisterOnSharedPreferenceChangeListener(coinPrefsListener);
        }
        super.onStop();
    }

    private void initializeViews() {
        coinBalanceText = findViewById(R.id.coinBalanceText);
        shopItemsRecycler = findViewById(R.id.shopItemsRecycler);
        findViewById(R.id.backButton).setOnClickListener(v -> finish());
    }

    private void setupAdapter() {
        adapter = new ShopAdapter(this::showMascotPreview);
        shopItemsRecycler.setLayoutManager(new GridLayoutManager(this, 2));
        shopItemsRecycler.setAdapter(adapter);
    }

    private void observeUser() {
        userRepository.getCurrentUser().observe(this, user -> {
            if (user != null) {
                currentUser = user;
                lastRemoteCoins = user.getCoins();
                if (localCoinStore != null && localCoinStore.getPendingDelta() == 0) {
                    localCoinStore.setBalanceFromServer(lastRemoteCoins);
                }
                updateCoinText();
                updateShopItems();
            }
        });
    }

    private void updateCoinText() {
        if (coinBalanceText == null || localCoinStore == null) return;
        int display = localCoinStore.getPendingDelta() != 0
                ? localCoinStore.getBalance()
                : lastRemoteCoins;
        coinBalanceText.setText(String.valueOf(Math.max(0, display)));
    }

    private void updateShopItems() {
        List<Mascot> mascotsList = new ArrayList<>();

        // Giga Toast
        mascotsList.add(new Mascot(
                "giga_toast",
                "Giga Toast",
                250,
                R.drawable.giga_cozy_toast,
                Arrays.asList(R.drawable.giga_cozy_toast, R.drawable.giga_crispy_toast, R.drawable.giga_cooked_toast, R.drawable.giga_overcooked_toast),
                new String[]{"Cozy", "Crispy", "Cooked", "Overcooked"}
        ));

        // Student
        mascotsList.add(new Mascot(
                "student",
                "Student",
                200,
            R.drawable.student_cozy,
            Arrays.asList(R.drawable.student_cozy, R.drawable.student_crispy, R.drawable.student_cooked, R.drawable.student_overcooked),
                new String[]{"Cozy", "Crispy", "Cooked", "Overcooked"}
        ));

        // Potato
        mascotsList.add(new Mascot(
                "potato",
                "Potato",
                180,
            R.drawable.potato_cozy,
            Arrays.asList(R.drawable.potato_cozy, R.drawable.potato_crispy, R.drawable.potato_cooked, R.drawable.potato_overcooked),
                new String[]{"Cozy", "Crispy", "Cooked", "Overcooked"}
        ));

        // Convert to ShopItems for adapter compatibility
        List<ShopItem> items = new ArrayList<>();
        if (currentUser != null && currentUser.getInventory() != null) {
            for (Mascot mascot : mascotsList) {
                ShopItem item = new ShopItem(mascot.getId(), mascot.getName(), mascot.getPrice(), mascot.getThumbnailResId());
                if (currentUser.getInventory().contains(mascot.getId())) {
                    item.setPurchased(true);
                }
                items.add(item);
            }
        } else {
            for (Mascot mascot : mascotsList) {
                items.add(new ShopItem(mascot.getId(), mascot.getName(), mascot.getPrice(), mascot.getThumbnailResId()));
            }
        }

        // Store mascots for dialog
        this.mascots = mascotsList;
        adapter.submitList(items);
    }

    private void showMascotPreview(ShopItem item) {
        // Find the mascot object for this item
        Mascot mascot = null;
        for (Mascot m : mascots) {
            if (m.getId().equals(item.getId())) {
                mascot = m;
                break;
            }
        }
        if (mascot == null) return;

        // Show preview dialog
        MascotPreviewDialog dialog = new MascotPreviewDialog(this, mascot, purchasedMascot -> {
            purchaseItem(item);
        });
        dialog.show();
    }

    private void purchaseItem(ShopItem item) {
        if (currentUser == null) return;

        int available = localCoinStore != null ? localCoinStore.getBalance() : currentUser.getCoins();
        if (available >= item.getPrice()) {
            int newBalance = available - item.getPrice();
            
            // Optimistic update
            currentUser.setCoins(newBalance);
            lastRemoteCoins = newBalance;
            if (localCoinStore != null) {
                // Online purchase: keep local mirror aligned and clear any pending delta.
                localCoinStore.setBalanceFromServer(newBalance);
            }
            updateCoinText();

            Log.d("ShopActivity", "Purchasing " + item.getId() + " for " + item.getPrice() + " coins");
            
            userRepository.updateUserCoins(newBalance, 
                aVoid -> {
                    Log.d("ShopActivity", "Coins updated to " + newBalance);
                    userRepository.addItemToInventory(item.getId(), 
                        aVoid2 -> {
                            Log.d("ShopActivity", "Inventory updated with " + item.getId());
                            Toast.makeText(this, "Purchased " + item.getName() + "!", Toast.LENGTH_SHORT).show();
                            // Inventory update will trigger observer
                        },
                        e -> {
                            Log.e("ShopActivity", "Failed to add item", e);
                            Toast.makeText(this, "Failed to add item: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    );
                },
                e -> {
                    Log.e("ShopActivity", "Purchase failed", e);
                    // Roll back local mirror; server value will also re-sync via observer.
                    if (localCoinStore != null) {
                        localCoinStore.setBalanceFromServer(Math.max(0, available));
                    }
                    lastRemoteCoins = Math.max(0, available);
                    updateCoinText();
                    Toast.makeText(this, "Purchase failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            );
        } else {
            Toast.makeText(this, "Not enough coins!", Toast.LENGTH_SHORT).show();
        }
    }
}
