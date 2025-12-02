package com.example.overcooked.data.model;

/**
 * Priority levels for tasks
 * Used to determine task importance and affects Cooked Meter calculation
 */
public enum Priority {
    LOW(1, "Low"),
    MEDIUM(2, "Medium"),
    HIGH(3, "High");

    private final int weight;
    private final String displayName;

    Priority(int weight, String displayName) {
        this.weight = weight;
        this.displayName = displayName;
    }

    public int getWeight() {
        return weight;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static Priority fromString(String value) {
        for (Priority p : values()) {
            if (p.name().equalsIgnoreCase(value)) {
                return p;
            }
        }
        return MEDIUM;
    }
}
