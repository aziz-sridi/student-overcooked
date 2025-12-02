package com.example.overcooked.ui.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import com.example.overcooked.R;
import com.example.overcooked.data.model.Priority;
import com.example.overcooked.data.model.Task;
import com.example.overcooked.data.model.TaskStatus;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Dialog to show task details and allow status changes
 */
public class TaskDetailsDialog extends DialogFragment {

    private static final String ARG_TASK_ID = "task_id";
    private static final String ARG_TASK_TITLE = "task_title";
    private static final String ARG_TASK_DESCRIPTION = "task_description";
    private static final String ARG_TASK_COURSE = "task_course";
    private static final String ARG_TASK_TYPE = "task_type";
    private static final String ARG_TASK_DEADLINE = "task_deadline";
    private static final String ARG_TASK_PRIORITY = "task_priority";
    private static final String ARG_TASK_NOTES = "task_notes";
    private static final String ARG_TASK_STATUS = "task_status";
    private static final String ARG_TASK_COMPLETED = "task_completed";

    private Task task;
    private TaskStatus selectedStatus;
    private OnTaskActionListener listener;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault());

    public interface OnTaskActionListener {
        void onTaskStatusChanged(Task task, TaskStatus newStatus);
    }

    public static TaskDetailsDialog newInstance(Task task) {
        TaskDetailsDialog dialog = new TaskDetailsDialog();
        Bundle args = new Bundle();
        args.putLong(ARG_TASK_ID, task.getId());
        args.putString(ARG_TASK_TITLE, task.getTitle());
        args.putString(ARG_TASK_DESCRIPTION, task.getDescription());
        args.putString(ARG_TASK_COURSE, task.getCourse());
        args.putString(ARG_TASK_TYPE, task.getTaskType() != null ? task.getTaskType().name() : "OTHER");
        args.putLong(ARG_TASK_DEADLINE, task.getDeadline() != null ? task.getDeadline().getTime() : 0);
        args.putString(ARG_TASK_PRIORITY, task.getPriority() != null ? task.getPriority().name() : "MEDIUM");
        args.putString(ARG_TASK_NOTES, task.getNotes());
        args.putString(ARG_TASK_STATUS, task.getStatus() != null ? task.getStatus().name() : "NOT_STARTED");
        args.putBoolean(ARG_TASK_COMPLETED, task.isCompleted());
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        // Try to get the listener from the parent fragment first
        if (getParentFragment() instanceof OnTaskActionListener) {
            listener = (OnTaskActionListener) getParentFragment();
        } else if (context instanceof OnTaskActionListener) {
            listener = (OnTaskActionListener) context;
        }
    }

    public void setOnTaskActionListener(OnTaskActionListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, com.google.android.material.R.style.Theme_MaterialComponents_Light_Dialog);
        
        if (getArguments() != null) {
            task = new Task();
            task.setId(getArguments().getLong(ARG_TASK_ID));
            task.setTitle(getArguments().getString(ARG_TASK_TITLE, ""));
            task.setDescription(getArguments().getString(ARG_TASK_DESCRIPTION, ""));
            task.setCourse(getArguments().getString(ARG_TASK_COURSE, ""));
            
            String taskType = getArguments().getString(ARG_TASK_TYPE, "OTHER");
            try {
                task.setTaskType(com.example.overcooked.data.model.TaskType.valueOf(taskType));
            } catch (Exception e) {
                task.setTaskType(com.example.overcooked.data.model.TaskType.OTHER);
            }
            
            long deadlineTime = getArguments().getLong(ARG_TASK_DEADLINE, 0);
            task.setDeadline(deadlineTime > 0 ? new java.util.Date(deadlineTime) : null);
            
            String priority = getArguments().getString(ARG_TASK_PRIORITY, "MEDIUM");
            try {
                task.setPriority(Priority.valueOf(priority));
            } catch (Exception e) {
                task.setPriority(Priority.MEDIUM);
            }
            
            task.setNotes(getArguments().getString(ARG_TASK_NOTES, ""));
            
            String status = getArguments().getString(ARG_TASK_STATUS, "NOT_STARTED");
            try {
                selectedStatus = TaskStatus.valueOf(status);
                task.setStatus(selectedStatus);
            } catch (Exception e) {
                selectedStatus = TaskStatus.NOT_STARTED;
                task.setStatus(selectedStatus);
            }
            
            task.setCompleted(getArguments().getBoolean(ARG_TASK_COMPLETED, false));
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_task_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize views
        ImageButton btnClose = view.findViewById(R.id.btnClose);
        TextView taskTitle = view.findViewById(R.id.taskTitle);
        TextView taskTypeBadge = view.findViewById(R.id.taskTypeBadge);
        TextView taskDescription = view.findViewById(R.id.taskDescription);
        TextView descriptionLabel = view.findViewById(R.id.descriptionLabel);
        TextView taskCourse = view.findViewById(R.id.taskCourse);
        TextView taskDeadline = view.findViewById(R.id.taskDeadline);
        TextView taskPriority = view.findViewById(R.id.taskPriority);
        TextView notesLabel = view.findViewById(R.id.notesLabel);
        TextView taskNotes = view.findViewById(R.id.taskNotes);
        ChipGroup statusChipGroup = view.findViewById(R.id.statusChipGroup);
        Chip chipNotStarted = view.findViewById(R.id.chipNotStarted);
        Chip chipInProgress = view.findViewById(R.id.chipInProgress);
        Chip chipDone = view.findViewById(R.id.chipDone);
        MaterialButton btnSave = view.findViewById(R.id.btnSave);

        // Populate data
        taskTitle.setText(task.getTitle());
        
        String taskTypeDisplay = formatTaskType(task.getTaskType() != null ? task.getTaskType().name() : "OTHER");
        taskTypeBadge.setText(taskTypeDisplay);

        // Description
        String description = task.getDescription();
        if (description != null && !description.isEmpty()) {
            taskDescription.setText(description);
            taskDescription.setVisibility(View.VISIBLE);
            descriptionLabel.setVisibility(View.VISIBLE);
        } else {
            taskDescription.setText(getString(R.string.task_description_hint));
            taskDescription.setAlpha(0.5f);
        }

        // Course
        String course = task.getCourse();
        taskCourse.setText(course != null && !course.isEmpty() ? course : "No course");

        // Deadline
        if (task.getDeadline() != null) {
            taskDeadline.setText(dateFormat.format(task.getDeadline()));
            if (task.isOverdue() && !task.isCompleted()) {
                taskDeadline.setTextColor(ContextCompat.getColor(requireContext(), R.color.tomatoRed));
            }
        } else {
            taskDeadline.setText("No deadline");
        }

        // Priority
        Priority priority = task.getPriority() != null ? task.getPriority() : Priority.MEDIUM;
        taskPriority.setText(priority.getDisplayName());
        int priorityColor;
        switch (priority) {
            case HIGH:
                priorityColor = ContextCompat.getColor(requireContext(), R.color.tomatoRed);
                break;
            case LOW:
                priorityColor = ContextCompat.getColor(requireContext(), R.color.successGreen);
                break;
            case MEDIUM:
            default:
                priorityColor = ContextCompat.getColor(requireContext(), R.color.mustardYellow);
                break;
        }
        taskPriority.setTextColor(priorityColor);

        // Notes
        String notes = task.getNotes();
        if (notes != null && !notes.isEmpty()) {
            taskNotes.setText(notes);
            taskNotes.setVisibility(View.VISIBLE);
            notesLabel.setVisibility(View.VISIBLE);
        }

        // Set current status
        switch (selectedStatus) {
            case NOT_STARTED:
                chipNotStarted.setChecked(true);
                break;
            case IN_PROGRESS:
                chipInProgress.setChecked(true);
                break;
            case DONE:
                chipDone.setChecked(true);
                break;
        }

        // Status chip listeners
        statusChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.contains(R.id.chipNotStarted)) {
                selectedStatus = TaskStatus.NOT_STARTED;
            } else if (checkedIds.contains(R.id.chipInProgress)) {
                selectedStatus = TaskStatus.IN_PROGRESS;
            } else if (checkedIds.contains(R.id.chipDone)) {
                selectedStatus = TaskStatus.DONE;
            }
        });

        // Button listeners
        btnClose.setOnClickListener(v -> dismiss());

        btnSave.setOnClickListener(v -> {
            if (listener != null && selectedStatus != task.getStatus()) {
                listener.onTaskStatusChanged(task, selectedStatus);
            }
            dismiss();
        });
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.bg_dialog);
        }
    }

    private String formatTaskType(String type) {
        if (type == null) return "Other";
        return type.charAt(0) + type.substring(1).toLowerCase().replace("_", " ");
    }
}
