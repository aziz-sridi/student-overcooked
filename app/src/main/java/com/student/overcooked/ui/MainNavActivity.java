package com.student.overcooked.ui;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;

import android.widget.Toast;

import com.student.overcooked.R;
import com.student.overcooked.OvercookedApplication;
import com.student.overcooked.data.repository.GroupRepository;
import com.student.overcooked.ui.fragments.GroupDetailFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.student.overcooked.util.ConnectivityObserver;
import com.student.overcooked.util.NotificationHelper;
import com.student.overcooked.util.NotificationSettings;
import com.student.overcooked.sync.SyncWorker;
import com.student.overcooked.data.LocalCoinStore;

/**
 * Main Activity with Bottom Navigation
 * Hosts the four main fragments: Home, Tasks, Groups, Profile
 */
public class MainNavActivity extends AppCompatActivity {

    private static final String KEY_ACTIVE_TAB = "active_tab";

    private BottomNavigationView bottomNavigation;

    private MainNavFragments navFragments;
    private MainNavInAppNotifications inAppNotifications;
    private int currentTabId = R.id.nav_home;

    private ConnectivityObserver connectivityObserver;
    private Observer<Boolean> connectivityListener;
    private boolean lastOnline = true;

    private GroupRepository groupRepository;
    private NotificationHelper notificationHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_nav);

        groupRepository = ((OvercookedApplication) getApplication()).getGroupRepository();
        notificationHelper = new NotificationHelper(this);

        connectivityObserver = new ConnectivityObserver(this);

        bottomNavigation = findViewById(R.id.bottom_navigation);
        navFragments = new MainNavFragments(this);

        int initialTab = savedInstanceState != null
                ? savedInstanceState.getInt(KEY_ACTIVE_TAB, R.id.nav_home)
                : R.id.nav_home;

        if (savedInstanceState == null) {
            navFragments.setupNew();
        } else {
            navFragments.restore(initialTab);
        }

        setupBottomNavigation();
        bottomNavigation.setSelectedItemId(initialTab);
        currentTabId = initialTab;

        setupConnectivityListener();

        // Observe for notifications while app is in foreground.
        inAppNotifications = new MainNavInAppNotifications(this, groupRepository, notificationHelper);
        inAppNotifications.setupIfEnabled(this);

        // Handle navigation requests if launched from a notification.
        handleNavigationIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleNavigationIntent(intent);
    }

    private void handleNavigationIntent(Intent intent) {
        if (intent == null) return;
        String navigateTo = intent.getStringExtra("navigate_to");
        String groupId = intent.getStringExtra("group_id");

        if (navigateTo == null) return;

        if ("tasks".equals(navigateTo)) {
            navigateToTasks();
            return;
        }
        if ("groups".equals(navigateTo)) {
            navigateToGroups();
            if (groupId != null && !groupId.isEmpty()) {
                // Delay until the Groups fragment is active.
                bottomNavigation.post(() -> navigateToGroupDetail(groupId));
            }
        }
    }

    private void setupInAppNotificationObservers() {
        // moved to MainNavInAppNotifications
    }

    private void setupConnectivityListener() {
        connectivityListener = online -> {
            if (online == null) return;
            if (!online) {
                if (lastOnline) {
                    Toast.makeText(this, "You are offline. Progress will sync when back online.", Toast.LENGTH_SHORT).show();
                }
                lastOnline = false;
            } else {
                if (!lastOnline) {
                    Toast.makeText(this, "Back online. Syncing progress…", Toast.LENGTH_SHORT).show();
                }
                lastOnline = true;
                if (new LocalCoinStore(this).getPendingDelta() != 0) {
                    SyncWorker.enqueue(this);
                }
            }
        };
    }

    private void setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener(item -> {
            Fragment target = navFragments.fragmentForItemId(item.getItemId());
            if (target != null) {
                navFragments.clearGroupDetailIfPresent(currentTabId);
                navFragments.switchTo(target);
                currentTabId = item.getItemId();
                return true;
            }
            return false;
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        connectivityObserver.start();
        connectivityObserver.isOnline().observe(this, connectivityListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        connectivityObserver.stop();
        connectivityObserver.isOnline().removeObserver(connectivityListener);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (bottomNavigation != null) {
            outState.putInt(KEY_ACTIVE_TAB, bottomNavigation.getSelectedItemId());
        }
    }

    /**
     * Navigate to group detail fragment
     */
    public void navigateToGroupDetail(String groupId) {
        navFragments.navigateToGroupDetail(groupId);
    }

    /**
     * Navigate to tasks tab programmatically
     */
    public void navigateToTasks() {
        bottomNavigation.setSelectedItemId(R.id.nav_tasks);
    }

    /**
     * Navigate to groups tab programmatically
     */
    public void navigateToGroups() {
        bottomNavigation.setSelectedItemId(R.id.nav_groups);
    }

}
