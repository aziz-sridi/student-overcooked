package com.student.overcooked.data.sync;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.student.overcooked.data.dao.TaskDao;
import com.student.overcooked.data.database.OvercookedDatabase;
import com.student.overcooked.data.model.Priority;
import com.student.overcooked.data.model.Task;
import com.student.overcooked.data.model.TaskStatus;
import com.student.overcooked.data.model.TaskType;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TaskSyncWorker extends Worker {

    public static final String UNIQUE_WORK_NAME = "task_sync";

    public static void enqueue(@NonNull Context context) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(TaskSyncWorker.class)
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(context)
                .enqueueUniqueWork(UNIQUE_WORK_NAME, androidx.work.ExistingWorkPolicy.REPLACE, request);
    }

    public TaskSyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            OvercookedDatabase db = OvercookedDatabase.getDatabase(getApplicationContext());
            TaskDao taskDao = db.taskDao();

            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

            FirebaseDatabase database = FirebaseDatabase.getInstance("https://studnetovercooked-default-rtdb.europe-west1.firebasedatabase.app");
            DatabaseReference root = database.getReference();

            List<Task> pending = taskDao.getPendingSyncTasksSync();
            for (Task task : pending) {
                if (task == null) continue;

                String userId = task.getUserId();
                if ((userId == null || userId.isEmpty()) && currentUser != null) {
                    userId = currentUser.getUid();
                    task.setUserId(userId);
                }
                if (userId == null || userId.isEmpty()) {
                    // Can't sync without a user.
                    continue;
                }

                String key = task.getFirestoreId();
                if (key == null || key.isEmpty()) {
                    key = UUID.randomUUID().toString();
                    task.setFirestoreId(key);
                    if (task.getId() == 0) {
                        task.setId(Math.abs(key.hashCode()));
                    }
                }

                DatabaseReference taskRef = root.child("users").child(userId).child("tasks").child(key);

                if (task.isPendingDelete()) {
                    if (task.isLastSyncedExists()) {
                        Tasks.await(taskRef.removeValue());
                    }
                    // Remove tombstone locally.
                    taskDao.deleteTaskById(task.getId());
                    continue;
                }

                // Upsert to RTDB
                Map<String, Object> map = taskToMap(task);
                Tasks.await(taskRef.setValue(map));

                task.setPendingSync(false);
                task.setPendingDelete(false);
                task.setLastSyncedExists(true);
                task.setLastSyncedCompleted(task.isCompleted());
                taskDao.updateTask(task);
            }

            return Result.success();
        } catch (Exception e) {
            android.util.Log.e("TaskSyncWorker", "Sync failed", e);
            return Result.retry();
        }
    }

    private static Map<String, Object> taskToMap(@NonNull Task task) {
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
