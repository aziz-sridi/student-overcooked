package com.student.overcooked.data.model;

import java.util.Date;
import java.util.Objects;
import java.util.UUID;

/**
 * Group member with role info
 */
public class GroupMember {
    private String id;
    private String groupId;
    private String userId;
    private String userName;
    private String userEmail;
    private GroupRole role;
    private Date joinedAt;
    private boolean isPending;

    public GroupMember() {
        this.id = UUID.randomUUID().toString();
        this.groupId = "";
        this.userId = "";
        this.userName = "";
        this.userEmail = "";
        this.role = GroupRole.MEMBER;
        this.joinedAt = new Date();
        this.isPending = false;
    }

    public GroupMember(String id, String groupId, String userId, String userName,
                       String userEmail, GroupRole role, Date joinedAt, boolean isPending) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.groupId = groupId;
        this.userId = userId;
        this.userName = userName;
        this.userEmail = userEmail;
        this.role = role != null ? role : GroupRole.MEMBER;
        this.joinedAt = joinedAt != null ? joinedAt : new Date();
        this.isPending = isPending;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public GroupRole getRole() { return role; }
    public void setRole(GroupRole role) { this.role = role; }

    public Date getJoinedAt() { return joinedAt; }
    public void setJoinedAt(Date joinedAt) { this.joinedAt = joinedAt; }

    public boolean isPending() { return isPending; }
    public void setPending(boolean pending) { isPending = pending; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GroupMember that = (GroupMember) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
