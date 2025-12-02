package com.example.overcooked.ui;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.overcooked.R;
import com.example.overcooked.ui.fragments.GroupDetailFragment;
import com.example.overcooked.ui.fragments.GroupsFragment;
import com.example.overcooked.ui.fragments.HomeFragment;
import com.example.overcooked.ui.fragments.ProfileFragment;
import com.example.overcooked.ui.fragments.TasksFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * Main Activity with Bottom Navigation
 * Hosts the four main fragments: Home, Tasks, Groups, Profile
 */
public class MainNavActivity extends AppCompatActivity {

    private static final String KEY_ACTIVE_TAB = "active_tab";
    private static final String TAG_HOME = "home";
    private static final String TAG_TASKS = "tasks";
    private static final String TAG_GROUPS = "groups";
    private static final String TAG_PROFILE = "profile";
    private static final String TAG_GROUP_DETAIL = "group_detail";

    private BottomNavigationView bottomNavigation;

    // Fragment instances
    private HomeFragment homeFragment;
    private TasksFragment tasksFragment;
    private GroupsFragment groupsFragment;
    private ProfileFragment profileFragment;

    private Fragment activeFragment;
    private int currentTabId = R.id.nav_home;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_nav);

        bottomNavigation = findViewById(R.id.bottom_navigation);

        int initialTab = savedInstanceState != null
                ? savedInstanceState.getInt(KEY_ACTIVE_TAB, R.id.nav_home)
                : R.id.nav_home;

        if (savedInstanceState == null) {
            setupFragments();
        } else {
            restoreFragments(initialTab);
        }

        setupBottomNavigation();
        bottomNavigation.setSelectedItemId(initialTab);
        currentTabId = initialTab;
    }

    private void setupFragments() {
        homeFragment = new HomeFragment();
        tasksFragment = new TasksFragment();
        groupsFragment = new GroupsFragment();
        profileFragment = new ProfileFragment();

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.add(R.id.nav_host_fragment, homeFragment, TAG_HOME);
        transaction.add(R.id.nav_host_fragment, tasksFragment, TAG_TASKS);
        transaction.hide(tasksFragment);
        transaction.add(R.id.nav_host_fragment, groupsFragment, TAG_GROUPS);
        transaction.hide(groupsFragment);
        transaction.add(R.id.nav_host_fragment, profileFragment, TAG_PROFILE);
        transaction.hide(profileFragment);
        transaction.setReorderingAllowed(true);
        transaction.commitNow();
        activeFragment = homeFragment;
    }

    private void setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener(item -> {
            Fragment target = fragmentForItemId(item.getItemId());
            if (target != null) {
                clearGroupDetailIfPresent();
                switchFragment(target);
                currentTabId = item.getItemId();
                return true;
            }
            return false;
        });
    }

    private void switchFragment(Fragment fragment) {
        if (fragment == null || fragment == activeFragment) {
            return;
        }
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        if (activeFragment != null) {
            transaction.hide(activeFragment);
        }
        if (!fragment.isAdded()) {
            transaction.add(R.id.nav_host_fragment, fragment, getTagForFragment(fragment));
        }
        transaction.show(fragment);
        transaction.setReorderingAllowed(true);
        transaction.commit();
        activeFragment = fragment;
    }

    private void restoreFragments(int selectedItemId) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        homeFragment = (HomeFragment) fragmentManager.findFragmentByTag(TAG_HOME);
        tasksFragment = (TasksFragment) fragmentManager.findFragmentByTag(TAG_TASKS);
        groupsFragment = (GroupsFragment) fragmentManager.findFragmentByTag(TAG_GROUPS);
        profileFragment = (ProfileFragment) fragmentManager.findFragmentByTag(TAG_PROFILE);

        if (homeFragment == null) {
            homeFragment = new HomeFragment();
        }
        if (tasksFragment == null) {
            tasksFragment = new TasksFragment();
        }
        if (groupsFragment == null) {
            groupsFragment = new GroupsFragment();
        }
        if (profileFragment == null) {
            profileFragment = new ProfileFragment();
        }

        Fragment target = fragmentForItemId(selectedItemId);
        if (target == null) {
            target = homeFragment;
        }

        FragmentTransaction transaction = fragmentManager.beginTransaction();
        attachFragment(transaction, homeFragment, TAG_HOME, target == homeFragment);
        attachFragment(transaction, tasksFragment, TAG_TASKS, target == tasksFragment);
        attachFragment(transaction, groupsFragment, TAG_GROUPS, target == groupsFragment);
        attachFragment(transaction, profileFragment, TAG_PROFILE, target == profileFragment);
        transaction.setReorderingAllowed(true);
        transaction.commitNow();
        activeFragment = target;
    }

    private void attachFragment(FragmentTransaction transaction, Fragment fragment, String tag, boolean shouldShow) {
        if (fragment == null) {
            return;
        }
        if (!fragment.isAdded()) {
            transaction.add(R.id.nav_host_fragment, fragment, tag);
        }
        if (shouldShow) {
            transaction.show(fragment);
        } else {
            transaction.hide(fragment);
        }
    }

    private void clearGroupDetailIfPresent() {
        if (activeFragment instanceof GroupDetailFragment) {
            getSupportFragmentManager().popBackStackImmediate();
            activeFragment = fragmentForItemId(currentTabId);
        }
    }

    private Fragment fragmentForItemId(int itemId) {
        if (itemId == R.id.nav_home) {
            if (homeFragment == null) {
                homeFragment = new HomeFragment();
            }
            return homeFragment;
        } else if (itemId == R.id.nav_tasks) {
            if (tasksFragment == null) {
                tasksFragment = new TasksFragment();
            }
            return tasksFragment;
        } else if (itemId == R.id.nav_groups) {
            if (groupsFragment == null) {
                groupsFragment = new GroupsFragment();
            }
            return groupsFragment;
        } else if (itemId == R.id.nav_profile) {
            if (profileFragment == null) {
                profileFragment = new ProfileFragment();
            }
            return profileFragment;
        }
        return null;
    }

    private String getTagForFragment(Fragment fragment) {
        if (fragment instanceof HomeFragment) {
            return TAG_HOME;
        } else if (fragment instanceof TasksFragment) {
            return TAG_TASKS;
        } else if (fragment instanceof GroupsFragment) {
            return TAG_GROUPS;
        } else if (fragment instanceof ProfileFragment) {
            return TAG_PROFILE;
        }
        return fragment.getClass().getSimpleName();
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
        GroupDetailFragment fragment = GroupDetailFragment.newInstance(groupId);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        if (activeFragment != null) {
            transaction.hide(activeFragment);
        }
        transaction.add(R.id.nav_host_fragment, fragment, TAG_GROUP_DETAIL + "_" + groupId);
        transaction.addToBackStack(TAG_GROUP_DETAIL);
        transaction.setReorderingAllowed(true);
        transaction.commit();
        activeFragment = fragment;
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
