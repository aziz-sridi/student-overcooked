package com.student.overcooked.data.repository.task;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.student.overcooked.data.dao.TaskDao;
import com.student.overcooked.data.model.Priority;
import com.student.overcooked.data.model.Task;
import com.student.overcooked.data.model.TaskStatus;
import com.student.overcooked.data.model.TaskType;
import com.student.overcooked.data.sync.TaskSyncWorker;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

/**
 * Manages real-time sync between Firebase Realtime Database and local Room cache for personal tasks.
 * Each user has their own tasks: users/{userId}/tasks/{taskId}
 * Uses ValueEventListener for instant real-time synchronization.
 */
public class TaskRealtimeDataSource {

    private static final String TAG = "TaskRealtimeSync";
    private static final String PATH_USERS = "users";
    private static final String PATH_TASKS = "tasks";

    private final FirebaseAuth auth;
    private final DatabaseReference database;
    private final TaskDao taskDao;
    private final ExecutorService executorService;

    private final Context appContext;
    
    private ValueEventListener tasksListener;

    public TaskRealtimeDataSource(@NonNull FirebaseAuth auth,
                                   @NonNull FirebaseDatabase firebaseDatabase,
                                   @NonNull TaskDao taskDao,
                                   @NonNull ExecutorService executorService,
                                   @NonNull Context appContext) {
        this.auth = auth;
        this.database = firebaseDatabase.getReference();
        this.taskDao = taskDao;
        this.executorService = executorService;
        this.appContext = appContext.getApplicationContext();
        
        // Log database URL for debugging
        Log.d(TAG, "Firebase Database URL: " + firebaseDatabase.getReference().toString());
    }

    /**
     * Start listening to user's tasks with real-time sync
     */
    public void startSync() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Log.w(TAG, "No user logged in, skipping task sync");
            return;
        }

        DatabaseReference tasksRef = getUserTasksRef(user.getUid());
        
        // Remove any existing listener
        if (tasksListener != null) {
            tasksRef.removeEventListener(tasksListener);
        }

        tasksListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d(TAG, "Real-time sync triggered - processing " + snapshot.getChildrenCount() + " tasks");
                
                List<Task> remoteTasks = new ArrayList<>();
                java.util.Set<String> remoteTaskIds = new java.util.HashSet<>();
                
                for (DataSnapshot taskSnapshot : snapshot.getChildren()) {
                    String taskId = taskSnapshot.getKey();
                    if (taskId == null) continue;
                    
                    Task task = snapshotToTask(taskSnapshot);
                    if (task != null) {
                        remoteTasks.add(task);
                        remoteTaskIds.add(taskId);
                    }
                }
                
                // Update local database
                executorService.execute(() -> {
                    List<Task> localAll = taskDao.getAllTasksIncludingDeletedSync();

                    // Apply remote deletions (but never override local pending changes).
                    for (Task local : localAll) {
                        String fid = local.getFirestoreId();
                        if (fid == null || fid.isEmpty()) continue;
                        if (local.isPendingSync() || local.isPendingDelete()) continue;

                        if (!remoteTaskIds.contains(fid)) {
                            if (local.isLastSyncedExists()) {
                                Log.d(TAG, "Removing task deleted remotely: " + local.getTitle());
                                taskDao.deleteTask(local);
                            }
                        }
                    }

                    // Upsert remote tasks into Room (but never overwrite local pending changes).
                    for (Task remote : remoteTasks) {
                        String fid = remote.getFirestoreId();
                        if (fid == null || fid.isEmpty()) continue;

                        Task local = taskDao.getTaskByFirestoreIdSync(fid);
                        if (local != null && (local.isPendingSync() || local.isPendingDelete())) {
                            continue;
                        }

                        remote.setPendingSync(false);
                        remote.setPendingDelete(false);
                        remote.setLastSyncedExists(true);
                        remote.setLastSyncedCompleted(remote.isCompleted());

                        if (local == null) {
                            if (remote.getId() == 0) {
                                remote.setId(Math.abs(fid.hashCode()));
                            }
                            taskDao.insertTask(remote);
                        } else {
                            local.setUserId(remote.getUserId());
                            local.setFirestoreId(remote.getFirestoreId());
                            local.setTitle(remote.getTitle());
                            local.setDescription(remote.getDescription());
                            local.setCourse(remote.getCourse());
                            local.setTaskType(remote.getTaskType());
                            local.setPriority(remote.getPriority());
                            local.setStatus(remote.getStatus());
                            local.setDeadline(remote.getDeadline());
                            local.setCreatedAt(remote.getCreatedAt());
                            local.setCompleted(remote.isCompleted());
                            local.setCompletedAt(remote.getCompletedAt());
                            local.setProjectId(remote.getProjectId());
                            local.setNotes(remote.getNotes());

                            local.setPendingSync(false);
                            local.setPendingDelete(false);
                            local.setLastSyncedExists(true);
                            local.setLastSyncedCompleted(remote.isCompleted());

                            taskDao.updateTask(local);
                        }
                    }
                    
                    Log.d(TAG, "Sync complete - " + remoteTasks.size() + " tasks synced");
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "❌ Failed to sync tasks - Error: " + error.getMessage(), error.toException());
                Log.e(TAG, "Error code: " + error.getCode());
                Log.e(TAG, "Error details: " + error.getDetails());
                
                if (error.getCode() == DatabaseError.PERMISSION_DENIED) {
                    Log.e(TAG, "⚠️ PERMISSION DENIED - You need to configure Firebase Realtime Database Rules!");
                    Log.e(TAG, "Go to Firebase Console → Realtime Database → Rules");
                    Log.e(TAG, "Set rules to allow authenticated users to read/write their own data");
                }
            }
        };
        
        tasksRef.addValueEventListener(tasksListener);
        Log.d(TAG, "Started real-time sync for user: " + user.getUid());
    }

    /**
     * Stop listening to updates (cleanup)
     */
    public void stopSync() {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null && tasksListener != null) {
            getUserTasksRef(user.getUid()).removeEventListener(tasksListener);
            tasksListener = null;
        }
    }

    /**
     * Create a new task
     */
    public void createTask(@NonNull Task task, @NonNull Runnable onSuccess, @NonNull Runnable onFailure) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            new Handler(Looper.getMainLooper()).post(onFailure);
            return;
        }

        // Generate ID if needed
        String taskId = task.getFirestoreId();
        if (taskId == null || taskId.isEmpty()) {
            taskId = UUID.randomUUID().toString();
            task.setFirestoreId(taskId);
        }
        task.setUserId(user.getUid());
        
        // Use hash of ID as numeric Room ID if not set
        if (task.getId() == 0) {
            task.setId(Math.abs(taskId.hashCode()));
        }

        Log.d(TAG, "Creating task (Room-first): " + task.getTitle() + " (ID: " + taskId + ")");

        task.setPendingSync(true);
        task.setPendingDelete(false);
        task.setLastSyncedExists(false);
        task.setLastSyncedCompleted(task.isCompleted());

        executorService.execute(() -> {
            taskDao.insertTask(task);
            TaskSyncWorker.enqueue(appContext);
            new Handler(Looper.getMainLooper()).post(onSuccess);
        });
    }

    /**
     * Update an existing task
     */
    public void updateTask(@NonNull Task task, @NonNull Runnable onSuccess, @NonNull Runnable onFailure) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            new Handler(Looper.getMainLooper()).post(onFailure);
            return;
        }

        String taskId = task.getFirestoreId();
        if (taskId == null || taskId.isEmpty()) {
            taskId = UUID.randomUUID().toString();
            task.setFirestoreId(taskId);
            if (task.getId() == 0) {
                task.setId(Math.abs(taskId.hashCode()));
            }
            task.setLastSyncedExists(false);
        }

        task.setUserId(user.getUid());
        task.setPendingSync(true);
        task.setPendingDelete(false);
        task.setLastSyncedCompleted(task.isCompleted());

        Log.d(TAG, "Updating task (Room-first): " + task.getTitle() + " (ID: " + taskId + ")");

        executorService.execute(() -> {
            taskDao.updateTask(task);
            TaskSyncWorker.enqueue(appContext);
            new Handler(Looper.getMainLooper()).post(onSuccess);
        });
    }

    /**
     * Delete a task
     */
    public void deleteTask(@NonNull Task task, @NonNull Runnable onSuccess, @NonNull Runnable onFailure) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            new Handler(Looper.getMainLooper()).post(onFailure);
            return;
        }

        String taskId = task.getFirestoreId();
        if (taskId == null || taskId.isEmpty()) {
            // No Firebase ID, just delete locally
            Log.d(TAG, "Deleting local-only task: " + task.getTitle());
            executorService.execute(() -> {
                taskDao.deleteTask(task);
                Log.d(TAG, "Local-only task deleted: " + task.getTitle());
                new Handler(Looper.getMainLooper()).post(onSuccess);
            });
            return;
        }

        Log.d(TAG, "Deleting task (Room-first tombstone): " + task.getTitle() + " (ID: " + taskId + ")");
        task.setUserId(user.getUid());
        task.setPendingDelete(true);
        task.setPendingSync(true);

        executorService.execute(() -> {
            taskDao.updateTask(task);
            TaskSyncWorker.enqueue(appContext);
            new Handler(Looper.getMainLooper()).post(onSuccess);
        });
    }

    /**
     * Get reference to user's tasks
     */
    private DatabaseReference getUserTasksRef(String userId) {
        return database.child(PATH_USERS).child(userId).child(PATH_TASKS);
    }

    /**
     * Convert DataSnapshot to Task
     */
    private Task snapshotToTask(DataSnapshot snapshot) {
        try {
            String taskId = snapshot.getKey();
            
            Task task = new Task();
            task.setFirestoreId(taskId);
            task.setId(snapshot.child("id").getValue(Long.class) != null ? 
                    snapshot.child("id").getValue(Long.class) : Math.abs(taskId.hashCode()));
            task.setUserId(snapshot.child("userId").getValue(String.class));
            task.setTitle(snapshot.child("title").getValue(String.class));
            task.setDescription(snapshot.child("description").getValue(String.class));
            task.setCourse(snapshot.child("course").getValue(String.class));
            
            // TaskType
            String typeStr = snapshot.child("taskType").getValue(String.class);
            task.setTaskType(typeStr != null ? TaskType.valueOf(typeStr) : TaskType.HOMEWORK);
            
            // Priority
            String priorityStr = snapshot.child("priority").getValue(String.class);
            task.setPriority(priorityStr != null ? Priority.valueOf(priorityStr) : Priority.MEDIUM);
            
            // TaskStatus
            String statusStr = snapshot.child("status").getValue(String.class);
            task.setStatus(statusStr != null ? TaskStatus.valueOf(statusStr) : TaskStatus.NOT_STARTED);
            
            // Dates
            Long deadlineMs = snapshot.child("deadline").getValue(Long.class);
            task.setDeadline(deadlineMs != null ? new Date(deadlineMs) : null);
            
            Long createdAtMs = snapshot.child("createdAt").getValue(Long.class);
            task.setCreatedAt(createdAtMs != null ? new Date(createdAtMs) : new Date());
            
            Long completedAtMs = snapshot.child("completedAt").getValue(Long.class);
            task.setCompletedAt(completedAtMs != null ? new Date(completedAtMs) : null);
            
            // Boolean
            Boolean isCompleted = snapshot.child("isCompleted").getValue(Boolean.class);
            task.setCompleted(isCompleted != null ? isCompleted : false);

            Boolean rewardClaimed = snapshot.child("rewardClaimed").getValue(Boolean.class);
            // If missing, treat already-completed tasks as claimed to prevent farming.
            task.setRewardClaimed(rewardClaimed != null ? rewardClaimed : task.isCompleted());
            
            // Long
            Long projectId = snapshot.child("projectId").getValue(Long.class);
            task.setProjectId(projectId);
            
            // Notes
            task.setNotes(snapshot.child("notes").getValue(String.class));
            
            return task;
        } catch (Exception e) {
            Log.e(TAG, "Error converting snapshot to task", e);
            return null;
        }
    }

    /**
     * Convert Task to Map for Firebase
     */
    private Map<String, Object> taskToMap(Task task) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", task.getId());
        map.put("firestoreId", task.getFirestoreId());
        map.put("userId", task.getUserId());
        map.put("title", task.getTitle());
        map.put("description", task.getDescription());
        map.put("course", task.getCourse());
        map.put("taskType", task.getTaskType() != null ? task.getTaskType().name() : TaskType.HOMEWORK.name());
        map.put("priority", task.getPriority() != null ? task.getPriority().name() : Priority.MEDIUM.name());
        map.put("status", task.getStatus() != null ? task.getStatus().name() : TaskStatus.NOT_STARTED.name());
        map.put("deadline", task.getDeadline() != null ? task.getDeadline().getTime() : null);
        map.put("createdAt", task.getCreatedAt() != null ? task.getCreatedAt().getTime() : new Date().getTime());
        map.put("completedAt", task.getCompletedAt() != null ? task.getCompletedAt().getTime() : null);
        map.put("isCompleted", task.isCompleted());
        map.put("rewardClaimed", task.isRewardClaimed());
        map.put("projectId", task.getProjectId());
        map.put("notes", task.getNotes());
        return map;
    }
}
