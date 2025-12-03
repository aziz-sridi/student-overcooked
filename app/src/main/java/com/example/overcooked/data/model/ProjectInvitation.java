package com.example.overcooked.data.model;

import androidx.annotation.NonNull;

import java.util.Date;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents an invitation to join a project/group
 */
public class ProjectInvitation {
    @NonNull
    private String id;
    private String groupId;
    private String groupName;
    private String invitedUserId;
    private String invitedUserEmail;
    private String invitedByUserId;
    private String invitedByUserName;
    private Date createdAt;
    private InvitationStatus status;

    public enum InvitationStatus {
        PENDING,
        ACCEPTED,
        DECLINED
    }

    public ProjectInvitation() {
        this.id = UUID.randomUUID().toString();
        this.groupId = "";
        this.groupName = "";
        this.invitedUserId = "";
        this.invitedUserEmail = "";
        this.invitedByUserId = "";
        this.invitedByUserName = "";
        this.createdAt = new Date();
        this.status = InvitationStatus.PENDING;
    }

    public ProjectInvitation(@NonNull String id, String groupId, String groupName,
                             String invitedUserId, String invitedUserEmail,
                             String invitedByUserId, String invitedByUserName,
                             Date createdAt, InvitationStatus status) {
        this.id = id;
        this.groupId = groupId != null ? groupId : "";
        this.groupName = groupName != null ? groupName : "";
        this.invitedUserId = invitedUserId != null ? invitedUserId : "";
        this.invitedUserEmail = invitedUserEmail != null ? invitedUserEmail : "";
        this.invitedByUserId = invitedByUserId != null ? invitedByUserId : "";
        this.invitedByUserName = invitedByUserName != null ? invitedByUserName : "";
        this.createdAt = createdAt != null ? createdAt : new Date();
        this.status = status != null ? status : InvitationStatus.PENDING;
    }

    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }

    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }

    public String getInvitedUserId() { return invitedUserId; }
    public void setInvitedUserId(String invitedUserId) { this.invitedUserId = invitedUserId; }

    public String getInvitedUserEmail() { return invitedUserEmail; }
    public void setInvitedUserEmail(String invitedUserEmail) { this.invitedUserEmail = invitedUserEmail; }

    public String getInvitedByUserId() { return invitedByUserId; }
    public void setInvitedByUserId(String invitedByUserId) { this.invitedByUserId = invitedByUserId; }

    public String getInvitedByUserName() { return invitedByUserName; }
    public void setInvitedByUserName(String invitedByUserName) { this.invitedByUserName = invitedByUserName; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public InvitationStatus getStatus() { return status; }
    public void setStatus(InvitationStatus status) { this.status = status; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProjectInvitation)) return false;
        ProjectInvitation that = (ProjectInvitation) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
