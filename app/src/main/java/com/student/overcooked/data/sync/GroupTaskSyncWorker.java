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
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.student.overcooked.data.dao.GroupTaskDao;
import com.student.overcooked.data.database.OvercookedDatabase;
import com.student.overcooked.data.model.GroupTask;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GroupTaskSyncWorker extends Worker {

    public static final String UNIQUE_WORK_NAME = "group_task_sync";

    public static void enqueue(@NonNull Context context) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(GroupTaskSyncWorker.class)
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(context)
                .enqueueUniqueWork(UNIQUE_WORK_NAME, androidx.work.ExistingWorkPolicy.REPLACE, request);
    }

    public GroupTaskSyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            OvercookedDatabase db = OvercookedDatabase.getDatabase(getApplicationContext());
            GroupTaskDao dao = db.groupTaskDao();

            FirebaseFirestore firestore = FirebaseFirestore.getInstance();
            com.google.firebase.firestore.CollectionReference groupTasksCollection = firestore.collection("group_tasks");
            com.google.firebase.firestore.CollectionReference groupsCollection = firestore.collection("groups");

            List<GroupTask> pending = dao.getPendingSyncTasksSync();
            for (GroupTask task : pending) {
                if (task == null || task.getId() == null) {
                    continue;
                }

                String groupId = task.getGroupId();

                if (task.isPendingDelete()) {
                    if (task.isLastSyncedExists()) {
                        Tasks.await(groupTasksCollection.document(task.getId()).delete());

                        if (groupId != null && !groupId.isEmpty()) {
                            Map<String, Object> updates = new HashMap<>();
                            updates.put("totalTasks", FieldValue.increment(-1));
                            if (task.isLastSyncedCompleted()) {
                                updates.put("completedTasks", FieldValue.increment(-1));
                            }
                            Tasks.await(groupsCollection.document(groupId).update(updates));
                        }
                    }

                    dao.deleteById(task.getId());
                    continue;
                }

                // Upsert task document
                Tasks.await(groupTasksCollection.document(task.getId()).set(task));

                // Update group aggregate counts based on last synced state
                if (groupId != null && !groupId.isEmpty()) {
                    Map<String, Object> updates = new HashMap<>();

                    if (!task.isLastSyncedExists()) {
                        updates.put("totalTasks", FieldValue.increment(1));
                    }

                    if (task.isLastSyncedCompleted() != task.isCompleted()) {
                        updates.put("completedTasks", FieldValue.increment(task.isCompleted() ? 1 : -1));
                    }

                    if (!updates.isEmpty()) {
                        Tasks.await(groupsCollection.document(groupId).update(updates));
                    }
                }

                task.setPendingSync(false);
                task.setPendingDelete(false);
                task.setLastSyncedExists(true);
                task.setLastSyncedCompleted(task.isCompleted());
                dao.upsert(task);
            }

            return Result.success();
        } catch (Exception e) {
            android.util.Log.e("GroupTaskSyncWorker", "Sync failed", e);
            return Result.retry();
        }
    }
}
