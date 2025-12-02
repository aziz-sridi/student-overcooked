package com.example.overcooked.data.repository.group;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.overcooked.data.model.Group;
import com.example.overcooked.data.model.GroupMember;
import com.example.overcooked.data.model.GroupRole;
import com.example.overcooked.data.model.User;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Handles group membership operations and queries.
 */
public class GroupMemberDataSource {

    private final FirebaseAuth auth;
    private final com.google.firebase.firestore.CollectionReference membersCollection;
    private final com.google.firebase.firestore.CollectionReference usersCollection;
    private final com.google.firebase.firestore.CollectionReference groupsCollection;

    public GroupMemberDataSource(@NonNull FirebaseAuth auth,
                                 @NonNull com.google.firebase.firestore.CollectionReference membersCollection,
                                 @NonNull com.google.firebase.firestore.CollectionReference usersCollection,
                                 @NonNull com.google.firebase.firestore.CollectionReference groupsCollection) {
        this.auth = auth;
        this.membersCollection = membersCollection;
        this.usersCollection = usersCollection;
        this.groupsCollection = groupsCollection;
    }

    public LiveData<List<GroupMember>> getGroupMembers(String groupId) {
        MutableLiveData<List<GroupMember>> liveData = new MutableLiveData<>();
        membersCollection.whereEqualTo("groupId", groupId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null || snapshot == null) {
                        liveData.setValue(new ArrayList<>());
                        return;
                    }
                    List<GroupMember> members = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        GroupMember member = doc.toObject(GroupMember.class);
                        if (member != null) {
                            members.add(member);
                        }
                    }
                    liveData.setValue(members);
                });
        return liveData;
    }

    public void isGroupAdmin(String groupId, OnSuccessListener<Boolean> onSuccess, OnFailureListener onFailure) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            onSuccess.onSuccess(false);
            return;
        }

        membersCollection.whereEqualTo("groupId", groupId)
                .whereEqualTo("userId", user.getUid())
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        onSuccess.onSuccess(false);
                        return;
                    }

                    GroupMember member = snapshot.getDocuments().get(0).toObject(GroupMember.class);
                    onSuccess.onSuccess(member != null && member.getRole() == GroupRole.ADMIN);
                })
                .addOnFailureListener(onFailure);
    }

    public void removeMember(String groupId, String memberId, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        membersCollection.document(memberId).delete()
                .addOnSuccessListener(aVoid -> groupsCollection.document(groupId)
                        .get()
                        .addOnSuccessListener(groupDoc -> {
                            Group group = groupDoc.toObject(Group.class);
                            if (group != null) {
                                groupsCollection.document(groupId)
                                        .update("memberCount", Math.max(0, group.getMemberCount() - 1))
                                        .addOnSuccessListener(onSuccess)
                                        .addOnFailureListener(onFailure);
                            } else {
                                onSuccess.onSuccess(null);
                            }
                        }))
                .addOnFailureListener(onFailure);
    }

    public void addMemberByUsername(String groupId, String username, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        usersCollection.whereEqualTo("username", username)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot userDoc = queryDocumentSnapshots.getDocuments().get(0);
                        User user = userDoc.toObject(User.class);
                        if (user != null) {
                            addMemberToGroup(groupId, user, onSuccess, onFailure);
                        } else {
                            onFailure.onFailure(new Exception("User data is invalid"));
                        }
                    } else {
                        onFailure.onFailure(new Exception("User not found"));
                    }
                })
                .addOnFailureListener(onFailure);
    }

    private void addMemberToGroup(String groupId, User user, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        membersCollection.whereEqualTo("groupId", groupId)
                .whereEqualTo("userId", user.getUid())
                .get()
                .addOnSuccessListener(memberSnapshot -> {
                    if (!memberSnapshot.isEmpty()) {
                        onFailure.onFailure(new Exception("User is already a member"));
                        return;
                    }

                    GroupMember member = new GroupMember();
                    member.setId(UUID.randomUUID().toString());
                    member.setGroupId(groupId);
                    member.setUserId(user.getUid());
                    member.setUserName(user.getDisplayName() != null ? user.getDisplayName() : user.getUsername());
                    member.setUserEmail(user.getEmail() != null ? user.getEmail() : "");
                    member.setRole(GroupRole.MEMBER);
                    member.setJoinedAt(new Date());

                    membersCollection.document(member.getId()).set(member)
                            .addOnSuccessListener(aVoid -> groupsCollection.document(groupId)
                                    .get()
                                    .addOnSuccessListener(groupDoc -> {
                                        Group group = groupDoc.toObject(Group.class);
                                        if (group != null) {
                                            long newCount = (long) group.getMemberCount() + 1;
                                            groupsCollection.document(groupId).update("memberCount", newCount);
                                        }
                                        onSuccess.onSuccess(null);
                                    }))
                            .addOnFailureListener(onFailure);
                })
                .addOnFailureListener(onFailure);
    }
}
