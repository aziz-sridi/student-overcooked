package com.student.overcooked.ui.groupdetail;

import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.student.overcooked.R;
import com.student.overcooked.data.repository.GroupRepository;
import com.student.overcooked.ui.workspace.GroupWorkspaceController;

public final class GroupDetailControllersFactory {

    public static final class SectionControllers {
        public final @Nullable GroupTasksController tasksController;
        public final @Nullable GroupChatController chatController;
        public final @Nullable GroupMembersController membersController;
        public final @Nullable GroupSettingsController settingsController;

        private SectionControllers(
                @Nullable GroupTasksController tasksController,
                @Nullable GroupChatController chatController,
                @Nullable GroupMembersController membersController,
                @Nullable GroupSettingsController settingsController
        ) {
            this.tasksController = tasksController;
            this.chatController = chatController;
            this.membersController = membersController;
            this.settingsController = settingsController;
        }
    }

    private GroupDetailControllersFactory() {
    }

    public static @Nullable GroupWorkspaceController createWorkspaceController(
            @NonNull Fragment fragment,
            @NonNull GroupRepository groupRepository,
            @NonNull String groupId,
            @NonNull View root,
            @NonNull ActivityResultLauncher<String[]> filePickerLauncher
    ) {
        RecyclerView workspaceRecycler = root.findViewById(R.id.workspaceRecycler);
        View workspaceEmptyState = root.findViewById(R.id.workspaceEmptyState);
        View addWorkspaceButton = root.findViewById(R.id.btnAddWorkspaceItem);

        GroupWorkspaceController controller = new GroupWorkspaceController(
                fragment,
                groupRepository,
                groupId,
                workspaceRecycler,
                workspaceEmptyState,
                addWorkspaceButton
        );
        controller.setFilePickerLauncher(filePickerLauncher);
        return controller;
    }

    public static @NonNull SectionControllers createSectionControllers(
            @NonNull Fragment fragment,
            @NonNull GroupRepository groupRepository,
            @NonNull String groupId,
            @Nullable RecyclerView tasksRecycler,
            @Nullable ChipGroup groupTaskStatusFilterChipGroup,
            @Nullable RecyclerView chatRecycler,
            @Nullable android.widget.EditText messageInput,
            @Nullable android.widget.ImageButton sendButton,
            @Nullable RecyclerView membersRecycler,
            @Nullable MaterialButton btnEditProject,
            @Nullable MaterialButton btnDeleteProject
    ) {
        GroupTasksController tasksController = null;
        if (tasksRecycler != null) {
            tasksController = new GroupTasksController(fragment, groupRepository, groupId, tasksRecycler, groupTaskStatusFilterChipGroup);
        }

        GroupChatController chatController = null;
        if (chatRecycler != null && messageInput != null && sendButton != null) {
            chatController = new GroupChatController(fragment, groupRepository, groupId, chatRecycler, messageInput, sendButton);
        }

        GroupMembersController membersController = null;
        if (membersRecycler != null) {
            membersController = new GroupMembersController(fragment, groupRepository, groupId, membersRecycler);
        }

        GroupSettingsController settingsController = null;
        if (btnEditProject != null && btnDeleteProject != null) {
            settingsController = new GroupSettingsController(fragment, groupRepository, groupId, btnEditProject, btnDeleteProject);
        }

        return new SectionControllers(tasksController, chatController, membersController, settingsController);
    }
}
