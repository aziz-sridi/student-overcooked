package com.student.overcooked.ui.groupdetail;

import android.view.View;
import androidx.annotation.NonNull;
import com.student.overcooked.ui.fragments.GroupDetailFragment.Section;
import com.student.overcooked.ui.workspace.GroupWorkspaceController;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class GroupDetailSectionNavigator {
    private final View tasksContainer;
    private final View chatContainer;
    private final View membersContainer;
    private final View workspaceContainer;
    private final View settingsContainer;
    private final FloatingActionButton fab;

    private GroupTasksController tasksController;
    private GroupChatController chatController;
    private GroupMembersController membersController;
    private GroupWorkspaceController workspaceController;
    private GroupSettingsController settingsController;

    public GroupDetailSectionNavigator(View tasksContainer,
                                       View chatContainer,
                                       View membersContainer,
                                       View workspaceContainer,
                                       View settingsContainer,
                                       FloatingActionButton fab) {
        this.tasksContainer = tasksContainer;
        this.chatContainer = chatContainer;
        this.membersContainer = membersContainer;
        this.workspaceContainer = workspaceContainer;
        this.settingsContainer = settingsContainer;
        this.fab = fab;
    }

    public void attachControllers(GroupTasksController tasksController,
                                  GroupChatController chatController,
                                  GroupMembersController membersController,
                                  GroupWorkspaceController workspaceController,
                                  GroupSettingsController settingsController) {
        this.tasksController = tasksController;
        this.chatController = chatController;
        this.membersController = membersController;
        this.workspaceController = workspaceController;
        this.settingsController = settingsController;
    }

    public void showSection(@NonNull Section section, boolean isIndividualProject) {
        switch (section) {
            case CHAT:
                showChat();
                break;
            case MEMBERS:
                showMembers();
                break;
            case WORKSPACE:
                if (isIndividualProject) {
                    showWorkspace();
                } else {
                    showTasks();
                }
                break;
            case SETTINGS:
                if (isIndividualProject) {
                    showSettings();
                } else {
                    showTasks();
                }
                break;
            case TASKS:
            default:
                showTasks();
                break;
        }
    }

    public void showTasks() {
        setVisibility(tasksContainer, View.VISIBLE);
        setVisibility(chatContainer, View.GONE);
        setVisibility(membersContainer, View.GONE);
        setVisibility(workspaceContainer, View.GONE);
        setVisibility(settingsContainer, View.GONE);
        if (tasksController != null && fab != null) {
            tasksController.configureFab(fab);
        } else {
            hideFab();
        }
    }

    public void showChat() {
        setVisibility(tasksContainer, View.GONE);
        setVisibility(chatContainer, View.VISIBLE);
        setVisibility(membersContainer, View.GONE);
        setVisibility(workspaceContainer, View.GONE);
        setVisibility(settingsContainer, View.GONE);
        if (fab != null) {
            fab.setVisibility(View.GONE);
            fab.setOnClickListener(null);
        }
    }

    public void showMembers() {
        setVisibility(tasksContainer, View.GONE);
        setVisibility(chatContainer, View.GONE);
        setVisibility(membersContainer, View.VISIBLE);
        setVisibility(workspaceContainer, View.GONE);
        setVisibility(settingsContainer, View.GONE);
        if (membersController != null && fab != null) {
            membersController.configureFab(fab);
        } else {
            hideFab();
        }
    }

    public void showWorkspace() {
        setVisibility(tasksContainer, View.GONE);
        setVisibility(chatContainer, View.GONE);
        setVisibility(membersContainer, View.GONE);
        setVisibility(workspaceContainer, View.VISIBLE);
        setVisibility(settingsContainer, View.GONE);
        if (workspaceController != null && fab != null) {
            workspaceController.configureFab(fab);
        } else {
            hideFab();
        }
    }

    public void showSettings() {
        setVisibility(tasksContainer, View.GONE);
        setVisibility(chatContainer, View.GONE);
        setVisibility(membersContainer, View.GONE);
        setVisibility(workspaceContainer, View.GONE);
        setVisibility(settingsContainer, View.VISIBLE);
        hideFab();
    }

    private void setVisibility(View v, int visibility) {
        if (v != null) v.setVisibility(visibility);
    }

    private void hideFab() {
        if (fab != null) {
            fab.setVisibility(View.GONE);
            fab.setOnClickListener(null);
        }
    }
}
