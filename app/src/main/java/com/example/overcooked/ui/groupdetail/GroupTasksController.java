package com.example.overcooked.ui.groupdetail;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.overcooked.R;
import com.example.overcooked.data.model.GroupMember;
import com.example.overcooked.data.model.GroupTask;
import com.example.overcooked.data.model.Priority;
import com.example.overcooked.data.repository.GroupRepository;
import com.example.overcooked.ui.adapter.GroupTaskAdapter;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Encapsulates all task-related UI interactions for {@link com.example.overcooked.ui.fragments.GroupDetailFragment}.
 */
public class GroupTasksController {

    private final Fragment fragment;
    private final GroupRepository groupRepository;
    private final String groupId;
    private final RecyclerView tasksRecycler;

    private final GroupTaskAdapter taskAdapter;
    private final List<GroupMember> currentMembers = new ArrayList<>();
    private boolean isIndividualProject;

    public GroupTasksController(@NonNull Fragment fragment,
                                @NonNull GroupRepository groupRepository,
                                @NonNull String groupId,
                                @NonNull RecyclerView tasksRecycler) {
        this.fragment = fragment;
        this.groupRepository = groupRepository;
        this.groupId = groupId;
        this.tasksRecycler = tasksRecycler;
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
        attachTaskSwipeGestures();
    }

    public void submitTasks(@Nullable List<GroupTask> tasks) {
        taskAdapter.submitList(tasks);
    }

    public void setMembers(@Nullable List<GroupMember> members) {
        currentMembers.clear();
        if (members != null) {
            currentMembers.addAll(members);
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
        PopupMenu popupMenu = new PopupMenu(fragment.requireContext(), anchor);
        popupMenu.inflate(R.menu.menu_group_task_actions);
        if (task.isCompleted()) {
            popupMenu.getMenu().findItem(R.id.action_toggle_complete)
                    .setTitle(R.string.group_task_mark_incomplete);
        } else {
            popupMenu.getMenu().findItem(R.id.action_toggle_complete)
                    .setTitle(R.string.group_task_mark_complete);
        }
        popupMenu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.action_view) {
                showGroupTaskDetailsDialog(task);
                return true;
            } else if (itemId == R.id.action_toggle_complete) {
                toggleTaskCompletion(task);
                return true;
            } else if (itemId == R.id.action_edit) {
                showTaskComposerDialog(task);
                return true;
            } else if (itemId == R.id.action_delete) {
                confirmDeleteTask(task);
                return true;
            }
            return false;
        });
        popupMenu.show();
    }

    private void toggleTaskCompletion(@Nullable GroupTask task) {
        if (task == null) {
            return;
        }
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

    private void attachTaskSwipeGestures() {
        ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(0,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                if (position == RecyclerView.NO_POSITION || position >= taskAdapter.getCurrentList().size()) {
                    return;
                }
                GroupTask task = taskAdapter.getCurrentList().get(position);
                if (direction == ItemTouchHelper.RIGHT) {
                    toggleTaskCompletion(task);
                    tasksRecycler.post(() -> taskAdapter.notifyItemChanged(position));
                } else if (direction == ItemTouchHelper.LEFT) {
                    tasksRecycler.post(() -> taskAdapter.notifyItemChanged(position));
                    confirmDeleteTask(task);
                }
            }
        };
        new ItemTouchHelper(callback).attachToRecyclerView(tasksRecycler);
    }

    private void showTaskComposerDialog(@Nullable GroupTask taskToEdit) {
        if (!fragment.isAdded()) {
            return;
        }
        boolean isEditing = taskToEdit != null;
        View dialogView = LayoutInflater.from(fragment.requireContext()).inflate(R.layout.dialog_add_group_task, null);
        TextView heading = dialogView.findViewById(R.id.dialogTitle);
        TextInputEditText titleInput = dialogView.findViewById(R.id.editTaskTitle);
        TextInputEditText descInput = dialogView.findViewById(R.id.editTaskDescription);
        MaterialButton dateButton = dialogView.findViewById(R.id.btnSelectDate);
        MaterialButton timeButton = dialogView.findViewById(R.id.btnSelectTime);
        ChipGroup priorityGroup = dialogView.findViewById(R.id.chipGroupPriority);
        TextView assigneeLabel = dialogView.findViewById(R.id.assigneeLabel);
        TextInputLayout assigneeLayout = dialogView.findViewById(R.id.assigneeInputLayout);
        MaterialAutoCompleteTextView assigneeDropdown = dialogView.findViewById(R.id.assigneeDropdown);

        if (heading != null) {
            heading.setText(fragment.getString(isEditing ? R.string.dialog_edit_group_task_title : R.string.dialog_add_group_task_title));
        }

        final Calendar deadlineCalendar = Calendar.getInstance();
        final boolean[] hasCustomDate = {false};
        final boolean[] hasCustomTime = {false};
        final DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM);
        final DateFormat timeFormat = DateFormat.getTimeInstance(DateFormat.SHORT);

        if (isEditing) {
            if (titleInput != null) {
                titleInput.setText(taskToEdit.getTitle());
            }
            if (descInput != null) {
                descInput.setText(taskToEdit.getDescription());
            }
            if (taskToEdit.getDeadline() != null) {
                deadlineCalendar.setTime(taskToEdit.getDeadline());
                hasCustomDate[0] = true;
                hasCustomTime[0] = true;
                dateButton.setText(dateFormat.format(taskToEdit.getDeadline()));
                timeButton.setText(timeFormat.format(taskToEdit.getDeadline()));
            }
            if (priorityGroup != null && taskToEdit.getPriority() != null) {
                if (taskToEdit.getPriority() == Priority.HIGH) {
                    priorityGroup.check(R.id.chipHigh);
                } else if (taskToEdit.getPriority() == Priority.LOW) {
                    priorityGroup.check(R.id.chipLow);
                } else {
                    priorityGroup.check(R.id.chipMedium);
                }
            }
        }

        dateButton.setOnClickListener(v -> {
            DatePickerDialog picker = new DatePickerDialog(fragment.requireContext(), (view, year, month, dayOfMonth) -> {
                deadlineCalendar.set(Calendar.YEAR, year);
                deadlineCalendar.set(Calendar.MONTH, month);
                deadlineCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                hasCustomDate[0] = true;
                dateButton.setText(dateFormat.format(deadlineCalendar.getTime()));
            }, deadlineCalendar.get(Calendar.YEAR), deadlineCalendar.get(Calendar.MONTH), deadlineCalendar.get(Calendar.DAY_OF_MONTH));
            picker.getDatePicker().setMinDate(System.currentTimeMillis());
            picker.show();
        });

        timeButton.setOnClickListener(v -> {
            TimePickerDialog picker = new TimePickerDialog(fragment.requireContext(), (view, hourOfDay, minute) -> {
                deadlineCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                deadlineCalendar.set(Calendar.MINUTE, minute);
                deadlineCalendar.set(Calendar.SECOND, 0);
                deadlineCalendar.set(Calendar.MILLISECOND, 0);
                hasCustomTime[0] = true;
                timeButton.setText(timeFormat.format(deadlineCalendar.getTime()));
            }, deadlineCalendar.get(Calendar.HOUR_OF_DAY), deadlineCalendar.get(Calendar.MINUTE), false);
            picker.show();
        });

        final String[] selectedAssigneeId = {isEditing ? taskToEdit.getAssigneeId() : null};
        final String[] selectedAssigneeName = {isEditing ? taskToEdit.getAssigneeName() : null};
        if (isIndividualProject || currentMembers.isEmpty()) {
            assigneeLabel.setVisibility(View.GONE);
            assigneeLayout.setVisibility(View.GONE);
        } else {
            assigneeLabel.setVisibility(View.VISIBLE);
            assigneeLayout.setVisibility(View.VISIBLE);
            List<String> options = new ArrayList<>();
            options.add(fragment.getString(R.string.task_assignee_unassigned));
            for (GroupMember member : currentMembers) {
                options.add(member.getUserName());
            }
            boolean hasExistingAssignee = isEditing && !TextUtils.isEmpty(taskToEdit.getAssigneeName());
            if (hasExistingAssignee && !options.contains(taskToEdit.getAssigneeName())) {
                options.add(taskToEdit.getAssigneeName());
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(fragment.requireContext(), android.R.layout.simple_list_item_1, options);
            assigneeDropdown.setAdapter(adapter);
            if (hasExistingAssignee) {
                assigneeDropdown.setText(taskToEdit.getAssigneeName(), false);
            } else {
                assigneeDropdown.setText(options.get(0), false);
            }
            assigneeDropdown.setOnItemClickListener((parent, view, position, id) -> {
                if (position == 0) {
                    selectedAssigneeId[0] = null;
                    selectedAssigneeName[0] = null;
                } else {
                    GroupMember member = position - 1 < currentMembers.size() ? currentMembers.get(position - 1) : null;
                    if (member != null) {
                        selectedAssigneeId[0] = member.getUserId();
                        selectedAssigneeName[0] = member.getUserName();
                    }
                }
            });
        }

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(fragment.requireContext())
                .setTitle(isEditing ? R.string.dialog_edit_group_task_title : R.string.dialog_add_group_task_title)
                .setView(dialogView)
                .setPositiveButton(isEditing ? R.string.save_changes : R.string.add_task_button, null)
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(d -> {
            Button positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positive.setOnClickListener(v -> {
                String title = titleInput != null && titleInput.getText() != null
                        ? titleInput.getText().toString().trim()
                        : "";
                if (TextUtils.isEmpty(title)) {
                    if (titleInput != null) {
                        titleInput.setError(fragment.getString(R.string.task_title_required));
                    }
                    return;
                }

                if (!hasCustomDate[0]) {
                    if (isEditing && taskToEdit.getDeadline() != null) {
                        deadlineCalendar.setTime(taskToEdit.getDeadline());
                    } else {
                        deadlineCalendar.setTime(new Date(System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000));
                    }
                }
                if (!hasCustomTime[0]) {
                    if (isEditing && taskToEdit.getDeadline() != null) {
                        deadlineCalendar.setTime(taskToEdit.getDeadline());
                    }
                    deadlineCalendar.set(Calendar.HOUR_OF_DAY, 23);
                    deadlineCalendar.set(Calendar.MINUTE, 59);
                    deadlineCalendar.set(Calendar.SECOND, 0);
                    deadlineCalendar.set(Calendar.MILLISECOND, 0);
                }

                String description = descInput != null && descInput.getText() != null
                        ? descInput.getText().toString().trim()
                        : "";
                Priority priority = resolvePriority(priorityGroup);
                Date finalDeadline = deadlineCalendar.getTime();

                positive.setEnabled(false);

                if (isEditing) {
                    groupRepository.updateGroupTask(
                            taskToEdit,
                            title,
                            description,
                            finalDeadline,
                            selectedAssigneeId[0],
                            selectedAssigneeName[0],
                            priority,
                            task -> fragment.requireActivity().runOnUiThread(() -> {
                                Toast.makeText(fragment.requireContext(), R.string.group_task_updated, Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                            }),
                            e -> fragment.requireActivity().runOnUiThread(() -> {
                                positive.setEnabled(true);
                                Toast.makeText(fragment.requireContext(), R.string.error_occurred, Toast.LENGTH_SHORT).show();
                            })
                    );
                } else {
                    groupRepository.createGroupTask(
                            groupId,
                            title,
                            description,
                            finalDeadline,
                            selectedAssigneeId[0],
                            selectedAssigneeName[0],
                            priority,
                            task -> fragment.requireActivity().runOnUiThread(() -> {
                                Toast.makeText(fragment.requireContext(), R.string.group_task_created, Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                            }),
                            e -> fragment.requireActivity().runOnUiThread(() -> {
                                positive.setEnabled(true);
                                Toast.makeText(fragment.requireContext(), R.string.error_occurred, Toast.LENGTH_SHORT).show();
                            })
                    );
                }
            });
        });

        dialog.show();
    }

    private void showGroupTaskDetailsDialog(@Nullable GroupTask task) {
        if (!fragment.isAdded() || task == null) {
            return;
        }

        View dialogView = LayoutInflater.from(fragment.requireContext()).inflate(R.layout.dialog_group_task_details, null);
        TextView titleText = dialogView.findViewById(R.id.taskTitle);
        TextView metaText = dialogView.findViewById(R.id.taskMeta);
        TextView statusChip = dialogView.findViewById(R.id.taskStatusChip);
        TextView deadlineText = dialogView.findViewById(R.id.taskDeadline);
        TextView assigneeText = dialogView.findViewById(R.id.taskAssignee);
        TextView priorityText = dialogView.findViewById(R.id.taskPriority);
        TextView descriptionLabel = dialogView.findViewById(R.id.descriptionLabel);
        TextView descriptionText = dialogView.findViewById(R.id.taskDescription);
        MaterialButton toggleButton = dialogView.findViewById(R.id.btnToggleComplete);
        View closeButton = dialogView.findViewById(R.id.btnClose);

        titleText.setText(task.getTitle());
        DateFormat metaFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);
        Date createdAt = task.getCreatedAt();
        metaText.setText(fragment.getString(R.string.group_task_meta_template,
                createdAt != null ? metaFormat.format(createdAt) : fragment.getString(R.string.not_connected)));

        boolean completed = task.isCompleted();
        statusChip.setText(completed ? fragment.getString(R.string.status_done) : fragment.getString(R.string.status_in_progress));
        int statusColor = ContextCompat.getColor(fragment.requireContext(),
                completed ? R.color.successGreen : R.color.mustardYellow);
        statusChip.setTextColor(statusColor);

        Date deadline = task.getDeadline();
        if (deadline != null) {
            DateFormat deadlineFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);
            deadlineText.setText(deadlineFormat.format(deadline));
            if (!completed && deadline.before(new Date())) {
                deadlineText.setTextColor(ContextCompat.getColor(fragment.requireContext(), R.color.tomatoRed));
            } else {
                deadlineText.setTextColor(ContextCompat.getColor(fragment.requireContext(), R.color.textSecondary));
            }
        } else {
            deadlineText.setText(R.string.project_deadline_placeholder);
            deadlineText.setTextColor(ContextCompat.getColor(fragment.requireContext(), R.color.textSecondary));
        }

        if (!TextUtils.isEmpty(task.getAssigneeName())) {
            assigneeText.setText(task.getAssigneeName());
        } else {
            assigneeText.setText(R.string.task_assignee_unassigned);
        }

        Priority priority = task.getPriority() != null ? task.getPriority() : Priority.MEDIUM;
        priorityText.setText(priority.getDisplayName());
        int priorityColor;
        if (priority == Priority.HIGH) {
            priorityColor = ContextCompat.getColor(fragment.requireContext(), R.color.tomatoRed);
        } else if (priority == Priority.LOW) {
            priorityColor = ContextCompat.getColor(fragment.requireContext(), R.color.successGreen);
        } else {
            priorityColor = ContextCompat.getColor(fragment.requireContext(), R.color.mustardYellow);
        }
        priorityText.setTextColor(priorityColor);

        String description = task.getDescription();
        if (!TextUtils.isEmpty(description)) {
            descriptionText.setText(description);
            descriptionLabel.setVisibility(View.VISIBLE);
            descriptionText.setVisibility(View.VISIBLE);
            descriptionText.setAlpha(1f);
        } else {
            descriptionText.setText(R.string.group_task_no_description);
            descriptionLabel.setVisibility(View.VISIBLE);
            descriptionText.setVisibility(View.VISIBLE);
            descriptionText.setAlpha(0.7f);
        }

        toggleButton.setText(completed ? R.string.group_task_mark_incomplete : R.string.group_task_mark_complete);

        AlertDialog dialog = new MaterialAlertDialogBuilder(fragment.requireContext())
                .setView(dialogView)
                .create();

        closeButton.setOnClickListener(v -> dialog.dismiss());

        toggleButton.setOnClickListener(v -> {
            toggleButton.setEnabled(false);
            groupRepository.toggleGroupTaskCompletion(task,
                    aVoid -> fragment.requireActivity().runOnUiThread(() -> {
                        Toast.makeText(fragment.requireContext(), R.string.group_task_updated, Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    }),
                    e -> fragment.requireActivity().runOnUiThread(() -> {
                        toggleButton.setEnabled(true);
                        Toast.makeText(fragment.requireContext(), R.string.error_occurred, Toast.LENGTH_SHORT).show();
                    }));
        });

        dialog.show();
    }

    private Priority resolvePriority(@Nullable ChipGroup chipGroup) {
        if (chipGroup == null) {
            return Priority.MEDIUM;
        }
        int checkedId = chipGroup.getCheckedChipId();
        if (checkedId == R.id.chipHigh) {
            return Priority.HIGH;
        } else if (checkedId == R.id.chipLow) {
            return Priority.LOW;
        }
        return Priority.MEDIUM;
    }
}
