package com.student.overcooked.ui.groupdetail;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.student.overcooked.R;
import com.student.overcooked.data.model.GroupMember;
import com.student.overcooked.data.model.GroupTask;
import com.student.overcooked.data.model.Priority;
import com.student.overcooked.data.repository.GroupRepository;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

final class GroupTaskComposerDialog {

    private final Fragment fragment;
    private final GroupRepository groupRepository;
    private final String groupId;

    GroupTaskComposerDialog(@NonNull Fragment fragment,
                           @NonNull GroupRepository groupRepository,
                           @NonNull String groupId) {
        this.fragment = fragment;
        this.groupRepository = groupRepository;
        this.groupId = groupId;
    }

    void show(@Nullable GroupTask taskToEdit,
              boolean isIndividualProject,
              @NonNull List<GroupMember> members) {
        if (!fragment.isAdded()) {
            return;
        }

        boolean isEditing = taskToEdit != null;
        View dialogView = LayoutInflater.from(fragment.requireContext())
                .inflate(R.layout.dialog_add_edit_task, null);

        TextView heading = dialogView.findViewById(R.id.dialogTitle);
        ImageButton closeButton = dialogView.findViewById(R.id.btnClose);
        TextInputEditText titleInput = dialogView.findViewById(R.id.taskTitleInput);
        TextInputEditText descInput = dialogView.findViewById(R.id.taskDescriptionInput);
        MaterialCardView datePickerCard = dialogView.findViewById(R.id.datePickerCard);
        MaterialCardView timePickerCard = dialogView.findViewById(R.id.timePickerCard);
        TextView selectedDateText = dialogView.findViewById(R.id.selectedDateText);
        TextView selectedTimeText = dialogView.findViewById(R.id.selectedTimeText);
        ChipGroup priorityGroup = dialogView.findViewById(R.id.priorityChipGroup);
        MaterialButton saveButton = dialogView.findViewById(R.id.saveTaskButton);

        View taskTypeLabel = dialogView.findViewById(R.id.taskTypeLabel);
        View taskTypeGroup = dialogView.findViewById(R.id.taskTypeChipGroup);
        View taskCourseLayout = dialogView.findViewById(R.id.taskCourseLayout);

        TextView assigneeLabel = dialogView.findViewById(R.id.assigneeLabel);
        TextInputLayout assigneeLayout = dialogView.findViewById(R.id.assigneeInputLayout);
        MaterialAutoCompleteTextView assigneeDropdown = dialogView.findViewById(R.id.assigneeDropdown);

        if (heading != null) {
            heading.setText(fragment.getString(isEditing
                    ? R.string.dialog_edit_group_task_title
                    : R.string.dialog_add_group_task_title));
        }
        if (saveButton != null) {
            saveButton.setText(isEditing ? R.string.save_changes : R.string.add_task_button);
        }

        if (taskTypeLabel != null) {
            taskTypeLabel.setVisibility(View.GONE);
        }
        if (taskTypeGroup != null) {
            taskTypeGroup.setVisibility(View.GONE);
        }
        if (taskCourseLayout != null) {
            taskCourseLayout.setVisibility(View.GONE);
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
                if (selectedDateText != null) {
                    selectedDateText.setText(dateFormat.format(taskToEdit.getDeadline()));
                }
                if (selectedTimeText != null) {
                    selectedTimeText.setText(timeFormat.format(taskToEdit.getDeadline()));
                }
            }
            if (priorityGroup != null && taskToEdit.getPriority() != null) {
                if (taskToEdit.getPriority() == Priority.HIGH) {
                    priorityGroup.check(R.id.chipHighPriority);
                } else if (taskToEdit.getPriority() == Priority.LOW) {
                    priorityGroup.check(R.id.chipLowPriority);
                } else {
                    priorityGroup.check(R.id.chipMediumPriority);
                }
            }
        }

        if (datePickerCard != null) {
            datePickerCard.setOnClickListener(v -> {
                DatePickerDialog picker = new DatePickerDialog(fragment.requireContext(), (view, year, month, dayOfMonth) -> {
                    deadlineCalendar.set(Calendar.YEAR, year);
                    deadlineCalendar.set(Calendar.MONTH, month);
                    deadlineCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    hasCustomDate[0] = true;
                    if (selectedDateText != null) {
                        selectedDateText.setText(dateFormat.format(deadlineCalendar.getTime()));
                    }
                }, deadlineCalendar.get(Calendar.YEAR), deadlineCalendar.get(Calendar.MONTH), deadlineCalendar.get(Calendar.DAY_OF_MONTH));
                picker.getDatePicker().setMinDate(System.currentTimeMillis());
                picker.show();
            });
        }

        if (timePickerCard != null) {
            timePickerCard.setOnClickListener(v -> {
                TimePickerDialog picker = new TimePickerDialog(fragment.requireContext(), (view, hourOfDay, minute) -> {
                    deadlineCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    deadlineCalendar.set(Calendar.MINUTE, minute);
                    deadlineCalendar.set(Calendar.SECOND, 0);
                    deadlineCalendar.set(Calendar.MILLISECOND, 0);
                    hasCustomTime[0] = true;
                    if (selectedTimeText != null) {
                        selectedTimeText.setText(timeFormat.format(deadlineCalendar.getTime()));
                    }
                }, deadlineCalendar.get(Calendar.HOUR_OF_DAY), deadlineCalendar.get(Calendar.MINUTE), false);
                picker.show();
            });
        }

        final List<GroupMember> currentMembers = new ArrayList<>(members);
        final String[] selectedAssigneeId = {isEditing ? taskToEdit.getAssigneeId() : null};
        final String[] selectedAssigneeName = {isEditing ? taskToEdit.getAssigneeName() : null};
        GroupTaskAssigneeBinder.bind(fragment, isIndividualProject, currentMembers, taskToEdit,
            assigneeLabel, assigneeLayout, assigneeDropdown, selectedAssigneeId, selectedAssigneeName);

        AlertDialog dialog = new MaterialAlertDialogBuilder(fragment.requireContext())
                .setView(dialogView)
                .create();

        if (closeButton != null) {
            closeButton.setOnClickListener(v -> dialog.dismiss());
        }

        if (saveButton != null) {
            saveButton.setOnClickListener(v -> {
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

                saveButton.setEnabled(false);

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
                                saveButton.setEnabled(true);
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
                                saveButton.setEnabled(true);
                                Toast.makeText(fragment.requireContext(), R.string.error_occurred, Toast.LENGTH_SHORT).show();
                            })
                    );
                }
            });
        }

        dialog.show();
    }

    private static Priority resolvePriority(@Nullable ChipGroup chipGroup) {
        if (chipGroup == null) {
            return Priority.MEDIUM;
        }
        int checkedId = chipGroup.getCheckedChipId();
        if (checkedId == R.id.chipHighPriority) {
            return Priority.HIGH;
        } else if (checkedId == R.id.chipLowPriority) {
            return Priority.LOW;
        }
        return Priority.MEDIUM;
    }
}
