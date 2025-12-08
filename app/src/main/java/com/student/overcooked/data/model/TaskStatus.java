package com.student.overcooked.data.model;

/**
 * Status of a task
 */
public enum TaskStatus {
    NOT_STARTED("Not Started"),
    IN_PROGRESS("In Progress"),
    DONE("Done");

    private final String displayName;

    TaskStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static TaskStatus fromString(String value) {
        if (value == null) return NOT_STARTED;
        try {
            return TaskStatus.valueOf(value.toUpperCase().replace(" ", "_"));
        } catch (IllegalArgumentException e) {
            return NOT_STARTED;
        }
    }
}
