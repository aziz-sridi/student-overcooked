package com.example.overcooked.data.model;

import androidx.annotation.NonNull;

import java.util.Date;
import java.util.Objects;
import java.util.UUID;

/**
 * Resource stored within a project (individual workspace)
 */
public class ProjectResource {
    @NonNull
    private String id;
    private String groupId;
    private ProjectResourceType type;
    private String title;
    private String content;
    private String createdBy;
    private Date createdAt;
    private String fileUrl;
    private String fileMimeType;
    private String fileName;
    private long fileSizeBytes;
    private String storagePath;

    public ProjectResource() {
        this.id = UUID.randomUUID().toString();
        this.groupId = "";
        this.type = ProjectResourceType.NOTE;
        this.title = "";
        this.content = "";
        this.createdBy = "";
        this.createdAt = new Date();
        this.fileUrl = "";
        this.fileMimeType = "";
        this.fileName = "";
        this.fileSizeBytes = 0L;
        this.storagePath = "";
    }

    public ProjectResource(@NonNull String id,
                           String groupId,
                           ProjectResourceType type,
                           String title,
                           String content,
                           String createdBy,
                           Date createdAt,
                           String fileUrl,
                           String fileMimeType,
                           String fileName,
                           long fileSizeBytes,
                           String storagePath) {
        this.id = id;
        this.groupId = groupId;
        this.type = type != null ? type : ProjectResourceType.NOTE;
        this.title = title != null ? title : "";
        this.content = content != null ? content : "";
        this.createdBy = createdBy != null ? createdBy : "";
        this.createdAt = createdAt != null ? createdAt : new Date();
        this.fileUrl = fileUrl != null ? fileUrl : "";
        this.fileMimeType = fileMimeType != null ? fileMimeType : "";
        this.fileName = fileName != null ? fileName : "";
        this.fileSizeBytes = fileSizeBytes;
        this.storagePath = storagePath != null ? storagePath : "";
    }

    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public ProjectResourceType getType() {
        return type;
    }

    public void setType(ProjectResourceType type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public String getFileMimeType() {
        return fileMimeType;
    }

    public void setFileMimeType(String fileMimeType) {
        this.fileMimeType = fileMimeType;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public void setFileSizeBytes(long fileSizeBytes) {
        this.fileSizeBytes = fileSizeBytes;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProjectResource)) return false;
        ProjectResource that = (ProjectResource) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
