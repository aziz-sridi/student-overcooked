package com.example.overcooked.ui;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.overcooked.OvercookedApplication;
import com.example.overcooked.R;
import com.example.overcooked.data.model.ShopItem;
import com.example.overcooked.data.model.User;
import com.example.overcooked.data.repository.UserRepository;
import com.example.overcooked.ui.adapter.ShopAdapter;

import java.util.ArrayList;
import java.util.List;

public class ShopActivity extends AppCompatActivity {

    private TextView coinBalanceText;
    private RecyclerView shopItemsRecycler;
    private ShopAdapter adapter;
    private UserRepository userRepository;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shop);

        userRepository = ((OvercookedApplication) getApplication()).getUserRepository();

        initializeViews();
        setupAdapter();
        observeUser();
    }

    private void initializeViews() {
        coinBalanceText = findViewById(R.id.coinBalanceText);
        shopItemsRecycler = findViewById(R.id.shopItemsRecycler);
        findViewById(R.id.backButton).setOnClickListener(v -> finish());
    }

    private void setupAdapter() {
        adapter = new ShopAdapter(this::purchaseItem);
        shopItemsRecycler.setLayoutManager(new GridLayoutManager(this, 2));
        shopItemsRecycler.setAdapter(adapter);
    }

    private void observeUser() {
        userRepository.getCurrentUser().observe(this, user -> {
            if (user != null) {
                currentUser = user;
                coinBalanceText.setText(String.valueOf(user.getCoins()));
                updateShopItems();
            }
        });
    }

    private void updateShopItems() {
        List<ShopItem> items = new ArrayList<>();
        // Add Gigatoast
        items.add(new ShopItem("gigatoast", "Gigatoast", 500, R.drawable.ic_avatar_placeholder));
        items.add(new ShopItem("chef_hat", "Chef Hat", 200, R.drawable.ic_avatar_placeholder));
        items.add(new ShopItem("golden_spatula", "Golden Spatula", 1000, R.drawable.ic_avatar_placeholder));

        // Check inventory
        if (currentUser != null && currentUser.getInventory() != null) {
            for (ShopItem item : items) {
                if (currentUser.getInventory().contains(item.getId())) {
                    item.setPurchased(true);
                }
            }
        }

        adapter.submitList(items);
    }

    private void purchaseItem(ShopItem item) {
        if (currentUser == null) return;

        if (currentUser.getCoins() >= item.getPrice()) {
            int newBalance = currentUser.getCoins() - item.getPrice();
            
            // Optimistic update
            currentUser.setCoins(newBalance);
            coinBalanceText.setText(String.valueOf(newBalance));

            userRepository.updateUserCoins(newBalance, 
                aVoid -> {
                    userRepository.addItemToInventory(item.getId(), 
                        aVoid2 -> {
                            Toast.makeText(this, "Purchased " + item.getName() + "!", Toast.LENGTH_SHORT).show();
                            // Inventory update will trigger observer
                        },
                        e -> Toast.makeText(this, "Failed to add item: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
                },
                e -> Toast.makeText(this, "Purchase failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
            );
        } else {
            Toast.makeText(this, "Not enough coins!", Toast.LENGTH_SHORT).show();
        }
    }
}
