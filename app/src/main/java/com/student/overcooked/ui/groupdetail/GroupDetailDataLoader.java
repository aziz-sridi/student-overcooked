package com.student.overcooked.ui.groupdetail;

import android.content.Context;
import android.text.TextUtils;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.student.overcooked.R;
import com.student.overcooked.data.model.Group;
import com.student.overcooked.data.repository.GroupRepository;
import com.student.overcooked.ui.fragments.GroupDetailFragment.Section;
import com.student.overcooked.ui.workspace.GroupWorkspaceController;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/** Wires GroupRepository data into the Group Detail screen controllers and header UI. */
public final class GroupDetailDataLoader {

    public interface ActiveSectionAccessor {
        @NonNull Section get();
        void set(@NonNull Section section);
    }

    private final Fragment fragment;
    private final GroupRepository groupRepository;
    private final String groupId;

    private final @Nullable TextView groupNameText;
    private final @Nullable TextView groupSubjectText;
    private final @Nullable TextView groupProgressText;
    private final @Nullable TextView joinCodeText;

    private final @Nullable GroupWorkspaceController workspaceController;
    private final @Nullable GroupTasksController tasksController;
    private final @Nullable GroupChatController chatController;
    private final @Nullable GroupMembersController membersController;
    private final @Nullable GroupSettingsController settingsController;

    private final @Nullable GroupDetailTabManager tabManager;
    private final @Nullable GroupDetailSectionNavigator sectionNavigator;

    private @Nullable Group currentGroup;
    private boolean isAdmin;
    private boolean canManageProject;
    private boolean isIndividualProject;

    public GroupDetailDataLoader(
            @NonNull Fragment fragment,
            @NonNull GroupRepository groupRepository,
            @NonNull String groupId,
            @Nullable TextView groupNameText,
            @Nullable TextView groupSubjectText,
            @Nullable TextView groupProgressText,
            @Nullable TextView joinCodeText,
            @Nullable GroupWorkspaceController workspaceController,
            @Nullable GroupTasksController tasksController,
            @Nullable GroupChatController chatController,
            @Nullable GroupMembersController membersController,
            @Nullable GroupSettingsController settingsController,
            @Nullable GroupDetailTabManager tabManager,
            @Nullable GroupDetailSectionNavigator sectionNavigator
    ) {
        this.fragment = fragment;
        this.groupRepository = groupRepository;
        this.groupId = groupId;
        this.groupNameText = groupNameText;
        this.groupSubjectText = groupSubjectText;
        this.groupProgressText = groupProgressText;
        this.joinCodeText = joinCodeText;
        this.workspaceController = workspaceController;
        this.tasksController = tasksController;
        this.chatController = chatController;
        this.membersController = membersController;
        this.settingsController = settingsController;
        this.tabManager = tabManager;
        this.sectionNavigator = sectionNavigator;
    }

    public @Nullable Group getCurrentGroup() {
        return currentGroup;
    }

    public boolean isIndividualProject() {
        return isIndividualProject;
    }

    public void start(@NonNull ActiveSectionAccessor activeSection) {
        if (TextUtils.isEmpty(groupId)) return;

        groupRepository.getGroup(groupId, group -> {
            currentGroup = group;
            if (group == null || !fragment.isAdded()) return;

            fragment.requireActivity().runOnUiThread(() -> {
                bindGroupInfo(group);
                isIndividualProject = group.isIndividualProject();

                if (workspaceController != null) workspaceController.setIndividualProject(isIndividualProject);
                if (tasksController != null) tasksController.setIndividualProject(isIndividualProject);
                if (settingsController != null) settingsController.setGroup(group);
                if (membersController != null) membersController.setGroupName(group.getName());

                if (tabManager != null) {
                    tabManager.setIsIndividualProject(isIndividualProject);
                    tabManager.setActiveSection(activeSection.get());
                    tabManager.rebuildTabs();
                    activeSection.set(tabManager.getActiveSection());
                }

                if (sectionNavigator != null) {
                    sectionNavigator.showSection(activeSection.get(), isIndividualProject);
                }

                updateManagementFlags(activeSection.get());
            });
        }, e -> {
            if (fragment.isAdded()) {
                fragment.requireActivity().runOnUiThread(() ->
                        Toast.makeText(fragment.requireContext(), "Failed to load group.", Toast.LENGTH_SHORT).show()
                );
            }
        });

        groupRepository.isGroupAdmin(groupId, admin -> {
            isAdmin = admin != null && admin;
            updateManagementFlags(activeSection.get());
        }, e -> {
        });

        groupRepository.getGroupTasks(groupId).observe(fragment.getViewLifecycleOwner(), tasks -> {
            if (tasksController != null) tasksController.submitTasks(tasks);
        });

        groupRepository.getGroupMessages(groupId).observe(fragment.getViewLifecycleOwner(), messages -> {
            if (chatController != null) chatController.submitMessages(messages);
        });

        groupRepository.getGroupMembers(groupId).observe(fragment.getViewLifecycleOwner(), members -> {
            if (membersController != null) membersController.submitMembers(members);
            if (tasksController != null) tasksController.setMembers(members);
        });

        groupRepository.getProjectResources(groupId).observe(fragment.getViewLifecycleOwner(), resources -> {
            if (workspaceController != null) workspaceController.submitResources(resources);
        });
    }

    private void bindGroupInfo(@NonNull Group group) {
        if (groupNameText != null) groupNameText.setText(group.getName());

        if (groupSubjectText != null) {
            String subject = group.getSubject();
            groupSubjectText.setText(TextUtils.isEmpty(subject)
                    ? fragment.getString(R.string.not_connected)
                    : subject);
        }

        if (groupProgressText != null) {
            groupProgressText.setText(group.getProgressText());
        }

        if (joinCodeText != null) {
            String joinCode = !TextUtils.isEmpty(group.getJoinCode())
                    ? group.getJoinCode()
                    : fragment.getString(R.string.not_connected);
            joinCodeText.setText("Code: " + joinCode);
        }
    }

    private void updateManagementFlags(@NonNull Section section) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        boolean isOwner = currentGroup != null
                && user != null
                && TextUtils.equals(user.getUid(), currentGroup.getCreatedBy());

        canManageProject = isAdmin || isOwner;

        if (!fragment.isAdded()) return;
        fragment.requireActivity().runOnUiThread(() -> {
            if (settingsController != null) {
                settingsController.setAccess(canManageProject, isIndividualProject);
            }
            if (membersController != null) {
                membersController.setAdmin(isAdmin);
            }
            if (sectionNavigator != null) {
                sectionNavigator.showSection(section, isIndividualProject);
            }
        });
    }

    public void copyJoinCodeToClipboard() {
        if (currentGroup == null || !fragment.isAdded()) return;

        String joinCode = currentGroup.getJoinCode();
        if (TextUtils.isEmpty(joinCode)) {
            Toast.makeText(fragment.requireContext(), "No join code available.", Toast.LENGTH_SHORT).show();
            return;
        }

        android.content.ClipboardManager clipboard =
                (android.content.ClipboardManager) fragment.requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Join Code", joinCode));
            Toast.makeText(fragment.requireContext(), "Join code copied!", Toast.LENGTH_SHORT).show();
        }
    }
}
