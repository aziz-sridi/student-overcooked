package com.student.overcooked.ui.workspace;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

final class WorkspaceFileSelection {

    private Uri fileUri;
    private String displayName;
    private long sizeBytes = -1L;
    private String mimeType;
    private String downloadUrl;
    private String storagePath;

    void applyLocalSelection(@NonNull Uri uri, @Nullable String name, long size, @Nullable String mime) {
        this.fileUri = uri;
        this.displayName = name;
        this.sizeBytes = size;
        this.mimeType = mime;
        this.downloadUrl = null;
        this.storagePath = null;
    }

    void clear() {
        fileUri = null;
        displayName = null;
        sizeBytes = -1L;
        mimeType = null;
        downloadUrl = null;
        storagePath = null;
    }

    boolean hasLocalFile() {
        return fileUri != null;
    }

    @Nullable
    Uri getFileUri() {
        return fileUri;
    }

    @Nullable
    String getDisplayName() {
        return displayName;
    }

    long getSizeBytes() {
        return sizeBytes;
    }

    @Nullable
    String getMimeType() {
        return mimeType;
    }

    @Nullable
    String getDownloadUrl() {
        return downloadUrl;
    }

    @Nullable
    String getStoragePath() {
        return storagePath;
    }

    void setDisplayName(@Nullable String name) {
        this.displayName = name;
    }

    void setSizeBytes(long size) {
        this.sizeBytes = size;
    }

    void setMimeType(@Nullable String mime) {
        this.mimeType = mime;
    }

    void setUploadMetadata(@NonNull String url, @NonNull String path) {
        this.downloadUrl = url;
        this.storagePath = path;
    }
}
