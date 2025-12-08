package com.student.overcooked.data.repository.group;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.student.overcooked.data.model.Group;
import com.student.overcooked.data.model.GroupMember;
import com.student.overcooked.data.model.GroupRole;
import com.student.overcooked.data.model.ProjectInvitation;
import com.student.overcooked.data.model.User;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Handles project invitations - sending, accepting, declining
 */
public class GroupInvitationDataSource {

    private final FirebaseAuth auth;
    private final CollectionReference invitationsCollection;
    private final CollectionReference usersCollection;
    private final CollectionReference groupsCollection;
    private final CollectionReference membersCollection;

    public GroupInvitationDataSource(@NonNull FirebaseAuth auth,
                                     @NonNull CollectionReference invitationsCollection,
                                     @NonNull CollectionReference usersCollection,
                                     @NonNull CollectionReference groupsCollection,
                                     @NonNull CollectionReference membersCollection) {
        this.auth = auth;
        this.invitationsCollection = invitationsCollection;
        this.usersCollection = usersCollection;
        this.groupsCollection = groupsCollection;
        this.membersCollection = membersCollection;
    }

    /**
     * Get all pending invitations for the current user
     */
    public LiveData<List<ProjectInvitation>> getPendingInvitations() {
        MutableLiveData<List<ProjectInvitation>> liveData = new MutableLiveData<>();
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            liveData.setValue(new ArrayList<>());
            return liveData;
        }

        invitationsCollection
                .whereEqualTo("invitedUserId", currentUser.getUid())
                .whereEqualTo("status", ProjectInvitation.InvitationStatus.PENDING.name())
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null || snapshot == null) {
                        liveData.setValue(new ArrayList<>());
                        return;
                    }
                    List<ProjectInvitation> invitations = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        ProjectInvitation invitation = doc.toObject(ProjectInvitation.class);
                        if (invitation != null) {
                            invitations.add(invitation);
                        }
                    }
                    liveData.setValue(invitations);
                });
        return liveData;
    }

    /**
     * Send an invitation to a user by username or email
     */
    public void sendInvitation(String groupId, String groupName, String usernameOrEmail,
                               OnSuccessListener<ProjectInvitation> onSuccess,
                               OnFailureListener onFailure) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            onFailure.onFailure(new Exception("Not authenticated"));
            return;
        }

        // Find user by username or email
        findUserByUsernameOrEmail(usernameOrEmail, user -> {
            if (user == null) {
                onFailure.onFailure(new Exception("User not found"));
                return;
            }

            // Check if user is already a member
            membersCollection.whereEqualTo("groupId", groupId)
                    .whereEqualTo("userId", user.getUid())
                    .get()
                    .addOnSuccessListener(memberSnapshot -> {
                        if (!memberSnapshot.isEmpty()) {
                            onFailure.onFailure(new Exception("User is already a member"));
                            return;
                        }

                        // Check if there's already a pending invitation
                        invitationsCollection
                                .whereEqualTo("groupId", groupId)
                                .whereEqualTo("invitedUserId", user.getUid())
                                .whereEqualTo("status", ProjectInvitation.InvitationStatus.PENDING.name())
                                .get()
                                .addOnSuccessListener(inviteSnapshot -> {
                                    if (!inviteSnapshot.isEmpty()) {
                                        onFailure.onFailure(new Exception("Invitation already sent"));
                                        return;
                                    }

                                    // Create the invitation
                                    ProjectInvitation invitation = new ProjectInvitation();
                                    invitation.setId(UUID.randomUUID().toString());
                                    invitation.setGroupId(groupId);
                                    invitation.setGroupName(groupName != null ? groupName : "Project");
                                    invitation.setInvitedUserId(user.getUid());
                                    invitation.setInvitedUserEmail(user.getEmail());
                                    invitation.setInvitedByUserId(currentUser.getUid());
                                    invitation.setInvitedByUserName(currentUser.getDisplayName() != null 
                                            ? currentUser.getDisplayName() : "Someone");
                                    invitation.setCreatedAt(new Date());
                                    invitation.setStatus(ProjectInvitation.InvitationStatus.PENDING);

                                    invitationsCollection.document(invitation.getId())
                                            .set(invitation)
                                            .addOnSuccessListener(aVoid -> onSuccess.onSuccess(invitation))
                                            .addOnFailureListener(onFailure);
                                })
                                .addOnFailureListener(onFailure);
                    })
                    .addOnFailureListener(onFailure);
        }, onFailure);
    }

    private void findUserByUsernameOrEmail(String usernameOrEmail, 
                                           OnSuccessListener<User> onSuccess, 
                                           OnFailureListener onFailure) {
        // Try to find by username first
        usersCollection.whereEqualTo("username", usernameOrEmail)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        User user = querySnapshot.getDocuments().get(0).toObject(User.class);
                        onSuccess.onSuccess(user);
                    } else {
                        // Try by email
                        usersCollection.whereEqualTo("email", usernameOrEmail)
                                .limit(1)
                                .get()
                                .addOnSuccessListener(emailSnapshot -> {
                                    if (!emailSnapshot.isEmpty()) {
                                        User user = emailSnapshot.getDocuments().get(0).toObject(User.class);
                                        onSuccess.onSuccess(user);
                                    } else {
                                        onSuccess.onSuccess(null);
                                    }
                                })
                                .addOnFailureListener(onFailure);
                    }
                })
                .addOnFailureListener(onFailure);
    }

    /**
     * Accept an invitation
     */
    public void acceptInvitation(ProjectInvitation invitation,
                                 OnSuccessListener<Void> onSuccess,
                                 OnFailureListener onFailure) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            onFailure.onFailure(new Exception("Not authenticated"));
            return;
        }

        // Update invitation status
        invitationsCollection.document(invitation.getId())
                .update("status", ProjectInvitation.InvitationStatus.ACCEPTED.name())
                .addOnSuccessListener(aVoid -> {
                    // Add user as member
                    GroupMember member = new GroupMember();
                    member.setId(UUID.randomUUID().toString());
                    member.setGroupId(invitation.getGroupId());
                    member.setUserId(currentUser.getUid());
                    member.setUserName(currentUser.getDisplayName() != null 
                            ? currentUser.getDisplayName() : "User");
                    member.setUserEmail(currentUser.getEmail() != null 
                            ? currentUser.getEmail() : "");
                    member.setRole(GroupRole.MEMBER);
                    member.setJoinedAt(new Date());

                    membersCollection.document(member.getId()).set(member)
                            .addOnSuccessListener(aVoid2 -> {
                                // Update group member count and set as team project
                                groupsCollection.document(invitation.getGroupId())
                                        .get()
                                        .addOnSuccessListener(groupDoc -> {
                                            Group group = groupDoc.toObject(Group.class);
                                            if (group != null) {
                                                int newCount = group.getMemberCount() + 1;
                                                groupsCollection.document(invitation.getGroupId())
                                                        .update(
                                                                "memberCount", newCount,
                                                                "individualProject", false
                                                        )
                                                        .addOnSuccessListener(onSuccess)
                                                        .addOnFailureListener(onFailure);
                                            } else {
                                                onSuccess.onSuccess(null);
                                            }
                                        })
                                        .addOnFailureListener(onFailure);
                            })
                            .addOnFailureListener(onFailure);
                })
                .addOnFailureListener(onFailure);
    }

    /**
     * Decline an invitation
     */
    public void declineInvitation(ProjectInvitation invitation,
                                  OnSuccessListener<Void> onSuccess,
                                  OnFailureListener onFailure) {
        invitationsCollection.document(invitation.getId())
                .update("status", ProjectInvitation.InvitationStatus.DECLINED.name())
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    /**
     * Get invitations count for badge
     */
    public LiveData<Integer> getPendingInvitationsCount() {
        MutableLiveData<Integer> liveData = new MutableLiveData<>(0);
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            return liveData;
        }

        invitationsCollection
                .whereEqualTo("invitedUserId", currentUser.getUid())
                .whereEqualTo("status", ProjectInvitation.InvitationStatus.PENDING.name())
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null || snapshot == null) {
                        liveData.setValue(0);
                        return;
                    }
                    liveData.setValue(snapshot.size());
                });
        return liveData;
    }
}
