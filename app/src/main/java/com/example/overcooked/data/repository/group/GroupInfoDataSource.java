package com.example.overcooked.data.repository.group;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import com.example.overcooked.data.dao.GroupDao;
import com.example.overcooked.data.model.Group;
import com.example.overcooked.data.model.GroupMember;
import com.example.overcooked.data.model.GroupRole;
import com.example.overcooked.data.model.GroupTask;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

/**
 * Handles high-level group information (CRUD, membership cache refresh, metadata updates).
 */
public class GroupInfoDataSource {

    private static final String TAG = "GroupInfoDataSource";

    private final FirebaseAuth auth;
    private final FirebaseFirestore firestore;
    private final GroupDao groupDao;
    private final ExecutorService executorService;
    private final com.google.firebase.firestore.CollectionReference groupsCollection;
    private final com.google.firebase.firestore.CollectionReference membersCollection;
    private final com.google.firebase.firestore.CollectionReference groupTasksCollection;
    private final com.google.firebase.firestore.CollectionReference messagesCollection;
    private final com.google.firebase.firestore.CollectionReference resourcesCollection;

    public GroupInfoDataSource(@NonNull FirebaseAuth auth,
                               @NonNull FirebaseFirestore firestore,
                               @NonNull GroupDao groupDao,
                               @NonNull ExecutorService executorService,
                               @NonNull com.google.firebase.firestore.CollectionReference groupsCollection,
                               @NonNull com.google.firebase.firestore.CollectionReference membersCollection,
                               @NonNull com.google.firebase.firestore.CollectionReference groupTasksCollection,
                               @NonNull com.google.firebase.firestore.CollectionReference messagesCollection,
                               @NonNull com.google.firebase.firestore.CollectionReference resourcesCollection) {
        this.auth = auth;
        this.firestore = firestore;
        this.groupDao = groupDao;
        this.executorService = executorService;
        this.groupsCollection = groupsCollection;
        this.membersCollection = membersCollection;
        this.groupTasksCollection = groupTasksCollection;
        this.messagesCollection = messagesCollection;
        this.resourcesCollection = resourcesCollection;
    }

    public LiveData<List<Group>> getUserGroups() {
        refreshUserGroups();
        return groupDao.getAllGroups();
    }

    private void refreshUserGroups() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) return;

        String userId = currentUser.getUid();
        membersCollection.whereEqualTo("userId", userId)
                .addSnapshotListener((memberSnapshot, error) -> {
                    if (error != null || memberSnapshot == null) return;

                    Set<String> groupIdSet = new LinkedHashSet<>();
                    for (DocumentSnapshot doc : memberSnapshot.getDocuments()) {
                        String groupId = doc.getString("groupId");
                        if (groupId != null) {
                            groupIdSet.add(groupId);
                        }
                    }

                    List<String> groupIds = new ArrayList<>(groupIdSet);
                    if (groupIds.isEmpty()) {
                        if (!memberSnapshot.getMetadata().isFromCache()) {
                            executorService.execute(groupDao::deleteAll);
                        }
                        return;
                    }

                    fetchAndCacheGroups(groupIds);
                });
    }

    private void fetchAndCacheGroups(List<String> groupIds) {
        List<List<String>> chunks = chunkIds(groupIds, 10);
        List<Task<QuerySnapshot>> fetchTasks = new ArrayList<>();
        for (List<String> chunk : chunks) {
            fetchTasks.add(groupsCollection.whereIn("id", chunk).get());
        }

        Tasks.whenAllSuccess(fetchTasks)
                .addOnSuccessListener(results -> {
                    List<Group> groups = new ArrayList<>();
                    for (Object result : results) {
                        if (result instanceof QuerySnapshot) {
                            QuerySnapshot snapshot = (QuerySnapshot) result;
                            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                                Group group = doc.toObject(Group.class);
                                if (group != null) {
                                    groups.add(group);
                                }
                            }
                        }
                    }

                    executorService.execute(() -> {
                        groupDao.insertAll(groups);
                        if (!groupIds.isEmpty()) {
                            groupDao.deleteAllExcept(groupIds);
                        } else {
                            groupDao.deleteAll();
                        }
                    });
                })
                .addOnFailureListener(e -> Log.w(TAG, "Failed to refresh groups", e));
    }

    private List<List<String>> chunkIds(List<String> ids, int size) {
        List<List<String>> chunks = new ArrayList<>();
        if (ids == null || ids.isEmpty() || size <= 0) {
            return chunks;
        }
        for (int i = 0; i < ids.size(); i += size) {
            int end = Math.min(ids.size(), i + size);
            chunks.add(new ArrayList<>(ids.subList(i, end)));
        }
        return chunks;
    }

    public void createGroup(String name, String subject, String description,
                            boolean individualProject, Date deadline,
                            OnSuccessListener<Group> onSuccess,
                            OnFailureListener onFailure) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            onFailure.onFailure(new IllegalStateException("User not logged in"));
            return;
        }

        String groupId = UUID.randomUUID().toString();
        Group group = new Group();
        group.setId(groupId);
        group.setName(name);
        group.setSubject(subject);
        group.setDescription(description);
        group.setCreatedBy(user.getUid());
        group.setCreatedAt(new Date());
        group.setMemberCount(1);
        group.setIndividualProject(individualProject);
        group.setDeadline(deadline);

        executorService.execute(() -> {
            groupDao.insert(group);
            new Handler(Looper.getMainLooper()).post(() -> onSuccess.onSuccess(group));
        });

        groupsCollection.document(groupId).set(group)
                .addOnSuccessListener(aVoid -> {
                    GroupMember member = new GroupMember();
                    member.setId(UUID.randomUUID().toString());
                    member.setGroupId(groupId);
                    member.setUserId(user.getUid());
                    member.setUserName(user.getDisplayName() != null ? user.getDisplayName() : "Unknown");
                    member.setUserEmail(user.getEmail() != null ? user.getEmail() : "");
                    member.setRole(GroupRole.ADMIN);
                    member.setJoinedAt(new Date());

                    membersCollection.document(member.getId()).set(member);
                })
                .addOnFailureListener(e -> Log.w(TAG, "Failed to persist group remotely", e));
    }

    public void joinGroup(String joinCode, OnSuccessListener<Group> onSuccess, OnFailureListener onFailure) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            onFailure.onFailure(new IllegalStateException("User not logged in"));
            return;
        }

        groupsCollection.whereEqualTo("joinCode", joinCode.toUpperCase())
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        onSuccess.onSuccess(null);
                        return;
                    }

                    Group group = querySnapshot.getDocuments().get(0).toObject(Group.class);
                    if (group == null) {
                        onSuccess.onSuccess(null);
                        return;
                    }

                    membersCollection.whereEqualTo("groupId", group.getId())
                            .whereEqualTo("userId", user.getUid())
                            .get()
                            .addOnSuccessListener(memberSnapshot -> {
                                if (!memberSnapshot.isEmpty()) {
                                    onSuccess.onSuccess(group);
                                    return;
                                }

                                GroupMember member = new GroupMember();
                                member.setId(UUID.randomUUID().toString());
                                member.setGroupId(group.getId());
                                member.setUserId(user.getUid());
                                member.setUserName(user.getDisplayName() != null ? user.getDisplayName() : "Unknown");
                                member.setUserEmail(user.getEmail() != null ? user.getEmail() : "");
                                member.setRole(GroupRole.MEMBER);
                                member.setJoinedAt(new Date());

                                membersCollection.document(member.getId()).set(member)
                                        .addOnSuccessListener(aVoid -> groupsCollection.document(group.getId())
                                                .update("memberCount", group.getMemberCount() + 1)
                                                .addOnSuccessListener(aVoid2 -> onSuccess.onSuccess(group))
                                                .addOnFailureListener(onFailure))
                                        .addOnFailureListener(onFailure);
                            })
                            .addOnFailureListener(onFailure);
                })
                .addOnFailureListener(onFailure);
    }

    public void getGroup(String groupId, OnSuccessListener<Group> onSuccess, OnFailureListener onFailure) {
        groupsCollection.document(groupId)
                .get()
                .addOnSuccessListener(doc -> onSuccess.onSuccess(doc.toObject(Group.class)))
                .addOnFailureListener(onFailure);
    }

    public void updateGroupDetails(String groupId, String name, String subject, String description,
                                   Date deadline, boolean individualProject,
                                   OnSuccessListener<Group> onSuccess, OnFailureListener onFailure) {
        Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("name", name);
        updates.put("subject", subject);
        updates.put("description", description);
        updates.put("deadline", deadline);
        updates.put("individualProject", individualProject);

        groupsCollection.document(groupId)
                .update(updates)
                .addOnSuccessListener(aVoid -> getGroup(groupId, group -> {
                    if (group != null) {
                        executorService.execute(() -> {
                            Group local = groupDao.getGroupByIdSync(groupId);
                            if (local != null) {
                                local.setName(name);
                                local.setSubject(subject);
                                local.setDescription(description);
                                local.setDeadline(deadline);
                                local.setIndividualProject(individualProject);
                                groupDao.update(local);
                            }
                        });
                    }
                    onSuccess.onSuccess(group);
                }, onFailure))
                .addOnFailureListener(onFailure);
    }

    public void deleteGroup(String groupId, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        List<Task<Void>> deletions = new ArrayList<>();
        deletions.add(deleteQuery(membersCollection.whereEqualTo("groupId", groupId)));
        deletions.add(deleteQuery(groupTasksCollection.whereEqualTo("groupId", groupId)));
        deletions.add(deleteQuery(messagesCollection.whereEqualTo("groupId", groupId)));
        deletions.add(deleteQuery(resourcesCollection.whereEqualTo("groupId", groupId)));
        deletions.add(groupsCollection.document(groupId).delete());

        Tasks.whenAllComplete(deletions)
                .addOnSuccessListener(tasks -> {
                    executorService.execute(() -> groupDao.deleteById(groupId));
                    onSuccess.onSuccess(null);
                })
                .addOnFailureListener(onFailure);
    }

    private Task<Void> deleteQuery(Query query) {
        return query.get().continueWithTask(task -> {
            if (!task.isSuccessful() || task.getResult() == null) {
                throw task.getException() != null ? task.getException() : new Exception("Query failed");
            }
            com.google.firebase.firestore.WriteBatch batch = firestore.batch();
            for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                batch.delete(doc.getReference());
            }
            return batch.commit();
        });
    }
}
