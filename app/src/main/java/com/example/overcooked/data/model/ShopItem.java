package com.example.overcooked.data.model;

public class ShopItem {
    private String id;
    private String name;
    private int price;
    private int imageResId;
    private boolean purchased;

    public ShopItem(String id, String name, int price, int imageResId) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.imageResId = imageResId;
        this.purchased = false;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public int getPrice() { return price; }
    public int getImageResId() { return imageResId; }
    public boolean isPurchased() { return purchased; }
    public void setPurchased(boolean purchased) { this.purchased = purchased; }
}
