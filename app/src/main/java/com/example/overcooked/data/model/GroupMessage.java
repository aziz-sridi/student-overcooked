package com.example.overcooked.data.model;

import java.util.Date;
import java.util.Objects;
import java.util.UUID;

/**
 * Chat message in a group
 */
public class GroupMessage {
    private String id;
    private String groupId;
    private String senderId;
    private String senderName;
    private String message;
    private Date timestamp;

    public GroupMessage() {
        this.id = UUID.randomUUID().toString();
        this.groupId = "";
        this.senderId = "";
        this.senderName = "";
        this.message = "";
        this.timestamp = new Date();
    }

    public GroupMessage(String id, String groupId, String senderId, String senderName,
                        String message, Date timestamp) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.groupId = groupId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.message = message;
        this.timestamp = timestamp != null ? timestamp : new Date();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GroupMessage that = (GroupMessage) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
