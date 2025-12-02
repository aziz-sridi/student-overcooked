package com.example.overcooked.data.model;

/**
 * Cooked Meter levels representing student stress/workload
 * 
 * Meter States:
 * - Cozy (0–30%): Low workload, student is relaxed
 * - Crispy (31–60%): Moderate workload, some pressure
 * - Cooked (61–85%): High workload, significant stress
 * - Overcooked (86–100%): Critical workload, maximum stress
 */
public enum CookedLevel {
    COZY(0, 30, "Cozy", "😌", "#4CAF50"),
    CRISPY(31, 60, "Crispy", "☕", "#FFD93D"),
    COOKED(61, 85, "Cooked", "🔥", "#FF9800"),
    OVERCOOKED(86, 100, "Overcooked", "💀", "#FF3B30");

    private final int minPercentage;
    private final int maxPercentage;
    private final String displayName;
    private final String emoji;
    private final String colorHex;

    CookedLevel(int minPercentage, int maxPercentage, String displayName, String emoji, String colorHex) {
        this.minPercentage = minPercentage;
        this.maxPercentage = maxPercentage;
        this.displayName = displayName;
        this.emoji = emoji;
        this.colorHex = colorHex;
    }

    public int getMinPercentage() {
        return minPercentage;
    }

    public int getMaxPercentage() {
        return maxPercentage;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getEmoji() {
        return emoji;
    }

    public String getColorHex() {
        return colorHex;
    }

    /**
     * Get the cooked level based on percentage
     */
    public static CookedLevel fromPercentage(int percentage) {
        if (percentage <= 30) return COZY;
        if (percentage <= 60) return CRISPY;
        if (percentage <= 85) return COOKED;
        return OVERCOOKED;
    }

    /**
     * Get motivational message for this level
     */
    public String getMotivationalMessage() {
        switch (this) {
            case COZY:
                return "You're crushing it! Keep the momentum going! 💪";
            case CRISPY:
                return "Coffee doesn't ask silly questions. Coffee understands. ☕";
            case COOKED:
                return "Breathe. You've survived worse deadlines... probably. 😰";
            case OVERCOOKED:
                return "SOS! Time to call in the study group! 🆘";
            default:
                return "";
        }
    }

    /**
     * Get status text for this level
     */
    public String getStatusText() {
        switch (this) {
            case COZY:
                return "You're chillin'! Keep it up! 😌";
            case CRISPY:
                return "Getting a little warm in here... ☕";
            case COOKED:
                return "Things are heating up! 🔥";
            case OVERCOOKED:
                return "Emergency mode activated! 💀";
            default:
                return "";
        }
    }

    /**
     * Get short status for meter display
     */
    public String getShortStatus() {
        switch (this) {
            case COZY:
                return "Cozy";
            case CRISPY:
                return "Crispy";
            case COOKED:
                return "Cooked";
            case OVERCOOKED:
                return "Overcooked";
            default:
                return "";
        }
    }
}
