package com.student.overcooked.data.model;

/**
 * Roles a team member can have in a project
 */
public enum MemberRole {
    ADMIN("Admin"),
    MEMBER("Member"),
    CONTRIBUTOR("Contributor");

    private final String displayName;

    MemberRole(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
