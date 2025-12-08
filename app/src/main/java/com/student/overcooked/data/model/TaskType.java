package com.student.overcooked.data.model;

/**
 * Types of tasks a student can create
 */
public enum TaskType {
    HOMEWORK("Homework"),
    ASSIGNMENT("Assignment"),
    EXAM("Exam"),
    PROJECT("Project"),
    OTHER("Other");

    private final String displayName;

    TaskType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static TaskType fromString(String value) {
        for (TaskType t : values()) {
            if (t.name().equalsIgnoreCase(value)) {
                return t;
            }
        }
        return OTHER;
    }
}
