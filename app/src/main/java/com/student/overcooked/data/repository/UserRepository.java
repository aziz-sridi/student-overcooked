package com.student.overcooked.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.student.overcooked.data.model.User;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

public class UserRepository {

    private final FirebaseFirestore firestore;
    private final FirebaseAuth auth;
    private final com.google.firebase.firestore.CollectionReference usersCollection;

    public UserRepository() {
        this.firestore = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
        this.usersCollection = firestore.collection("users");
    }

    public LiveData<User> getCurrentUser() {
        MutableLiveData<User> liveData = new MutableLiveData<>();
        FirebaseUser firebaseUser = auth.getCurrentUser();
        
        if (firebaseUser == null) {
            liveData.setValue(null);
            return liveData;
        }

        usersCollection.document(firebaseUser.getUid())
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null || snapshot == null) {
                        return;
                    }
                    User user = snapshot.toObject(User.class);
                    liveData.setValue(user);
                });

        return liveData;
    }

    public void updateUserCoins(int newBalance, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        usersCollection.document(user.getUid())
                .update("coins", newBalance)
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    public void addItemToInventory(String itemId, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        usersCollection.document(user.getUid())
                .update("inventory", FieldValue.arrayUnion(itemId))
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }
}
