package com.student.overcooked.ui.home;

import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.student.overcooked.R;
import com.student.overcooked.data.model.Task;
import com.student.overcooked.data.model.TaskStatus;
import com.student.overcooked.data.repository.TaskRepository;
import com.student.overcooked.ui.adapter.WorkNowTaskAdapter;
import com.student.overcooked.ui.dialog.AddEditTaskDialog;
import com.student.overcooked.ui.dialog.TaskDetailsDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Coordinates the "Work Now" list, dialogs, and task persistence for HomeFragment.
 */
public class WorkNowController implements TaskDetailsDialog.OnTaskActionListener, AddEditTaskDialog.OnTaskSavedListener {

    private final Fragment fragment;
    private final TaskRepository taskRepository;
    private final RecyclerView workNowRecycler;
    private final View emptyStateLayout;
    private final FloatingActionButton fabAddTask;

    private final WorkNowTaskAdapter workNowAdapter;

    public WorkNowController(@NonNull Fragment fragment,
                             @NonNull TaskRepository taskRepository,
                             @Nullable RecyclerView workNowRecycler,
                             @Nullable View emptyStateLayout,
                             @Nullable FloatingActionButton fabAddTask) {
        this.fragment = fragment;
        this.taskRepository = taskRepository;
        this.workNowRecycler = workNowRecycler;
        this.emptyStateLayout = emptyStateLayout;
        this.fabAddTask = fabAddTask;
        this.workNowAdapter = new WorkNowTaskAdapter(
                this::showTaskDetailsDialog,
                task -> taskRepository.toggleTaskCompletion(task.getId(), true),
                new WorkNowTaskAdapter.OnTaskMenuListener() {
                    @Override
                    public void onEditTask(Task task) {
                        showEditTaskDialog(task);
                    }

                    @Override
                    public void onDeleteTask(Task task) {
                        showDeleteConfirmation(task);
                    }
                }
        );
        if (this.workNowRecycler != null) {
            this.workNowRecycler.setLayoutManager(new LinearLayoutManager(fragment.requireContext()));
            this.workNowRecycler.setAdapter(workNowAdapter);
        }
        if (this.fabAddTask != null) {
            this.fabAddTask.setOnClickListener(v -> showAddTaskDialog());
        }
    }

    public void submitTasks(@Nullable List<Task> tasks) {
        List<Task> safeTasks = tasks != null ? tasks : Collections.emptyList();
        List<Task> priorityTasks = new ArrayList<>();
        for (Task task : safeTasks) {
            if (!task.isCompleted() && task.getDeadline() != null) {
                priorityTasks.add(task);
            }
        }
        priorityTasks.sort((t1, t2) -> {
            if (t1.getDeadline() == null && t2.getDeadline() == null) return 0;
            if (t1.getDeadline() == null) return 1;
            if (t2.getDeadline() == null) return -1;
            return t1.getDeadline().compareTo(t2.getDeadline());
        });
        if (priorityTasks.size() > 5) {
            priorityTasks = priorityTasks.subList(0, 5);
        }
        if (priorityTasks.isEmpty()) {
            if (emptyStateLayout != null) emptyStateLayout.setVisibility(View.VISIBLE);
            if (workNowRecycler != null) workNowRecycler.setVisibility(View.GONE);
        } else {
            if (emptyStateLayout != null) emptyStateLayout.setVisibility(View.GONE);
            if (workNowRecycler != null) workNowRecycler.setVisibility(View.VISIBLE);
            workNowAdapter.submitList(priorityTasks);
        }
    }

    private void showEditTaskDialog(@NonNull Task task) {
        AddEditTaskDialog dialog = AddEditTaskDialog.newInstanceEdit(task);
        dialog.setOnTaskSavedListener(this);
        dialog.show(fragment.getChildFragmentManager(), "EditTaskDialog");
    }

    private void showAddTaskDialog() {
        AddEditTaskDialog dialog = AddEditTaskDialog.newInstanceAdd();
        dialog.setOnTaskSavedListener(this);
        dialog.show(fragment.getChildFragmentManager(), "AddTaskDialog");
    }

    private void showDeleteConfirmation(@NonNull Task task) {
        new AlertDialog.Builder(fragment.requireContext())
                .setTitle(R.string.delete_task)
                .setMessage(fragment.getString(R.string.delete_task_confirm, task.getTitle()))
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    taskRepository.deleteTask(task);
                    Toast.makeText(fragment.requireContext(), R.string.task_deleted, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showTaskDetailsDialog(@NonNull Task task) {
        TaskDetailsDialog dialog = TaskDetailsDialog.newInstance(task);
        dialog.setOnTaskActionListener(this);
        dialog.show(fragment.getChildFragmentManager(), "TaskDetailsDialog");
    }

    @Override
    public void onTaskStatusChanged(Task task, TaskStatus newStatus) {
        task.setStatus(newStatus);
        boolean isCompleted = (newStatus == TaskStatus.DONE);
        task.setCompleted(isCompleted);
        task.setCompletedAt(isCompleted ? new Date() : null);
        taskRepository.updateTask(task);
        Toast.makeText(fragment.requireContext(), R.string.task_updated, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onTaskSaved(Task task, boolean isNew) {
        if (isNew) {
            task.setCreatedAt(new Date());
            taskRepository.insertTask(task, id -> {});
            Toast.makeText(fragment.requireContext(), R.string.task_saved, Toast.LENGTH_SHORT).show();
        } else {
            taskRepository.updateTask(task);
            Toast.makeText(fragment.requireContext(), R.string.task_updated, Toast.LENGTH_SHORT).show();
        }
    }
}
