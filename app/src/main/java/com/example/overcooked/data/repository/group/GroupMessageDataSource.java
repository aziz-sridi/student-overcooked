package com.example.overcooked.data.repository.group;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.overcooked.data.model.GroupMessage;
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

    public GroupMessageDataSource(@NonNull FirebaseAuth auth,
                                  @NonNull com.google.firebase.firestore.CollectionReference messagesCollection) {
        this.auth = auth;
        this.messagesCollection = messagesCollection;
    }

    public LiveData<List<GroupMessage>> getGroupMessages(String groupId) {
        MutableLiveData<List<GroupMessage>> liveData = new MutableLiveData<>();
        messagesCollection.whereEqualTo("groupId", groupId)
                .orderBy("timestamp", Query.Direction.ASCENDING)
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

        GroupMessage msg = new GroupMessage();
        msg.setId(UUID.randomUUID().toString());
        msg.setGroupId(groupId);
        msg.setSenderId(user.getUid());
        msg.setSenderName(user.getDisplayName() != null ? user.getDisplayName() : "Unknown");
        msg.setMessage(message);
        msg.setTimestamp(new Date());

        messagesCollection.document(msg.getId()).set(msg)
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }
}
