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
import java.util.List;
import java.util.Map;
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
                        Task task = mapToTask(doc);
                        if (task != null) {
                            tasks.add(task);
                        }
                    }

                    // Update local cache
                    executorService.execute(() -> {
                        taskDao.deleteAllTasks();
                        if (!tasks.isEmpty()) {
                            taskDao.insertTasks(tasks);
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

        Map<String, Object> data = taskToMap(task);

        getTasksCollection(user.getUid())
                .document(docId)
                .set(data)
                .addOnSuccessListener(aVoid -> {
                    executorService.execute(() -> taskDao.updateTask(task));
                    new Handler(Looper.getMainLooper()).post(onSuccess);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update task", e);
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
            executorService.execute(() -> taskDao.deleteTask(task));
            new Handler(Looper.getMainLooper()).post(onSuccess);
            return;
        }

        getTasksCollection(user.getUid())
                .document(docId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    executorService.execute(() -> taskDao.deleteTask(task));
                    new Handler(Looper.getMainLooper()).post(onSuccess);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to delete task", e);
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
