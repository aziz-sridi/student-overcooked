package com.example.overcooked.data.repository.group;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.overcooked.data.model.Group;
import com.example.overcooked.data.model.GroupTask;
import com.example.overcooked.data.model.Priority;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Handles all group task operations (CRUD + LiveData streaming).
 */
public class GroupTasksDataSource {

    public interface GroupFetcher {
        void getGroup(String groupId, OnSuccessListener<Group> onSuccess, OnFailureListener onFailure);
    }

    private final FirebaseAuth auth;
    private final com.google.firebase.firestore.CollectionReference groupsCollection;
    private final com.google.firebase.firestore.CollectionReference groupTasksCollection;
    private final GroupFetcher groupFetcher;

    public GroupTasksDataSource(@NonNull FirebaseAuth auth,
                                @NonNull com.google.firebase.firestore.CollectionReference groupsCollection,
                                @NonNull com.google.firebase.firestore.CollectionReference groupTasksCollection,
                                @NonNull GroupFetcher groupFetcher) {
        this.auth = auth;
        this.groupsCollection = groupsCollection;
        this.groupTasksCollection = groupTasksCollection;
        this.groupFetcher = groupFetcher;
    }

    public LiveData<List<GroupTask>> getGroupTasks(String groupId) {
        MutableLiveData<List<GroupTask>> liveData = new MutableLiveData<>();
        groupTasksCollection.whereEqualTo("groupId", groupId)
                .orderBy("deadline", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null || snapshot == null) {
                        liveData.setValue(new ArrayList<>());
                        return;
                    }

                    List<GroupTask> tasks = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        GroupTask task = doc.toObject(GroupTask.class);
                        if (task != null) {
                            tasks.add(task);
                        }
                    }
                    liveData.setValue(tasks);
                });
        return liveData;
    }

    public void createGroupTask(String groupId, String title, String description, Date deadline,
                                String assigneeId, String assigneeName, Priority priority,
                                OnSuccessListener<GroupTask> onSuccess, OnFailureListener onFailure) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            onFailure.onFailure(new IllegalStateException("User not logged in"));
            return;
        }

        GroupTask task = new GroupTask();
        task.setId(UUID.randomUUID().toString());
        task.setGroupId(groupId);
        task.setTitle(title);
        task.setDescription(description);
        task.setDeadline(deadline);
        task.setAssigneeId(assigneeId);
        task.setAssigneeName(assigneeName);
        task.setPriority(priority != null ? priority : Priority.MEDIUM);
        task.setCreatedBy(user.getUid());
        task.setCreatedAt(new Date());

        groupTasksCollection.document(task.getId()).set(task)
                .addOnSuccessListener(aVoid -> groupFetcher.getGroup(groupId, group -> {
                    if (group != null) {
                        groupsCollection.document(groupId)
                                .update("totalTasks", group.getTotalTasks() + 1)
                                .addOnSuccessListener(aVoid2 -> onSuccess.onSuccess(task))
                                .addOnFailureListener(onFailure);
                    } else {
                        onSuccess.onSuccess(task);
                    }
                }, onFailure))
                .addOnFailureListener(onFailure);
    }

    public void updateGroupTask(GroupTask task, String title, String description, Date deadline,
                                String assigneeId, String assigneeName, Priority priority,
                                OnSuccessListener<GroupTask> onSuccess, OnFailureListener onFailure) {
        if (task == null) {
            onFailure.onFailure(new IllegalArgumentException("Task is null"));
            return;
        }

        Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("title", title);
        updates.put("description", description);
        updates.put("deadline", deadline);
        updates.put("assigneeId", assigneeId);
        updates.put("assigneeName", assigneeName);
        updates.put("priority", priority != null ? priority : Priority.MEDIUM);

        groupTasksCollection.document(task.getId())
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    task.setTitle(title);
                    task.setDescription(description);
                    task.setDeadline(deadline);
                    task.setAssigneeId(assigneeId);
                    task.setAssigneeName(assigneeName);
                    task.setPriority(priority != null ? priority : Priority.MEDIUM);
                    onSuccess.onSuccess(task);
                })
                .addOnFailureListener(onFailure);
    }

    public void deleteGroupTask(GroupTask task, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        if (task == null) {
            onFailure.onFailure(new IllegalArgumentException("Task is null"));
            return;
        }

        groupTasksCollection.document(task.getId())
                .delete()
                .addOnSuccessListener(aVoid -> groupFetcher.getGroup(task.getGroupId(), group -> {
                    if (group != null) {
                        int newTotal = Math.max(0, group.getTotalTasks() - 1);
                        int newCompleted = group.getCompletedTasks();
                        if (task.isCompleted()) {
                            newCompleted = Math.max(0, newCompleted - 1);
                        }
                        groupsCollection.document(task.getGroupId())
                                .update("totalTasks", newTotal, "completedTasks", newCompleted)
                                .addOnSuccessListener(onSuccess)
                                .addOnFailureListener(onFailure);
                    } else {
                        onSuccess.onSuccess(null);
                    }
                }, onFailure))
                .addOnFailureListener(onFailure);
    }

    public void toggleGroupTaskCompletion(GroupTask task, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        if (task == null) {
            onFailure.onFailure(new IllegalArgumentException("Task is null"));
            return;
        }
        boolean newStatus = !task.isCompleted();

        groupTasksCollection.document(task.getId())
                .update("isCompleted", newStatus, "completedAt", newStatus ? new Date() : null)
                .addOnSuccessListener(aVoid -> groupFetcher.getGroup(task.getGroupId(), group -> {
                    if (group != null) {
                        int newCompleted = newStatus ?
                                group.getCompletedTasks() + 1 :
                                Math.max(0, group.getCompletedTasks() - 1);
                        groupsCollection.document(task.getGroupId())
                                .update("completedTasks", newCompleted)
                                .addOnSuccessListener(onSuccess)
                                .addOnFailureListener(onFailure);
                    } else {
                        onSuccess.onSuccess(null);
                    }
                }, onFailure))
                .addOnFailureListener(onFailure);
    }
}
