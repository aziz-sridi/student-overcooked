package com.student.overcooked.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.student.overcooked.data.model.User;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class UserRepository {

    private final FirebaseFirestore firestore;
    private final FirebaseAuth auth;
    private final com.google.firebase.firestore.CollectionReference usersCollection;
    
    // Cached LiveData to share across all observers
    private MutableLiveData<User> currentUserLiveData;
    private String currentListenerUserId;

    public UserRepository() {
        this.firestore = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
        this.usersCollection = firestore.collection("users");
    }

    public LiveData<User> getCurrentUser() {
        FirebaseUser firebaseUser = auth.getCurrentUser();
        
        if (firebaseUser == null) {
            if (currentUserLiveData == null) {
                currentUserLiveData = new MutableLiveData<>();
            }
            currentUserLiveData.setValue(null);
            return currentUserLiveData;
        }

        // If already listening to this user, return cached LiveData
        if (currentUserLiveData != null && firebaseUser.getUid().equals(currentListenerUserId)) {
            return currentUserLiveData;
        }
        
        // Create new LiveData and start listening
        currentUserLiveData = new MutableLiveData<>();
        currentListenerUserId = firebaseUser.getUid();
        
        android.util.Log.d("UserRepository", "Starting user listener for: " + firebaseUser.getUid());

        usersCollection.document(firebaseUser.getUid())
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        android.util.Log.e("UserRepository", "User snapshot error", error);
                        return;
                    }
                    if (snapshot == null) {
                        return;
                    }
                    if (!snapshot.exists()) {
                        currentUserLiveData.setValue(null);
                        return;
                    }
                    User user = snapshot.toObject(User.class);
                    android.util.Log.d("UserRepository", "User updated - coins: " + (user != null ? user.getCoins() : "null"));
                    if (user != null) {
                        // Ensure coins field exists in Firestore (for older users)
                        if (snapshot.getLong("coins") == null) {
                            android.util.Log.d("UserRepository", "Initializing coins field for user: " + firebaseUser.getUid());
                            usersCollection.document(firebaseUser.getUid())
                                    .update("coins", 0)
                                    .addOnFailureListener(e -> 
                                            android.util.Log.w("UserRepository", "Failed to initialize coins", e));
                        }
                    }
                    currentUserLiveData.setValue(user);
                });

        return currentUserLiveData;
    }

    public void updateUserCoins(int newBalance, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        usersCollection.document(user.getUid())
                .update("coins", newBalance)
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    /**
     * Atomically adjusts the user's coin balance in Firestore and returns the new balance.
     * Clamps at zero to prevent negative balances when undoing completions.
     */
    public void updateCoinsBy(int delta,
                              OnSuccessListener<Integer> onSuccess,
                              OnFailureListener onFailure) {
        android.util.Log.d("UserRepository", "updateCoinsBy called with delta: " + delta);
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            android.util.Log.e("UserRepository", "User not logged in");
            if (onFailure != null) {
                onFailure.onFailure(new IllegalStateException("User not logged in"));
            }
            return;
        }

        android.util.Log.d("UserRepository", "User ID: " + user.getUid());
        DocumentReference docRef = usersCollection.document(user.getUid());
        firestore.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(docRef);
            Long currentCoins = snapshot.getLong("coins");
            int newBalance = Math.max(0, (currentCoins != null ? currentCoins.intValue() : 0) + delta);
            android.util.Log.d("UserRepository", "Current coins: " + currentCoins + ", new balance: " + newBalance);
            transaction.update(docRef, "coins", newBalance);
            return newBalance;
        }).addOnSuccessListener(balance -> {
            android.util.Log.d("UserRepository", "Transaction successful, new balance: " + balance);
            if (onSuccess != null) {
                onSuccess.onSuccess(balance);
            }
        }).addOnFailureListener(e -> {
            android.util.Log.e("UserRepository", "Transaction failed", e);
            if (onFailure != null) {
                onFailure.onFailure(e);
            }
        });
    }

    public void addItemToInventory(String itemId, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        usersCollection.document(user.getUid())
                .update("inventory", FieldValue.arrayUnion(itemId))
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    public void updateUserDisplayName(String displayName,
                                      OnSuccessListener<Void> onSuccess,
                                      OnFailureListener onFailure) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        String safe = displayName != null ? displayName.trim() : "";
        Map<String, Object> updates = new HashMap<>();
        updates.put("displayName", safe);

        usersCollection.document(user.getUid())
                .update(updates)
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }
}
