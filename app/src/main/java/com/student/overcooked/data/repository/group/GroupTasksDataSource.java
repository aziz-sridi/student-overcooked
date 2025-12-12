package com.student.overcooked.data.repository.group;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import com.google.firebase.firestore.ListenerRegistration;

import com.student.overcooked.data.LocalCoinStore;
import com.student.overcooked.data.dao.GroupTaskDao;
import com.student.overcooked.data.model.Group;
import com.student.overcooked.data.model.GroupTask;
import com.student.overcooked.data.model.Priority;
import com.student.overcooked.data.model.TaskStatus;
import com.student.overcooked.data.repository.UserRepository;
import com.student.overcooked.data.sync.GroupTaskSyncWorker;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;

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
    private final UserRepository userRepository;

    private final GroupTaskDao groupTaskDao;
    private final ExecutorService executorService;
    private final Context appContext;
    private final LocalCoinStore localCoinStore;

    private final Map<String, ListenerRegistration> listenersByGroupId = new HashMap<>();

    private static final int GROUP_TASK_REWARD = 10;

    public GroupTasksDataSource(@NonNull FirebaseAuth auth,
                                @NonNull com.google.firebase.firestore.CollectionReference groupsCollection,
                                @NonNull com.google.firebase.firestore.CollectionReference groupTasksCollection,
                                @NonNull GroupFetcher groupFetcher,
                                @NonNull UserRepository userRepository,
                                @NonNull GroupTaskDao groupTaskDao,
                                @NonNull ExecutorService executorService,
                                @NonNull Context appContext) {
        this.auth = auth;
        this.groupsCollection = groupsCollection;
        this.groupTasksCollection = groupTasksCollection;
        this.groupFetcher = groupFetcher;
        this.userRepository = userRepository;

        this.groupTaskDao = groupTaskDao;
        this.executorService = executorService;
        this.appContext = appContext.getApplicationContext();
        this.localCoinStore = new LocalCoinStore(this.appContext);
    }

    public LiveData<List<GroupTask>> getGroupTasks(String groupId) {
        startSync(groupId);
        return groupTaskDao.getGroupTasks(groupId);
    }

    private void startSync(@NonNull String groupId) {
        synchronized (listenersByGroupId) {
            if (listenersByGroupId.containsKey(groupId)) {
                return;
            }

            ListenerRegistration registration = groupTasksCollection
                    .whereEqualTo("groupId", groupId)
                    .addSnapshotListener((snapshot, error) -> {
                        if (error != null || snapshot == null) {
                            return;
                        }

                        List<GroupTask> remoteTasks = new ArrayList<>();
                        Set<String> remoteIds = new HashSet<>();

                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            GroupTask task = doc.toObject(GroupTask.class);
                            if (task == null) {
                                continue;
                            }

                            // Ensure TaskStatus is always available (Firestore stores it as a string)
                            String statusStr = doc.getString("status");
                            TaskStatus status = statusStr != null ? TaskStatus.valueOf(statusStr)
                                    : (task.isCompleted() ? TaskStatus.DONE : TaskStatus.NOT_STARTED);
                            task.setStatus(status);

                                Boolean rewardClaimed = doc.getBoolean("rewardClaimed");
                                task.setRewardClaimed(rewardClaimed != null ? rewardClaimed : task.isCompleted());

                            // Clear local sync flags for remote truth
                            task.setPendingSync(false);
                            task.setPendingDelete(false);
                            task.setLastSyncedExists(true);
                            task.setLastSyncedCompleted(task.isCompleted());

                            remoteTasks.add(task);
                            remoteIds.add(task.getId());
                        }

                        executorService.execute(() -> {
                            // Remove local tasks that were deleted remotely (but don't touch local pending items)
                            List<GroupTask> localTasks = groupTaskDao.getGroupTasksSync(groupId);
                            for (GroupTask local : localTasks) {
                                if (local == null || local.getId() == null) continue;
                                if (!remoteIds.contains(local.getId())) {
                                    if (!local.isPendingSync() && !local.isPendingDelete() && local.isLastSyncedExists()) {
                                        groupTaskDao.deleteById(local.getId());
                                    }
                                }
                            }

                            // Upsert remote tasks unless there is a local pending change
                            for (GroupTask remote : remoteTasks) {
                                GroupTask local = groupTaskDao.getByIdSync(remote.getId());
                                if (local != null && (local.isPendingSync() || local.isPendingDelete())) {
                                    continue;
                                }
                                groupTaskDao.upsert(remote);
                            }
                        });
                    });

            listenersByGroupId.put(groupId, registration);
        }
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
        task.setStatus(TaskStatus.NOT_STARTED);

        task.setPendingSync(true);
        task.setPendingDelete(false);
        task.setLastSyncedExists(false);
        task.setLastSyncedCompleted(false);

        executorService.execute(() -> groupTaskDao.upsert(task));
        enqueueSync();
        postSuccess(onSuccess, task);
    }

    public void updateGroupTask(GroupTask task, String title, String description, Date deadline,
                                String assigneeId, String assigneeName, Priority priority,
                                OnSuccessListener<GroupTask> onSuccess, OnFailureListener onFailure) {
        if (task == null) {
            onFailure.onFailure(new IllegalArgumentException("Task is null"));
            return;
        }

        task.setTitle(title);
        task.setDescription(description);
        task.setDeadline(deadline);
        task.setAssigneeId(assigneeId);
        task.setAssigneeName(assigneeName);
        task.setPriority(priority != null ? priority : Priority.MEDIUM);
        task.setPendingSync(true);

        executorService.execute(() -> groupTaskDao.upsert(task));
        enqueueSync();
        postSuccess(onSuccess, task);
    }

    public void deleteGroupTask(GroupTask task, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        if (task == null) {
            onFailure.onFailure(new IllegalArgumentException("Task is null"));
            return;
        }

        task.setPendingDelete(true);
        task.setPendingSync(true);
        executorService.execute(() -> groupTaskDao.upsert(task));
        enqueueSync();
        postSuccess(onSuccess, null);
    }

    public void toggleGroupTaskCompletion(GroupTask task, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        if (task == null) {
            onFailure.onFailure(new IllegalArgumentException("Task is null"));
            return;
        }
        boolean previousStatus = task.isCompleted();
        boolean newStatus = !previousStatus;
        TaskStatus newTaskStatus = newStatus ? TaskStatus.DONE : TaskStatus.NOT_STARTED;

        task.setCompleted(newStatus);
        task.setCompletedAt(newStatus ? new Date() : null);
        task.setStatus(newTaskStatus);
        task.setPendingSync(true);

        if (newStatus && !task.isRewardClaimed()) {
            task.setRewardClaimed(true);
            applyCoinRewardOnce();
        }

        executorService.execute(() -> groupTaskDao.upsert(task));
        enqueueSync();
        postSuccess(onSuccess, null);
    }

    public void updateGroupTaskStatus(GroupTask task, TaskStatus status, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        if (task == null) {
            onFailure.onFailure(new IllegalArgumentException("Task is null"));
            return;
        }
        TaskStatus newStatus = status != null ? status : TaskStatus.NOT_STARTED;

        boolean previousCompleted = task.isCompleted();
        boolean newCompleted = (newStatus == TaskStatus.DONE);
        Date newCompletedAt = newCompleted ? new Date() : null;

        task.setStatus(newStatus);
        task.setCompleted(newCompleted);
        task.setCompletedAt(newCompletedAt);
        task.setPendingSync(true);

        if (newCompleted && !task.isRewardClaimed()) {
            task.setRewardClaimed(true);
            applyCoinRewardOnce();
        }

        executorService.execute(() -> groupTaskDao.upsert(task));
        enqueueSync();
        postSuccess(onSuccess, null);
    }

    private void applyCoinRewardOnce() {
        int delta = GROUP_TASK_REWARD;
        android.util.Log.d("GroupTasksDataSource", "Applying coin reward once. Delta: " + delta);

        // Local-first: update local coin mirror immediately.
        localCoinStore.addCoins(delta);

        userRepository.updateCoinsBy(delta,
                newBalance -> android.util.Log.d("GroupTasksDataSource", "Coins updated successfully: " + newBalance),
                e -> android.util.Log.e("GroupTasksDataSource", "Failed to update coins", e));
    }

    private void enqueueSync() {
        GroupTaskSyncWorker.enqueue(appContext);
    }

    private void postSuccess(OnSuccessListener<?> onSuccess, Object value) {
        if (onSuccess == null) return;
        new Handler(Looper.getMainLooper()).post(() -> {
            //noinspection unchecked
            ((OnSuccessListener<Object>) onSuccess).onSuccess(value);
        });
    }
}
