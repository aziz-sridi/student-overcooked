package com.example.overcooked.ui.dialog;

import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.overcooked.R;
import com.example.overcooked.data.model.Priority;
import com.example.overcooked.data.model.Task;
import com.example.overcooked.data.model.TaskStatus;
import com.example.overcooked.data.model.TaskType;
import com.google.android.material.chip.ChipGroup;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Encapsulates the Add/Edit Task form state, argument parsing, and validation logic.
 */
public class AddEditTaskFormState {

    private final Calendar selectedDeadline = Calendar.getInstance();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());

    private int mode = AddEditTaskDialog.MODE_ADD;
    @Nullable
    private Task existingTask;

    public static AddEditTaskFormState fromArguments(@Nullable Bundle args) {
        AddEditTaskFormState state = new AddEditTaskFormState();
        if (args == null) {
            state.applyDefaultDeadline();
            return state;
        }
        state.mode = args.getInt(AddEditTaskDialog.ARG_MODE, AddEditTaskDialog.MODE_ADD);
        if (state.mode == AddEditTaskDialog.MODE_EDIT) {
            Task task = new Task();
            task.setId(args.getLong(AddEditTaskDialog.ARG_TASK_ID));
            task.setTitle(args.getString(AddEditTaskDialog.ARG_TASK_TITLE, ""));
            task.setDescription(args.getString(AddEditTaskDialog.ARG_TASK_DESCRIPTION, ""));
            task.setCourse(args.getString(AddEditTaskDialog.ARG_TASK_COURSE, ""));
            task.setProjectId(args.getLong(AddEditTaskDialog.ARG_TASK_PROJECT_ID, 0));

            String typeValue = args.getString(AddEditTaskDialog.ARG_TASK_TYPE, TaskType.HOMEWORK.name());
            try {
                task.setTaskType(TaskType.valueOf(typeValue));
            } catch (IllegalArgumentException ignored) {
                task.setTaskType(TaskType.HOMEWORK);
            }

            long deadlineTime = args.getLong(AddEditTaskDialog.ARG_TASK_DEADLINE, 0);
            if (deadlineTime > 0) {
                Date deadline = new Date(deadlineTime);
                task.setDeadline(deadline);
                state.selectedDeadline.setTime(deadline);
            }

            String priorityValue = args.getString(AddEditTaskDialog.ARG_TASK_PRIORITY, Priority.MEDIUM.name());
            try {
                task.setPriority(Priority.valueOf(priorityValue));
            } catch (IllegalArgumentException ignored) {
                task.setPriority(Priority.MEDIUM);
            }

            String statusValue = args.getString(AddEditTaskDialog.ARG_TASK_STATUS, TaskStatus.NOT_STARTED.name());
            try {
                task.setStatus(TaskStatus.valueOf(statusValue));
            } catch (IllegalArgumentException ignored) {
                task.setStatus(TaskStatus.NOT_STARTED);
            }

            task.setCompleted(args.getBoolean(AddEditTaskDialog.ARG_TASK_COMPLETED, false));
            state.existingTask = task;
        } else {
            state.applyDefaultDeadline();
        }
        return state;
    }

    private void applyDefaultDeadline() {
        selectedDeadline.add(Calendar.DAY_OF_YEAR, 1);
        selectedDeadline.set(Calendar.HOUR_OF_DAY, 23);
        selectedDeadline.set(Calendar.MINUTE, 59);
        selectedDeadline.set(Calendar.SECOND, 0);
        selectedDeadline.set(Calendar.MILLISECOND, 0);
    }

    public void bindInitialState(@NonNull AddEditTaskViewBinder binder) {
        binder.applyModeText(mode);
        if (isEditMode() && existingTask != null) {
            binder.taskTitleInput.setText(existingTask.getTitle());
            binder.taskDescriptionInput.setText(existingTask.getDescription());
            binder.taskCourseInput.setText(existingTask.getCourse(), false);
            checkTaskTypeChip(binder.taskTypeChipGroup, existingTask.getTaskType());
            checkPriorityChip(binder.priorityChipGroup, existingTask.getPriority());
        } else {
            // Default chip selections for new tasks
            binder.taskTypeChipGroup.check(R.id.chipHomework);
            binder.priorityChipGroup.check(R.id.chipMediumPriority);
        }
        updateDateDisplay(binder);
        updateTimeDisplay(binder);
    }

    public void updateDateDisplay(@NonNull AddEditTaskViewBinder binder) {
        binder.selectedDateText.setText(dateFormat.format(selectedDeadline.getTime()));
    }

    public void updateTimeDisplay(@NonNull AddEditTaskViewBinder binder) {
        binder.selectedTimeText.setText(timeFormat.format(selectedDeadline.getTime()));
    }

    @NonNull
    public Calendar getSelectedDeadline() {
        return selectedDeadline;
    }

    public SaveResult buildTask(@NonNull AddEditTaskViewBinder binder) {
        String title = binder.taskTitleInput.getText() != null
                ? binder.taskTitleInput.getText().toString().trim()
                : "";
        if (TextUtils.isEmpty(title)) {
            return SaveResult.error(binder.taskTitleInput.getContext().getString(R.string.task_title_required));
        }

        Task task;
        boolean isNew;
        if (isEditMode() && existingTask != null) {
            task = existingTask;
            isNew = false;
        } else {
            task = new Task();
            task.setCreatedAt(new Date());
            task.setStatus(TaskStatus.NOT_STARTED);
            isNew = true;
        }

        String description = binder.taskDescriptionInput.getText() != null
                ? binder.taskDescriptionInput.getText().toString().trim()
                : "";
        String course = binder.taskCourseInput.getText() != null
                ? binder.taskCourseInput.getText().toString().trim()
                : "";

        task.setTitle(title);
        task.setDescription(description);
        task.setCourse(course);
        task.setTaskType(resolveTaskType(binder.taskTypeChipGroup));
        task.setPriority(resolvePriority(binder.priorityChipGroup));
        task.setDeadline(selectedDeadline.getTime());

        return SaveResult.success(task, isNew);
    }

    private void checkTaskTypeChip(@Nullable ChipGroup group, @Nullable TaskType taskType) {
        if (group == null) {
            return;
        }
        TaskType type = taskType != null ? taskType : TaskType.HOMEWORK;
        int chipId;
        switch (type) {
            case ASSIGNMENT:
                chipId = R.id.chipAssignment;
                break;
            case EXAM:
                chipId = R.id.chipExam;
                break;
            case OTHER:
                chipId = R.id.chipOther;
                break;
            case HOMEWORK:
            default:
                chipId = R.id.chipHomework;
                break;
        }
        group.check(chipId);
    }

    private void checkPriorityChip(@Nullable ChipGroup group, @Nullable Priority priority) {
        if (group == null) {
            return;
        }
        Priority value = priority != null ? priority : Priority.MEDIUM;
        int chipId;
        switch (value) {
            case LOW:
                chipId = R.id.chipLowPriority;
                break;
            case HIGH:
                chipId = R.id.chipHighPriority;
                break;
            case MEDIUM:
            default:
                chipId = R.id.chipMediumPriority;
                break;
        }
        group.check(chipId);
    }

    private TaskType resolveTaskType(@Nullable ChipGroup chipGroup) {
        if (chipGroup == null) {
            return TaskType.HOMEWORK;
        }
        int checkedId = chipGroup.getCheckedChipId();
        if (checkedId == R.id.chipAssignment) return TaskType.ASSIGNMENT;
        if (checkedId == R.id.chipExam) return TaskType.EXAM;
        if (checkedId == R.id.chipOther) return TaskType.OTHER;
        return TaskType.HOMEWORK;
    }

    private Priority resolvePriority(@Nullable ChipGroup chipGroup) {
        if (chipGroup == null) {
            return Priority.MEDIUM;
        }
        int checkedId = chipGroup.getCheckedChipId();
        if (checkedId == R.id.chipLowPriority) return Priority.LOW;
        if (checkedId == R.id.chipHighPriority) return Priority.HIGH;
        return Priority.MEDIUM;
    }

    public boolean isEditMode() {
        return mode == AddEditTaskDialog.MODE_EDIT && existingTask != null;
    }

    public int getMode() {
        return mode;
    }

    public static final class SaveResult {
        @Nullable
        public final Task task;
        public final boolean isNew;
        @Nullable
        public final String errorMessage;

        private SaveResult(@Nullable Task task, boolean isNew, @Nullable String errorMessage) {
            this.task = task;
            this.isNew = isNew;
            this.errorMessage = errorMessage;
        }

        public static SaveResult success(@NonNull Task task, boolean isNew) {
            return new SaveResult(task, isNew, null);
        }

        public static SaveResult error(@NonNull String message) {
            return new SaveResult(null, false, message);
        }

        public boolean isSuccess() {
            return task != null;
        }
    }
}
