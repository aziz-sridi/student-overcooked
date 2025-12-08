package com.student.overcooked.data.repository.group;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.student.overcooked.data.model.ProjectResource;
import com.student.overcooked.data.model.ProjectResourceType;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Manages workspace resources (notes/files) for a group.
 */
public class GroupResourceDataSource {

    private final FirebaseAuth auth;
    private final StorageReference storageRoot;
    private final com.google.firebase.firestore.CollectionReference resourcesCollection;

    public GroupResourceDataSource(@NonNull FirebaseAuth auth,
                                   @NonNull StorageReference storageRoot,
                                   @NonNull com.google.firebase.firestore.CollectionReference resourcesCollection) {
        this.auth = auth;
        this.storageRoot = storageRoot;
        this.resourcesCollection = resourcesCollection;
    }

    public LiveData<List<ProjectResource>> getProjectResources(String groupId) {
        MutableLiveData<List<ProjectResource>> liveData = new MutableLiveData<>();
        resourcesCollection.whereEqualTo("groupId", groupId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null || snapshot == null) {
                        liveData.setValue(new ArrayList<>());
                        return;
                    }
                    List<ProjectResource> resources = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        ProjectResource resource = doc.toObject(ProjectResource.class);
                        if (resource != null) {
                            resources.add(resource);
                        }
                    }
                    // Sort by createdAt descending in memory to avoid needing composite index
                    resources.sort((r1, r2) -> {
                        if (r1.getCreatedAt() == null && r2.getCreatedAt() == null) return 0;
                        if (r1.getCreatedAt() == null) return 1;
                        if (r2.getCreatedAt() == null) return -1;
                        return r2.getCreatedAt().compareTo(r1.getCreatedAt());
                    });
                    liveData.setValue(resources);
                });
        return liveData;
    }

    public void addProjectResource(String groupId, ProjectResourceType type, String title, String content,
                                   String fileName, String fileUrl, long fileSizeBytes, String fileMimeType,
                                   String storagePath,
                                   OnSuccessListener<ProjectResource> onSuccess, OnFailureListener onFailure) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            onFailure.onFailure(new IllegalStateException("User not logged in"));
            return;
        }

        ProjectResource resource = new ProjectResource();
        resource.setId(UUID.randomUUID().toString());
        resource.setGroupId(groupId);
        resource.setType(type != null ? type : ProjectResourceType.NOTE);
        resource.setTitle(title);
        resource.setContent(content);
        resource.setCreatedBy(user.getUid());
        resource.setCreatedAt(new Date());
        resource.setFileName(fileName);
        resource.setFileUrl(fileUrl);
        resource.setFileSizeBytes(fileSizeBytes);
        resource.setFileMimeType(fileMimeType);
        resource.setStoragePath(storagePath);

        resourcesCollection.document(resource.getId()).set(resource)
                .addOnSuccessListener(aVoid -> onSuccess.onSuccess(resource))
                .addOnFailureListener(onFailure);
    }

    public void deleteProjectResource(ProjectResource resource, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        if (resource == null) {
            onFailure.onFailure(new IllegalArgumentException("Resource is null"));
            return;
        }
        resourcesCollection.document(resource.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    if (resource.getType() == ProjectResourceType.FILE &&
                            resource.getStoragePath() != null && !resource.getStoragePath().isEmpty()) {
                        storageRoot.child(resource.getStoragePath())
                                .delete()
                                .addOnSuccessListener(onSuccess)
                                .addOnFailureListener(onFailure);
                    } else {
                        onSuccess.onSuccess(null);
                    }
                })
                .addOnFailureListener(onFailure);
    }
}
