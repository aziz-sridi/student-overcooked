package com.example.overcooked.ui.adapter;

/**
 * Data class for Quick Stat items in Home Fragment
 */
public class QuickStatItem {
    private String label;
    private String value;
    private int iconRes;
    private int colorRes;

    public QuickStatItem(String label, String value, int iconRes, int colorRes) {
        this.label = label;
        this.value = value;
        this.iconRes = iconRes;
        this.colorRes = colorRes;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public int getIconRes() {
        return iconRes;
    }

    public void setIconRes(int iconRes) {
        this.iconRes = iconRes;
    }

    public int getColorRes() {
        return colorRes;
    }

    public void setColorRes(int colorRes) {
        this.colorRes = colorRes;
    }
}
