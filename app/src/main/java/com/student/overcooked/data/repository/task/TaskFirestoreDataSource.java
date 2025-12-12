package com.student.overcooked.data.repository.task;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.student.overcooked.data.dao.TaskDao;
import com.student.overcooked.data.model.Priority;
import com.student.overcooked.data.model.Task;
import com.student.overcooked.data.model.TaskStatus;
import com.student.overcooked.data.model.TaskType;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

/**
 * Manages real-time sync between Firestore and local Room cache for personal tasks.
 * Each user has their own tasks collection: users/{userId}/tasks/{taskId}
 */
public class TaskFirestoreDataSource {

    private static final String TAG = "TaskFirestoreSync";
    private static final String COLLECTION_USERS = "users";
    private static final String COLLECTION_TASKS = "tasks";

    private final FirebaseAuth auth;
    private final FirebaseFirestore firestore;
    private final TaskDao taskDao;
    private final ExecutorService executorService;
    
    // Track tasks that are being updated locally to avoid sync conflicts
    private final Set<String> pendingUpdates = new HashSet<>();
    // Track tasks that are being deleted to prevent re-sync
    private final Set<String> pendingDeletions = new HashSet<>();

    public TaskFirestoreDataSource(@NonNull FirebaseAuth auth,
                                   @NonNull FirebaseFirestore firestore,
                                   @NonNull TaskDao taskDao,
                                   @NonNull ExecutorService executorService) {
        this.auth = auth;
        this.firestore = firestore;
        this.taskDao = taskDao;
        this.executorService = executorService;
    }

    /**
     * Start listening to user's tasks from Firestore and sync to Room
     */
    public void startSync() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Log.w(TAG, "No user logged in, skipping task sync");
            return;
        }

        getTasksCollection(user.getUid())
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Failed to sync tasks", error);
                        return;
                    }
                    if (snapshot == null) {
                        return;
                    }

                    List<Task> tasks = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String docId = doc.getId();
                        // Skip tasks that have pending local updates
                        synchronized (pendingUpdates) {
                            if (pendingUpdates.contains(docId)) {
                                Log.d(TAG, "Skipping sync for task with pending update: " + docId);
                                continue;
                            }
                        }
                        // Skip tasks that are being deleted
                        synchronized (pendingDeletions) {
                            if (pendingDeletions.contains(docId)) {
                                Log.d(TAG, "Skipping sync for task with pending deletion: " + docId);
                                continue;
                            }
                        }
                        Task task = mapToTask(doc);
                        if (task != null) {
                            tasks.add(task);
                        }
                    }

                    // Collect Firestore IDs from the snapshot (including pending deletes as they're already removed)
                    Set<String> firestoreIds = new HashSet<>();
                    for (Task task : tasks) {
                        if (task.getFirestoreId() != null) {
                            firestoreIds.add(task.getFirestoreId());
                        }
                    }
                    
                    // Also add pending deletions to the set (don't delete them twice)
                    synchronized (pendingDeletions) {
                        firestoreIds.addAll(pendingDeletions);
                    }

                    // Update local cache - sync with Firestore state
                    executorService.execute(() -> {
                        // Get all local tasks and remove ones not in Firestore anymore
                        List<Task> localTasks = taskDao.getAllTasksSync();
                        for (Task localTask : localTasks) {
                            String localFirestoreId = localTask.getFirestoreId();
                            if (localFirestoreId != null && !localFirestoreId.isEmpty() && !firestoreIds.contains(localFirestoreId)) {
                                // Check if it's pending deletion before removing
                                boolean isPendingDeletion;
                                synchronized (pendingDeletions) {
                                    isPendingDeletion = pendingDeletions.contains(localFirestoreId);
                                }
                                if (!isPendingDeletion) {
                                    // This task was deleted from Firestore, remove locally
                                    Log.d(TAG, "Removing deleted task: " + localFirestoreId);
                                    taskDao.deleteTask(localTask);
                                }
                            }
                        }
                        
                        // Insert or update tasks from Firestore (skip if pending deletion)
                        for (Task task : tasks) {
                            String firestoreId = task.getFirestoreId();
                            boolean isPendingDeletion = false;
                            if (firestoreId != null) {
                                synchronized (pendingDeletions) {
                                    isPendingDeletion = pendingDeletions.contains(firestoreId);
                                }
                            }
                            if (!isPendingDeletion) {
                                Task existing = taskDao.getTaskById(task.getId());
                                if (existing == null) {
                                    taskDao.insertTask(task);
                                } else {
                                    taskDao.updateTask(task);
                                }
                            } else {
                                Log.d(TAG, "Skipping insert/update for pending deletion: " + firestoreId);
                            }
                        }
                    });
                });
    }

    /**
     * Create a new task in Firestore
     */
    public void createTask(@NonNull Task task, @NonNull Runnable onSuccess, @NonNull Runnable onFailure) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            new Handler(Looper.getMainLooper()).post(onFailure);
            return;
        }

        // Generate Firestore ID if needed
        String docId = task.getFirestoreId();
        if (docId == null || docId.isEmpty()) {
            docId = UUID.randomUUID().toString();
            task.setFirestoreId(docId);
        }
        task.setUserId(user.getUid());
        
        // Use hash of firestore ID as numeric Room ID if not set
        if (task.getId() == 0) {
            task.setId(Math.abs(docId.hashCode()));
        }

        Map<String, Object> data = taskToMap(task);
        getTasksCollection(user.getUid())
                .document(docId)
                .set(data)
                .addOnSuccessListener(aVoid -> {
                    // Update local cache
                    executorService.execute(() -> taskDao.insertTask(task));
                    new Handler(Looper.getMainLooper()).post(onSuccess);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to create task", e);
                    new Handler(Looper.getMainLooper()).post(onFailure);
                });
    }

    /**
     * Update an existing task in Firestore
     */
    public void updateTask(@NonNull Task task, @NonNull Runnable onSuccess, @NonNull Runnable onFailure) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            new Handler(Looper.getMainLooper()).post(onFailure);
            return;
        }

        String docId = task.getFirestoreId();
        if (docId == null || docId.isEmpty()) {
            // No Firestore ID, treat as create
            createTask(task, onSuccess, onFailure);
            return;
        }

        // Mark this task as having a pending update to prevent sync from reverting it
        synchronized (pendingUpdates) {
            pendingUpdates.add(docId);
        }
        Log.d(TAG, "Marked task as pending: " + docId);
        
        // Optimistic local update FIRST
        executorService.execute(() -> taskDao.updateTask(task));

        Map<String, Object> data = taskToMap(task);

        getTasksCollection(user.getUid())
                .document(docId)
                .set(data)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Firestore update successful for task: " + docId);
                    // Clear pending flag after success
                    synchronized (pendingUpdates) {
                        pendingUpdates.remove(docId);
                    }
                    new Handler(Looper.getMainLooper()).post(onSuccess);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update task in Firestore: " + docId, e);
                    // Clear pending flag on failure too
                    synchronized (pendingUpdates) {
                        pendingUpdates.remove(docId);
                    }
                    new Handler(Looper.getMainLooper()).post(onFailure);
                });
    }

    /**
     * Delete a task from Firestore
     */
    public void deleteTask(@NonNull Task task, @NonNull Runnable onSuccess, @NonNull Runnable onFailure) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            new Handler(Looper.getMainLooper()).post(onFailure);
            return;
        }

        String docId = task.getFirestoreId();
        if (docId == null || docId.isEmpty()) {
            // No Firestore ID, just delete locally
            Log.d(TAG, "Deleting local-only task (no Firestore ID): " + task.getTitle());
            executorService.execute(() -> {
                taskDao.deleteTask(task);
                Log.d(TAG, "Local-only task deleted: " + task.getTitle());
                new Handler(Looper.getMainLooper()).post(onSuccess);
            });
            return;
        }

        // Mark as pending deletion to prevent re-sync from re-adding
        synchronized (pendingDeletions) {
            pendingDeletions.add(docId);
        }
        Log.d(TAG, "Marked task for deletion: " + docId);
        
        // Delete locally first (optimistic)
        executorService.execute(() -> taskDao.deleteTask(task));

        getTasksCollection(user.getUid())
                .document(docId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Firestore delete successful for task: " + docId);
                    // Clear pending deletion flag after success
                    synchronized (pendingDeletions) {
                        pendingDeletions.remove(docId);
                    }
                    new Handler(Looper.getMainLooper()).post(onSuccess);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to delete task from Firestore: " + docId, e);
                    // Clear pending deletion flag on failure
                    synchronized (pendingDeletions) {
                        pendingDeletions.remove(docId);
                    }
                    new Handler(Looper.getMainLooper()).post(onFailure);
                });
    }

    private CollectionReference getTasksCollection(String userId) {
        return firestore.collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_TASKS);
    }

    private Map<String, Object> taskToMap(Task task) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", task.getId());
        map.put("firestoreId", task.getFirestoreId());
        map.put("userId", task.getUserId());
        map.put("title", task.getTitle());
        map.put("description", task.getDescription());
        map.put("course", task.getCourse());
        map.put("taskType", task.getTaskType() != null ? task.getTaskType().name() : TaskType.HOMEWORK.name());
        map.put("deadline", task.getDeadline());
        map.put("isCompleted", task.isCompleted());
        map.put("projectId", task.getProjectId());
        map.put("priority", task.getPriority() != null ? task.getPriority().name() : Priority.MEDIUM.name());
        map.put("createdAt", task.getCreatedAt());
        map.put("completedAt", task.getCompletedAt());
        map.put("notes", task.getNotes());
        map.put("status", task.getStatus() != null ? task.getStatus().name() : TaskStatus.NOT_STARTED.name());
        return map;
    }

    private Task mapToTask(DocumentSnapshot doc) {
        try {
            Task task = new Task();
            Long id = doc.getLong("id");
            task.setId(id != null ? id : Math.abs(doc.getId().hashCode()));
            task.setFirestoreId(doc.getId());
            task.setUserId(doc.getString("userId"));
            task.setTitle(doc.getString("title"));
            task.setDescription(doc.getString("description"));
            task.setCourse(doc.getString("course"));
            
            String taskTypeStr = doc.getString("taskType");
            task.setTaskType(taskTypeStr != null ? TaskType.valueOf(taskTypeStr) : TaskType.HOMEWORK);
            
            task.setDeadline(doc.getDate("deadline"));
            Boolean completed = doc.getBoolean("isCompleted");
            task.setCompleted(completed != null && completed);
            task.setProjectId(doc.getLong("projectId"));
            
            String priorityStr = doc.getString("priority");
            task.setPriority(priorityStr != null ? Priority.valueOf(priorityStr) : Priority.MEDIUM);
            
            task.setCreatedAt(doc.getDate("createdAt"));
            task.setCompletedAt(doc.getDate("completedAt"));
            task.setNotes(doc.getString("notes"));
            
            String statusStr = doc.getString("status");
            task.setStatus(statusStr != null ? TaskStatus.valueOf(statusStr) : TaskStatus.NOT_STARTED);
            
            return task;
        } catch (Exception e) {
            Log.e(TAG, "Failed to map task from Firestore", e);
            return null;
        }
    }
}
