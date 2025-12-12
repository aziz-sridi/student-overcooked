package com.student.overcooked.ui.workspace;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.UUID;

final class WorkspaceResourceUploader {

    interface Callback {
        void onSuccess(@NonNull Uri downloadUri, @NonNull String storagePath, long sizeBytes, @Nullable String mimeType);

        void onFailure(@NonNull Exception e);
    }

    private WorkspaceResourceUploader() {
    }

    static void uploadToFirebase(@NonNull String groupId,
                                @NonNull Uri fileUri,
                                @NonNull String displayName,
                                @NonNull Callback callback) {
        String safeName = displayName.replaceAll("[^a-zA-Z0-9._-]", "_");
        String storagePath = "workspace/" + groupId + "/" + UUID.randomUUID() + "_" + safeName;

        StorageReference storageReference = FirebaseStorage.getInstance().getReference().child(storagePath);
        storageReference.putFile(fileUri)
                .addOnSuccessListener(taskSnapshot ->
                        storageReference.getDownloadUrl()
                                .addOnSuccessListener(downloadUri -> {
                                    long size = -1L;
                                    String mime = null;
                                    if (taskSnapshot.getMetadata() != null) {
                                        size = taskSnapshot.getMetadata().getSizeBytes();
                                        mime = taskSnapshot.getMetadata().getContentType();
                                    }
                                    callback.onSuccess(downloadUri, storagePath, size, mime);
                                })
                                .addOnFailureListener(e -> callback.onFailure(e)))
                .addOnFailureListener(e -> callback.onFailure(e));
    }
}
