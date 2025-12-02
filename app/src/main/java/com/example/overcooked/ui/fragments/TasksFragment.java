package com.example.overcooked.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.overcooked.OvercookedApplication;
import com.example.overcooked.R;
import com.example.overcooked.data.model.Task;
import com.example.overcooked.data.model.TaskStatus;
import com.example.overcooked.data.repository.TaskRepository;
import com.example.overcooked.ui.adapter.TaskListAdapter;
import com.example.overcooked.ui.dialog.AddEditTaskDialog;
import com.example.overcooked.ui.dialog.TaskDetailsDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Tasks Fragment - Personal Tasks List
 * Shows all personal tasks with filter options
 */
public class TasksFragment extends Fragment implements TaskDetailsDialog.OnTaskActionListener, AddEditTaskDialog.OnTaskSavedListener {

    private ChipGroup filterChipGroup;
    private Chip chipAll;
    private Chip chipUpcoming;
    private Chip chipCompleted;
    private Chip chipOverdue;
    private RecyclerView tasksRecycler;
    private TextView taskCountText;
    private View emptyStateLayout;
    private FloatingActionButton fabAddTask;

    private TaskListAdapter taskAdapter;

    private TaskRepository taskRepository;

    private List<Task> allTasks = new ArrayList<>();
    private TaskFilter currentFilter = TaskFilter.ALL;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_tasks, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        taskRepository = ((OvercookedApplication) requireActivity().getApplication()).getTaskRepository();
        
        initializeViews(view);
        setupAdapters();
        setupFilterChips();
        setupClickListeners();
        observeData();
    }

    private void initializeViews(View view) {
        filterChipGroup = view.findViewById(R.id.filterChipGroup);
        chipAll = view.findViewById(R.id.chipAll);
        chipUpcoming = view.findViewById(R.id.chipUpcoming);
        chipCompleted = view.findViewById(R.id.chipCompleted);
        chipOverdue = view.findViewById(R.id.chipOverdue);
        tasksRecycler = view.findViewById(R.id.tasksRecycler);
        taskCountText = view.findViewById(R.id.taskCountText);
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout);
        fabAddTask = view.findViewById(R.id.fabAddTask);
    }

    private void setupAdapters() {
        taskAdapter = new TaskListAdapter(
                task -> {
                    // Open task detail dialog
                    showTaskDetailsDialog(task);
                },
                task -> taskRepository.toggleTaskCompletion(task.getId(), !task.isCompleted()),
                new TaskListAdapter.OnTaskMenuListener() {
                    @Override
                    public void onEditTask(Task task) {
                        // Launch edit task dialog
                        showEditTaskDialog(task);
                    }

                    @Override
                    public void onDeleteTask(Task task) {
                        showDeleteConfirmation(task);
                    }
                }
        );
        tasksRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        tasksRecycler.setAdapter(taskAdapter);
    }

    private void showEditTaskDialog(Task task) {
        AddEditTaskDialog dialog = AddEditTaskDialog.newInstanceEdit(task);
        dialog.setOnTaskSavedListener(this);
        dialog.show(getChildFragmentManager(), "EditTaskDialog");
    }

    private void showAddTaskDialog() {
        AddEditTaskDialog dialog = AddEditTaskDialog.newInstanceAdd();
        dialog.setOnTaskSavedListener(this);
        dialog.show(getChildFragmentManager(), "AddTaskDialog");
    }

    @Override
    public void onTaskSaved(Task task, boolean isNew) {
        if (isNew) {
            task.setCreatedAt(new Date());
            taskRepository.insertTask(task, id -> {});
            Toast.makeText(requireContext(), R.string.task_saved, Toast.LENGTH_SHORT).show();
        } else {
            taskRepository.updateTask(task);
            Toast.makeText(requireContext(), R.string.task_updated, Toast.LENGTH_SHORT).show();
        }
    }

    private void showDeleteConfirmation(Task task) {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.delete_task)
                .setMessage(getString(R.string.delete_task_confirm, task.getTitle()))
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    taskRepository.deleteTask(task);
                    Toast.makeText(requireContext(), R.string.task_deleted, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showTaskDetailsDialog(Task task) {
        TaskDetailsDialog dialog = TaskDetailsDialog.newInstance(task);
        dialog.setOnTaskActionListener(this);
        dialog.show(getChildFragmentManager(), "TaskDetailsDialog");
    }

    @Override
    public void onTaskStatusChanged(Task task, TaskStatus newStatus) {
        // Update task status and completion
        task.setStatus(newStatus);
        boolean isCompleted = (newStatus == TaskStatus.DONE);
        task.setCompleted(isCompleted);
        task.setCompletedAt(isCompleted ? new Date() : null);
        taskRepository.updateTask(task);
        Toast.makeText(requireContext(), R.string.task_updated, Toast.LENGTH_SHORT).show();
    }

    private void setupFilterChips() {
        chipAll.setOnClickListener(v -> {
            currentFilter = TaskFilter.ALL;
            applyFilter();
        });
        chipUpcoming.setOnClickListener(v -> {
            currentFilter = TaskFilter.UPCOMING;
            applyFilter();
        });
        chipCompleted.setOnClickListener(v -> {
            currentFilter = TaskFilter.COMPLETED;
            applyFilter();
        });
        chipOverdue.setOnClickListener(v -> {
            currentFilter = TaskFilter.OVERDUE;
            applyFilter();
        });
    }

    private void setupClickListeners() {
        fabAddTask.setOnClickListener(v -> showAddTaskDialog());
    }

    private void observeData() {
        taskRepository.getAllTasks().observe(getViewLifecycleOwner(), tasks -> {
            allTasks = tasks != null ? tasks : new ArrayList<>();
            applyFilter();
        });
    }

    private void applyFilter() {
        if (allTasks == null) {
            allTasks = new ArrayList<>();
        }
        
        List<Task> filteredTasks = new ArrayList<>();
        
        switch (currentFilter) {
            case ALL:
                for (Task task : allTasks) {
                    if (!task.isCompleted()) filteredTasks.add(task);
                }
                break;
            case UPCOMING:
                for (Task task : allTasks) {
                    if (!task.isCompleted() && !task.isOverdue()) filteredTasks.add(task);
                }
                // Null-safe sorting
                filteredTasks.sort((t1, t2) -> {
                    if (t1.getDeadline() == null && t2.getDeadline() == null) return 0;
                    if (t1.getDeadline() == null) return 1;
                    if (t2.getDeadline() == null) return -1;
                    return t1.getDeadline().compareTo(t2.getDeadline());
                });
                break;
            case COMPLETED:
                for (Task task : allTasks) {
                    if (task.isCompleted()) filteredTasks.add(task);
                }
                break;
            case OVERDUE:
                for (Task task : allTasks) {
                    if (!task.isCompleted() && task.isOverdue()) filteredTasks.add(task);
                }
                break;
        }

        updateTaskList(filteredTasks);
    }

    private void updateTaskList(List<Task> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            if (emptyStateLayout != null) emptyStateLayout.setVisibility(View.VISIBLE);
            if (tasksRecycler != null) tasksRecycler.setVisibility(View.GONE);
            if (taskCountText != null) taskCountText.setText("No tasks");
        } else {
            if (emptyStateLayout != null) emptyStateLayout.setVisibility(View.GONE);
            if (tasksRecycler != null) tasksRecycler.setVisibility(View.VISIBLE);
            if (taskAdapter != null) taskAdapter.submitList(new ArrayList<>(tasks));
            
            String label;
            switch (currentFilter) {
                case ALL: label = "pending"; break;
                case UPCOMING: label = "upcoming"; break;
                case COMPLETED: label = "completed"; break;
                case OVERDUE: label = "overdue"; break;
                default: label = ""; break;
            }
            if (taskCountText != null) {
                taskCountText.setText(tasks.size() + " " + label + " task" + (tasks.size() != 1 ? "s" : ""));
            }
        }
    }

    public enum TaskFilter {
        ALL, UPCOMING, COMPLETED, OVERDUE
    }
}
