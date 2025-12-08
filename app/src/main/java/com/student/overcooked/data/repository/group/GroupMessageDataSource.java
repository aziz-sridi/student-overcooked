package com.student.overcooked.data.repository.group;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.student.overcooked.data.model.GroupMessage;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Handles chat message streaming and sending for group conversations.
 */
public class GroupMessageDataSource {

    private final FirebaseAuth auth;
    private final com.google.firebase.firestore.CollectionReference messagesCollection;
    private final com.google.firebase.firestore.CollectionReference usersCollection;

    public GroupMessageDataSource(@NonNull FirebaseAuth auth,
                                  @NonNull com.google.firebase.firestore.CollectionReference messagesCollection,
                                  @NonNull com.google.firebase.firestore.CollectionReference usersCollection) {
        this.auth = auth;
        this.messagesCollection = messagesCollection;
        this.usersCollection = usersCollection;
    }

    public LiveData<List<GroupMessage>> getGroupMessages(String groupId) {
        MutableLiveData<List<GroupMessage>> liveData = new MutableLiveData<>();
        messagesCollection.whereEqualTo("groupId", groupId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null || snapshot == null) {
                        liveData.setValue(new ArrayList<>());
                        return;
                    }
                    List<GroupMessage> messages = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        GroupMessage message = doc.toObject(GroupMessage.class);
                        if (message != null) {
                            messages.add(message);
                        }
                    }
                    // Sort by timestamp in memory to avoid needing composite index
                    messages.sort((m1, m2) -> {
                        if (m1.getTimestamp() == null && m2.getTimestamp() == null) return 0;
                        if (m1.getTimestamp() == null) return 1;
                        if (m2.getTimestamp() == null) return -1;
                        return m1.getTimestamp().compareTo(m2.getTimestamp());
                    });
                    liveData.setValue(messages);
                });
        return liveData;
    }

    public void sendMessage(String groupId, String message, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            onFailure.onFailure(new IllegalStateException("User not logged in"));
            return;
        }

        // Fetch user data from Firestore to get the correct display name
        usersCollection.document(user.getUid())
                .get()
                .addOnSuccessListener(userDoc -> {
                    com.student.overcooked.data.model.User userData = userDoc.toObject(com.student.overcooked.data.model.User.class);
                    String senderName = "Unknown";
                    
                    if (userData != null) {
                        // Try displayName first
                        if (userData.getDisplayName() != null && !userData.getDisplayName().isEmpty()) {
                            senderName = userData.getDisplayName();
                        }
                        // Fall back to username
                        else if (userData.getUsername() != null && !userData.getUsername().isEmpty()) {
                            senderName = userData.getUsername();
                        }
                        // Last resort: email prefix
                        else if (userData.getEmail() != null && !userData.getEmail().isEmpty()) {
                            senderName = userData.getEmail().split("@")[0];
                        }
                    } else if (user.getDisplayName() != null && !user.getDisplayName().isEmpty()) {
                        // Fallback to Firebase Auth if Firestore document doesn't exist
                        senderName = user.getDisplayName();
                    }

                    GroupMessage msg = new GroupMessage();
                    msg.setId(UUID.randomUUID().toString());
                    msg.setGroupId(groupId);
                    msg.setSenderId(user.getUid());
                    msg.setSenderName(senderName);
                    msg.setMessage(message);
                    msg.setTimestamp(new Date());

                    messagesCollection.document(msg.getId()).set(msg)
                            .addOnSuccessListener(onSuccess)
                            .addOnFailureListener(onFailure);
                })
                .addOnFailureListener(onFailure);
    }
}
