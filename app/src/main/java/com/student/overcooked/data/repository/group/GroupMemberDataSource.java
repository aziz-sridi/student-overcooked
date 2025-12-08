package com.student.overcooked.data.repository.group;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.student.overcooked.data.model.Group;
import com.student.overcooked.data.model.GroupMember;
import com.student.overcooked.data.model.GroupRole;
import com.student.overcooked.data.model.User;
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
                    
                    // Always enrich members with user data to ensure names are populated
                    enrichMembersWithUserData(members, liveData);
                });
        return liveData;
    }

    private void enrichMembersWithUserData(List<GroupMember> members, MutableLiveData<List<GroupMember>> liveData) {
        if (members.isEmpty()) {
            liveData.setValue(members);
            return;
        }

        int[] completedCount = {0};
        
        for (GroupMember member : members) {
            usersCollection.document(member.getUserId())
                    .get()
                    .addOnSuccessListener(userDoc -> {
                        User user = userDoc.toObject(User.class);
                        if (user != null) {
                            String name = null;
                            
                            // Try displayName first
                            if (user.getDisplayName() != null && !user.getDisplayName().isEmpty()) {
                                name = user.getDisplayName();
                            }
                            // Fall back to username
                            else if (user.getUsername() != null && !user.getUsername().isEmpty()) {
                                name = user.getUsername();
                            }
                            // Last resort: try email prefix
                            else if (user.getEmail() != null && !user.getEmail().isEmpty()) {
                                name = user.getEmail().split("@")[0];
                            }
                            
                            if (name != null && !name.isEmpty()) {
                                member.setUserName(name);
                                android.util.Log.d("MemberEnrichment", "Set member name: " + name + " (ID: " + member.getUserId() + ")");
                            } else {
                                android.util.Log.w("MemberEnrichment", "No name found for user " + member.getUserId());
                                member.setUserName("Unknown");
                            }
                            
                            if (user.getEmail() != null && !user.getEmail().isEmpty()) {
                                member.setUserEmail(user.getEmail());
                            }
                        } else {
                            android.util.Log.w("MemberEnrichment", "User document is null for " + member.getUserId());
                            member.setUserName("Unknown");
                        }
                        completedCount[0]++;
                        if (completedCount[0] == members.size()) {
                            liveData.setValue(members);
                        }
                    })
                    .addOnFailureListener(e -> {
                        android.util.Log.e("MemberEnrichment", "Failed to fetch user data for " + member.getUserId() + ": " + e.getMessage());
                        member.setUserName("Unknown");
                        completedCount[0]++;
                        if (completedCount[0] == members.size()) {
                            liveData.setValue(members);
                        }
                    });
        }
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

    public void approveMember(String memberId, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        membersCollection.document(memberId)
                .update("pending", false)
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    public void rejectMember(String groupId, String memberId, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        // Just remove the pending member
        removeMember(groupId, memberId, onSuccess, onFailure);
    }
}
