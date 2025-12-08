package com.student.overcooked.util;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class to migrate/fix user data in Firestore
 * Run this once to populate missing displayName and username fields
 */
public class FirebaseDataMigration {

    private static final String TAG = "FirebaseDataMigration";
    private final FirebaseFirestore firestore;
    private final FirebaseAuth auth;

    public FirebaseDataMigration(FirebaseFirestore firestore, FirebaseAuth auth) {
        this.firestore = firestore;
        this.auth = auth;
    }

    /**
     * Migrates all user documents to ensure displayName and username are populated
     * This should be called once during app initialization
     */
    public void migrateUserDisplayNames(OnMigrationCompleteListener listener) {
        firestore.collection("users")
                .get()
                .addOnSuccessListener(snapshot -> {
                    int totalUsers = snapshot.size();
                    int[] migratedCount = {0};
                    int[] errorCount = {0};

                    if (totalUsers == 0) {
                        Log.d(TAG, "No users to migrate");
                        listener.onComplete(0, 0);
                        return;
                    }

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        migrateUserDocument(doc, () -> {
                            migratedCount[0]++;
                            if (migratedCount[0] + errorCount[0] == totalUsers) {
                                listener.onComplete(migratedCount[0], errorCount[0]);
                            }
                        }, e -> {
                            errorCount[0]++;
                            if (migratedCount[0] + errorCount[0] == totalUsers) {
                                listener.onComplete(migratedCount[0], errorCount[0]);
                            }
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch users: " + e.getMessage(), e);
                    listener.onError(e);
                });
    }

    private void migrateUserDocument(DocumentSnapshot doc, Runnable onSuccess, OnErrorListener onError) {
        String userId = doc.getId();
        String email = doc.getString("email");
        String displayName = doc.getString("displayName");
        String username = doc.getString("username");

        // Check if migration is needed
        if ((displayName != null && !displayName.isEmpty()) && 
            (username != null && !username.isEmpty())) {
            onSuccess.run();
            return;
        }

        // Get Firebase Auth user for reference
        FirebaseUser authUser = auth.getCurrentUser();
        String nameFromAuth = authUser != null && userId.equals(authUser.getUid()) 
                ? authUser.getDisplayName() 
                : null;

        // Generate missing fields
        String newDisplayName = displayName;
        String newUsername = username;

        if (newDisplayName == null || newDisplayName.isEmpty()) {
            if (nameFromAuth != null && !nameFromAuth.isEmpty()) {
                newDisplayName = nameFromAuth;
            } else if (username != null && !username.isEmpty()) {
                newDisplayName = username;
            } else if (email != null && !email.isEmpty()) {
                newDisplayName = email.split("@")[0];
            }
        }

        if (newUsername == null || newUsername.isEmpty()) {
            if (displayName != null && !displayName.isEmpty()) {
                newUsername = displayName;
            } else if (nameFromAuth != null && !nameFromAuth.isEmpty()) {
                newUsername = nameFromAuth;
            } else if (email != null && !email.isEmpty()) {
                newUsername = email.split("@")[0];
            }
        }

        // Update user document
        Map<String, Object> updates = new HashMap<>();
        if (newDisplayName != null && !newDisplayName.isEmpty()) {
            updates.put("displayName", newDisplayName);
        }
        if (newUsername != null && !newUsername.isEmpty()) {
            updates.put("username", newUsername);
        }

        if (updates.isEmpty()) {
            Log.w(TAG, "No updates needed for user: " + userId);
            onSuccess.run();
            return;
        }

        // Use final variables for lambda
        final String finalDisplayName = newDisplayName;
        final String finalUsername = newUsername;

        firestore.collection("users").document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Migrated user: " + userId + " -> displayName: " + finalDisplayName + ", username: " + finalUsername);
                    onSuccess.run();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to migrate user " + userId + ": " + e.getMessage(), e);
                    onError.onError(e);
                });
    }

    public interface OnMigrationCompleteListener {
        void onComplete(int migratedCount, int errorCount);
        void onError(Exception e);
    }

    public interface OnErrorListener {
        void onError(Exception e);
    }
}
