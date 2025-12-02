package com.example.overcooked.data.model;

/**
 * User data class for authentication
 * Used with Firebase Auth
 */
public class User {
    private String uid;
    private String email;
    private String username;
    private String displayName;
    private String photoUrl;
    private long createdAt;
    private int coins;
    private java.util.List<String> inventory;

    public User() {
        this.uid = "";
        this.email = "";
        this.username = "";
        this.displayName = "";
        this.photoUrl = null;
        this.createdAt = System.currentTimeMillis();
        this.coins = 0;
        this.inventory = new java.util.ArrayList<>();
    }

    public User(String uid, String email, String username, String displayName, String photoUrl, long createdAt) {
        this.uid = uid != null ? uid : "";
        this.email = email != null ? email : "";
        this.username = username != null ? username : "";
        this.displayName = displayName != null ? displayName : "";
        this.photoUrl = photoUrl;
        this.createdAt = createdAt;
        this.coins = 0;
        this.inventory = new java.util.ArrayList<>();
    }

    // Getters and Setters
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public int getCoins() { return coins; }
    public void setCoins(int coins) { this.coins = coins; }

    public java.util.List<String> getInventory() { return inventory; }
    public void setInventory(java.util.List<String> inventory) { this.inventory = inventory; }
}
