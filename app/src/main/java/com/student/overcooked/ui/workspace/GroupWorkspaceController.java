package com.student.overcooked.ui.workspace;

import android.content.Context;
import android.net.Uri;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.student.overcooked.R;
import com.student.overcooked.data.model.ProjectResource;
import com.student.overcooked.data.repository.GroupRepository;
import com.student.overcooked.ui.adapter.ProjectResourceAdapter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

/**
 * Encapsulates workspace-specific UI and logic to keep {@code GroupDetailFragment} smaller.
 */
public class GroupWorkspaceController {

    private final Fragment fragment;
    private final GroupRepository groupRepository;
    private final String groupId;
    private final RecyclerView workspaceRecycler;
    private final View workspaceEmptyState;
    private final View addWorkspaceButton;
    private final ProjectResourceAdapter resourceAdapter;
    private final WorkspaceResourceActions resourceActions;

    private ActivityResultLauncher<String[]> filePickerLauncher;
    private boolean isIndividualProject;

    private @Nullable WorkspaceAddResourceDialog addResourceDialog;

    public GroupWorkspaceController(@NonNull Fragment fragment,
                                    @NonNull GroupRepository groupRepository,
                                    @NonNull String groupId,
                                    @Nullable RecyclerView workspaceRecycler,
                                    @Nullable View workspaceEmptyState,
                                    @Nullable View addWorkspaceButton) {
        this.fragment = fragment;
        this.groupRepository = groupRepository;
        this.groupId = groupId;
        this.workspaceRecycler = workspaceRecycler;
        this.workspaceEmptyState = workspaceEmptyState;
        this.addWorkspaceButton = addWorkspaceButton;

        resourceActions = new WorkspaceResourceActions(fragment, groupRepository);

        Context context = fragment.requireContext();
        resourceAdapter = new ProjectResourceAdapter(new ProjectResourceAdapter.ResourceActionListener() {
            @Override
            public void onResourceClick(ProjectResource resource) {
                resourceActions.handleResourceClick(resource);
            }

            @Override
            public void onResourceDelete(ProjectResource resource) {
                resourceActions.confirmDeleteResource(resource);
            }
        });

        if (this.workspaceRecycler != null) {
            this.workspaceRecycler.setLayoutManager(new LinearLayoutManager(context));
            this.workspaceRecycler.setAdapter(resourceAdapter);
        }
        if (this.addWorkspaceButton != null) {
            this.addWorkspaceButton.setOnClickListener(v -> showAddWorkspaceItemDialog());
        }
    }

    public void setFilePickerLauncher(@Nullable ActivityResultLauncher<String[]> launcher) {
        this.filePickerLauncher = launcher;
    }

    public void setIndividualProject(boolean individualProject) {
        this.isIndividualProject = individualProject;
    }

    public void submitResources(@Nullable List<ProjectResource> resources) {
        resourceAdapter.submitList(resources);
        boolean hasItems = resources != null && !resources.isEmpty();
        if (workspaceRecycler != null) {
            workspaceRecycler.setVisibility(hasItems ? View.VISIBLE : View.GONE);
        }
        if (workspaceEmptyState != null) {
            workspaceEmptyState.setVisibility(hasItems ? View.GONE : View.VISIBLE);
        }
    }

    public void handleFilePicked(@NonNull Uri uri) {
        if (addResourceDialog != null) {
            addResourceDialog.handleFilePicked(uri);
        }
    }

    public void configureFab(@NonNull FloatingActionButton fab) {
        fab.setVisibility(View.VISIBLE);
        fab.setImageResource(R.drawable.ic_add);
        fab.setOnClickListener(v -> showAddWorkspaceItemDialog());
    }

    public ProjectResourceAdapter getAdapter() {
        return resourceAdapter;
    }

    private void showAddWorkspaceItemDialog() {
        addResourceDialog = new WorkspaceAddResourceDialog(fragment, groupRepository, groupId, filePickerLauncher);
        addResourceDialog.show(() -> addResourceDialog = null);
    }
}
