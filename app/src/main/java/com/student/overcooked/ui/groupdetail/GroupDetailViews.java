package com.student.overcooked.ui.groupdetail;

import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.student.overcooked.R;

public final class GroupDetailViews {

    public final @Nullable TextView groupNameText;
    public final @Nullable TextView groupSubjectText;
    public final @Nullable TextView groupProgressText;
    public final @Nullable TextView joinCodeText;
    public final @Nullable TabLayout tabLayout;

    public final @Nullable View tasksContainer;
    public final @Nullable View chatContainer;
    public final @Nullable View membersContainer;
    public final @Nullable View workspaceContainer;
    public final @Nullable View settingsContainer;

    public final @Nullable RecyclerView tasksRecycler;
    public final @Nullable FloatingActionButton fabAddTask;
    public final @Nullable ChipGroup groupTaskStatusFilterChipGroup;

    public final @Nullable RecyclerView chatRecycler;
    public final @Nullable EditText messageInput;
    public final @Nullable ImageButton sendButton;

    public final @Nullable RecyclerView membersRecycler;

    public final @Nullable MaterialButton btnEditProject;
    public final @Nullable MaterialButton btnDeleteProject;

    public final @Nullable ImageView backButton;

    private GroupDetailViews(@NonNull View root) {
        groupNameText = root.findViewById(R.id.groupNameText);
        groupSubjectText = root.findViewById(R.id.groupSubjectText);
        groupProgressText = root.findViewById(R.id.groupProgressText);
        joinCodeText = root.findViewById(R.id.joinCodeText);
        tabLayout = root.findViewById(R.id.tabLayout);

        tasksContainer = root.findViewById(R.id.tasksContainer);
        chatContainer = root.findViewById(R.id.chatContainer);
        membersContainer = root.findViewById(R.id.membersContainer);
        workspaceContainer = root.findViewById(R.id.workspaceContainer);
        settingsContainer = root.findViewById(R.id.settingsContainer);

        tasksRecycler = root.findViewById(R.id.tasksRecycler);
        fabAddTask = root.findViewById(R.id.fabAddTask);
        groupTaskStatusFilterChipGroup = root.findViewById(R.id.groupTaskStatusFilterChipGroup);

        chatRecycler = root.findViewById(R.id.chatRecycler);
        messageInput = root.findViewById(R.id.messageInput);
        sendButton = root.findViewById(R.id.sendButton);

        membersRecycler = root.findViewById(R.id.membersRecycler);

        btnEditProject = root.findViewById(R.id.btnEditProject);
        btnDeleteProject = root.findViewById(R.id.btnDeleteProject);

        backButton = root.findViewById(R.id.backButton);
    }

    public static @NonNull GroupDetailViews bind(@NonNull View root) {
        return new GroupDetailViews(root);
    }
}
