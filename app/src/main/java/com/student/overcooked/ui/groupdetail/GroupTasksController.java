package com.student.overcooked.ui.groupdetail;

import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.student.overcooked.R;
import com.student.overcooked.data.model.GroupMember;
import com.student.overcooked.data.model.GroupTask;
import com.student.overcooked.data.model.TaskStatus;
import com.student.overcooked.data.repository.GroupRepository;
import com.student.overcooked.ui.adapter.GroupTaskAdapter;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates all task-related UI interactions for {@link com.student.overcooked.ui.fragments.GroupDetailFragment}.
 */
public class GroupTasksController {

    private final Fragment fragment;
    private final GroupRepository groupRepository;
    private final String groupId;
    private final RecyclerView tasksRecycler;

    private final GroupTaskAdapter taskAdapter;
    private final GroupTaskComposerDialog composerDialog;
    private final List<GroupMember> currentMembers = new ArrayList<>();
    private boolean isIndividualProject;

    @Nullable
    private final ChipGroup statusFilterChipGroup;
    @NonNull
    private List<GroupTask> latestTasks = new ArrayList<>();
    @Nullable
    private TaskStatus activeStatusFilter;

    public GroupTasksController(@NonNull Fragment fragment,
                                @NonNull GroupRepository groupRepository,
                                @NonNull String groupId,
                                @NonNull RecyclerView tasksRecycler,
                                @Nullable ChipGroup statusFilterChipGroup) {
        this.fragment = fragment;
        this.groupRepository = groupRepository;
        this.groupId = groupId;
        this.tasksRecycler = tasksRecycler;
        this.statusFilterChipGroup = statusFilterChipGroup;
        this.composerDialog = new GroupTaskComposerDialog(fragment, groupRepository, groupId);
        this.taskAdapter = new GroupTaskAdapter(new GroupTaskAdapter.TaskInteractionListener() {
            @Override
            public void onTaskSelected(GroupTask task) {
                showGroupTaskDetailsDialog(task);
            }

            @Override
            public void onTaskCompletionToggle(GroupTask task) {
                toggleTaskCompletion(task);
            }

            @Override
            public void onTaskMenuRequested(@NonNull View anchor, GroupTask task) {
                showTaskOverflowMenu(anchor, task);
            }

            @Override
            public void onTaskLongPressed(@NonNull View anchor, GroupTask task) {
                showTaskOverflowMenu(anchor, task);
            }
        });
        this.tasksRecycler.setLayoutManager(new LinearLayoutManager(fragment.requireContext()));
        this.tasksRecycler.setAdapter(taskAdapter);
        GroupTaskSwipeGestures.attach(tasksRecycler, taskAdapter, this::toggleTaskCompletion, this::confirmDeleteTask);
        configureStatusFilterChips();
    }

    public void submitTasks(@Nullable List<GroupTask> tasks) {
        latestTasks = tasks != null ? new ArrayList<>(tasks) : new ArrayList<>();
        applyFilterAndSubmit();
    }

    private void configureStatusFilterChips() {
        if (statusFilterChipGroup == null) {
            return;
        }
        statusFilterChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds == null || checkedIds.isEmpty()) {
                activeStatusFilter = null;
            } else {
                int checkedId = checkedIds.get(0);
                if (checkedId == R.id.chipFilterNotStarted) {
                    activeStatusFilter = TaskStatus.NOT_STARTED;
                } else if (checkedId == R.id.chipFilterInProgress) {
                    activeStatusFilter = TaskStatus.IN_PROGRESS;
                } else if (checkedId == R.id.chipFilterDone) {
                    activeStatusFilter = TaskStatus.DONE;
                } else {
                    activeStatusFilter = null;
                }
            }
            applyFilterAndSubmit();
        });
    }

    private void applyFilterAndSubmit() {
        List<GroupTask> filtered = new ArrayList<>();
        if (activeStatusFilter == null) {
            filtered.addAll(latestTasks);
        } else {
            for (GroupTask task : latestTasks) {
                TaskStatus status = task != null ? task.getStatus() : null;
                if (status == activeStatusFilter) {
                    filtered.add(task);
                }
            }
        }
        taskAdapter.submitList(filtered);
    }

    private void updateCachedTask(@NonNull GroupTask updatedTask) {
        for (int i = 0; i < latestTasks.size(); i++) {
            GroupTask existing = latestTasks.get(i);
            if (existing != null && TextUtils.equals(existing.getId(), updatedTask.getId())) {
                latestTasks.set(i, updatedTask);
                return;
            }
        }
    }

    public void setMembers(@Nullable List<GroupMember> members) {
        currentMembers.clear();
        if (members != null) {
            // Only include approved members (not pending)
            for (GroupMember member : members) {
                if (!member.isPending()) {
                    currentMembers.add(member);
                }
            }
        }
    }

    public void setIndividualProject(boolean individualProject) {
        isIndividualProject = individualProject;
    }

    public void configureFab(@Nullable FloatingActionButton fab) {
        if (fab == null) {
            return;
        }
        fab.setVisibility(View.VISIBLE);
        fab.setImageResource(R.drawable.ic_add);
        fab.setOnClickListener(v -> showTaskComposerDialog(null));
    }

    private void showTaskOverflowMenu(@NonNull View anchor, @Nullable GroupTask task) {
        if (!fragment.isAdded() || task == null) {
            return;
        }
        GroupTaskActionsMenu.show(
                fragment,
                anchor,
                task,
                this::showGroupTaskDetailsDialog,
                this::setTaskStatus,
                this::toggleTaskCompletion,
                this::showTaskComposerDialog,
                this::confirmDeleteTask
        );
    }

    @Nullable
    private GroupTask findTaskById(@NonNull String taskId) {
        for (GroupTask t : latestTasks) {
            if (t != null && TextUtils.equals(t.getId(), taskId)) {
                return t;
            }
        }
        for (GroupTask t : taskAdapter.getCurrentList()) {
            if (t != null && TextUtils.equals(t.getId(), taskId)) {
                return t;
            }
        }
        return null;
    }

    private void setTaskStatus(@NonNull GroupTask task, @NonNull com.student.overcooked.data.model.TaskStatus status) {
        // Optimistic UI update
        boolean newCompleted = (status == com.student.overcooked.data.model.TaskStatus.DONE);
        task.setStatus(status);
        task.setCompleted(newCompleted);
        task.setCompletedAt(newCompleted ? new java.util.Date() : null);
        updateCachedTask(task);
        tasksRecycler.post(this::applyFilterAndSubmit);

        groupRepository.updateGroupTaskStatus(task, status,
                aVoid -> {
                },
                e -> fragment.requireActivity().runOnUiThread(() ->
                        Toast.makeText(fragment.requireContext(), R.string.error_occurred, Toast.LENGTH_SHORT).show()));
    }

    private void toggleTaskCompletion(@Nullable GroupTask task) {
        if (task == null) {
            return;
        }
        // Optimistic UI update: flip completion and refresh list immediately
        boolean newCompleted = !task.isCompleted();
        task.setCompleted(newCompleted);
        task.setCompletedAt(newCompleted ? new java.util.Date() : null);
        task.setStatus(newCompleted ? com.student.overcooked.data.model.TaskStatus.DONE : com.student.overcooked.data.model.TaskStatus.NOT_STARTED);
        updateCachedTask(task);
        tasksRecycler.post(this::applyFilterAndSubmit);
        groupRepository.toggleGroupTaskCompletion(task,
                aVoid -> {
                },
                e -> fragment.requireActivity().runOnUiThread(() ->
                        Toast.makeText(fragment.requireContext(), R.string.error_occurred, Toast.LENGTH_SHORT).show()));
    }

    private void confirmDeleteTask(@Nullable GroupTask task) {
        if (!fragment.isAdded() || task == null) {
            return;
        }
        new MaterialAlertDialogBuilder(fragment.requireContext())
                .setTitle(R.string.delete_task_title)
                .setMessage(fragment.getString(R.string.delete_task_confirm, task.getTitle()))
                .setPositiveButton(R.string.delete_task, (dialog, which) ->
                        groupRepository.deleteGroupTask(task,
                                aVoid -> fragment.requireActivity().runOnUiThread(() ->
                                        Toast.makeText(fragment.requireContext(), R.string.task_deleted, Toast.LENGTH_SHORT).show()),
                                e -> fragment.requireActivity().runOnUiThread(() ->
                                        Toast.makeText(fragment.requireContext(), R.string.error_occurred, Toast.LENGTH_SHORT).show())))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showTaskComposerDialog(@Nullable GroupTask taskToEdit) {
        composerDialog.show(taskToEdit, isIndividualProject, currentMembers);
    }

    private void showGroupTaskDetailsDialog(@Nullable GroupTask task) {
        if (!fragment.isAdded() || task == null) {
            return;
        }
        GroupTaskDetailsLauncher.show(fragment, task, this::findTaskById, this::setTaskStatus);
    }
}
