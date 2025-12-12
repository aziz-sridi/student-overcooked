package com.student.overcooked.ui;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.student.overcooked.R;
import com.student.overcooked.ui.focus.FocusModeFragment;
import com.student.overcooked.ui.fragments.GroupDetailFragment;
import com.student.overcooked.ui.fragments.GroupsFragment;
import com.student.overcooked.ui.fragments.HomeFragment;
import com.student.overcooked.ui.fragments.ProfileFragment;
import com.student.overcooked.ui.fragments.TasksFragment;

final class MainNavFragments {

    private static final String TAG_FOCUS = "focus";
    private static final String TAG_HOME = "home";
    private static final String TAG_TASKS = "tasks";
    private static final String TAG_GROUPS = "groups";
    private static final String TAG_PROFILE = "profile";
    private static final String TAG_GROUP_DETAIL = "group_detail";

    private final AppCompatActivity activity;

    private FocusModeFragment focusFragment;
    private HomeFragment homeFragment;
    private TasksFragment tasksFragment;
    private GroupsFragment groupsFragment;
    private ProfileFragment profileFragment;

    private Fragment activeFragment;

    MainNavFragments(@NonNull AppCompatActivity activity) {
        this.activity = activity;
    }

    void setupNew() {
        focusFragment = new FocusModeFragment();
        homeFragment = new HomeFragment();
        tasksFragment = new TasksFragment();
        groupsFragment = new GroupsFragment();
        profileFragment = new ProfileFragment();

        FragmentTransaction transaction = activity.getSupportFragmentManager().beginTransaction();
        transaction.add(R.id.nav_host_fragment, focusFragment, TAG_FOCUS);
        transaction.hide(focusFragment);
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

    void restore(int selectedItemId) {
        FragmentManager fragmentManager = activity.getSupportFragmentManager();
        focusFragment = (FocusModeFragment) fragmentManager.findFragmentByTag(TAG_FOCUS);
        homeFragment = (HomeFragment) fragmentManager.findFragmentByTag(TAG_HOME);
        tasksFragment = (TasksFragment) fragmentManager.findFragmentByTag(TAG_TASKS);
        groupsFragment = (GroupsFragment) fragmentManager.findFragmentByTag(TAG_GROUPS);
        profileFragment = (ProfileFragment) fragmentManager.findFragmentByTag(TAG_PROFILE);

        if (focusFragment == null) {
            focusFragment = new FocusModeFragment();
        }
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
        attachFragment(transaction, focusFragment, TAG_FOCUS, target == focusFragment);
        attachFragment(transaction, homeFragment, TAG_HOME, target == homeFragment);
        attachFragment(transaction, tasksFragment, TAG_TASKS, target == tasksFragment);
        attachFragment(transaction, groupsFragment, TAG_GROUPS, target == groupsFragment);
        attachFragment(transaction, profileFragment, TAG_PROFILE, target == profileFragment);
        transaction.setReorderingAllowed(true);
        transaction.commitNow();
        activeFragment = target;
    }

    @Nullable
    Fragment fragmentForItemId(int itemId) {
        if (itemId == R.id.nav_focus) {
            if (focusFragment == null) {
                focusFragment = new FocusModeFragment();
            }
            return focusFragment;
        }
        if (itemId == R.id.nav_home) {
            if (homeFragment == null) {
                homeFragment = new HomeFragment();
            }
            return homeFragment;
        }
        if (itemId == R.id.nav_tasks) {
            if (tasksFragment == null) {
                tasksFragment = new TasksFragment();
            }
            return tasksFragment;
        }
        if (itemId == R.id.nav_groups) {
            if (groupsFragment == null) {
                groupsFragment = new GroupsFragment();
            }
            return groupsFragment;
        }
        if (itemId == R.id.nav_profile) {
            if (profileFragment == null) {
                profileFragment = new ProfileFragment();
            }
            return profileFragment;
        }
        return null;
    }

    void clearGroupDetailIfPresent(int currentTabId) {
        if (activeFragment instanceof GroupDetailFragment) {
            activity.getSupportFragmentManager().popBackStackImmediate();
            activeFragment = fragmentForItemId(currentTabId);
        }
    }

    void switchTo(@NonNull Fragment fragment) {
        if (fragment == activeFragment) {
            return;
        }
        FragmentTransaction transaction = activity.getSupportFragmentManager().beginTransaction();
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

    void navigateToGroupDetail(@NonNull String groupId) {
        GroupDetailFragment fragment = GroupDetailFragment.newInstance(groupId);
        FragmentTransaction transaction = activity.getSupportFragmentManager().beginTransaction();
        if (activeFragment != null) {
            transaction.hide(activeFragment);
        }
        transaction.add(R.id.nav_host_fragment, fragment, TAG_GROUP_DETAIL + "_" + groupId);
        transaction.addToBackStack(TAG_GROUP_DETAIL);
        transaction.setReorderingAllowed(true);
        transaction.commit();
        activeFragment = fragment;
    }

    @NonNull
    private String getTagForFragment(@NonNull Fragment fragment) {
        if (fragment instanceof FocusModeFragment) {
            return TAG_FOCUS;
        }
        if (fragment instanceof HomeFragment) {
            return TAG_HOME;
        }
        if (fragment instanceof TasksFragment) {
            return TAG_TASKS;
        }
        if (fragment instanceof GroupsFragment) {
            return TAG_GROUPS;
        }
        if (fragment instanceof ProfileFragment) {
            return TAG_PROFILE;
        }
        return fragment.getClass().getSimpleName();
    }

    private void attachFragment(@NonNull FragmentTransaction transaction,
                                @NonNull Fragment fragment,
                                @NonNull String tag,
                                boolean shouldShow) {
        if (!fragment.isAdded()) {
            transaction.add(R.id.nav_host_fragment, fragment, tag);
        }
        if (shouldShow) {
            transaction.show(fragment);
        } else {
            transaction.hide(fragment);
        }
    }
}
