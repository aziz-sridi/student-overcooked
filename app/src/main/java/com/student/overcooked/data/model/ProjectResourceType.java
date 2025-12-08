package com.student.overcooked.data.model;

/**
 * Types of resources an individual project can store
 */
public enum ProjectResourceType {
    FILE("File"),
    LINK("Link"),
    NOTE("Note");

    private final String displayName;

    ProjectResourceType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static ProjectResourceType fromString(String value) {
        if (value == null) {
            return NOTE;
        }
        for (ProjectResourceType type : values()) {
            if (type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        return NOTE;
    }
}
