package com.student.overcooked.ui.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.student.overcooked.R;
import com.student.overcooked.data.model.Priority;
import com.student.overcooked.data.model.TaskStatus;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Dialog to show group task details and allow status changes.
 */
public class GroupTaskDetailsDialog extends DialogFragment {

    private static final String ARG_TASK_ID = "task_id";
    private static final String ARG_TASK_TITLE = "task_title";
    private static final String ARG_TASK_DESCRIPTION = "task_description";
    private static final String ARG_TASK_DEADLINE = "task_deadline";
    private static final String ARG_TASK_PRIORITY = "task_priority";
    private static final String ARG_TASK_ASSIGNEE_NAME = "task_assignee_name";
    private static final String ARG_TASK_STATUS = "task_status";

    private String taskId;
    private String title;
    private String description;
    @Nullable
    private Date deadline;
    private Priority priority = Priority.MEDIUM;
    private String assigneeName;
    private TaskStatus originalStatus = TaskStatus.NOT_STARTED;
    private TaskStatus selectedStatus = TaskStatus.NOT_STARTED;

    private OnGroupTaskActionListener listener;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault());

    public interface OnGroupTaskActionListener {
        void onGroupTaskStatusChanged(@NonNull String taskId, @NonNull TaskStatus newStatus);
    }

    public static GroupTaskDetailsDialog newInstance(@NonNull String taskId,
                                                     @NonNull String title,
                                                     @Nullable String description,
                                                     @Nullable Date deadline,
                                                     @Nullable Priority priority,
                                                     @Nullable String assigneeName,
                                                     @Nullable TaskStatus status) {
        GroupTaskDetailsDialog dialog = new GroupTaskDetailsDialog();
        Bundle args = new Bundle();
        args.putString(ARG_TASK_ID, taskId);
        args.putString(ARG_TASK_TITLE, title);
        args.putString(ARG_TASK_DESCRIPTION, description != null ? description : "");
        args.putLong(ARG_TASK_DEADLINE, deadline != null ? deadline.getTime() : 0L);
        args.putString(ARG_TASK_PRIORITY, priority != null ? priority.name() : Priority.MEDIUM.name());
        args.putString(ARG_TASK_ASSIGNEE_NAME, assigneeName != null ? assigneeName : "");
        args.putString(ARG_TASK_STATUS, status != null ? status.name() : TaskStatus.NOT_STARTED.name());
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (getParentFragment() instanceof OnGroupTaskActionListener) {
            listener = (OnGroupTaskActionListener) getParentFragment();
        } else if (context instanceof OnGroupTaskActionListener) {
            listener = (OnGroupTaskActionListener) context;
        }
    }

    public void setOnGroupTaskActionListener(@Nullable OnGroupTaskActionListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, com.google.android.material.R.style.Theme_MaterialComponents_Light_Dialog);

        Bundle args = getArguments();
        if (args != null) {
            taskId = args.getString(ARG_TASK_ID, "");
            title = args.getString(ARG_TASK_TITLE, "");
            description = args.getString(ARG_TASK_DESCRIPTION, "");

            long deadlineTime = args.getLong(ARG_TASK_DEADLINE, 0L);
            deadline = deadlineTime > 0L ? new Date(deadlineTime) : null;

            String priorityStr = args.getString(ARG_TASK_PRIORITY, Priority.MEDIUM.name());
            try {
                priority = Priority.valueOf(priorityStr);
            } catch (Exception ignored) {
                priority = Priority.MEDIUM;
            }

            assigneeName = args.getString(ARG_TASK_ASSIGNEE_NAME, "");

            String statusStr = args.getString(ARG_TASK_STATUS, TaskStatus.NOT_STARTED.name());
            try {
                originalStatus = TaskStatus.valueOf(statusStr);
            } catch (Exception ignored) {
                originalStatus = TaskStatus.NOT_STARTED;
            }
            selectedStatus = originalStatus;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_group_task_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ImageButton btnClose = view.findViewById(R.id.btnClose);
        TextView taskTitle = view.findViewById(R.id.taskTitle);
        TextView descriptionLabel = view.findViewById(R.id.descriptionLabel);
        TextView taskDescription = view.findViewById(R.id.taskDescription);
        TextView taskAssignee = view.findViewById(R.id.taskAssignee);
        TextView taskDeadline = view.findViewById(R.id.taskDeadline);
        TextView taskPriority = view.findViewById(R.id.taskPriority);

        ChipGroup statusChipGroup = view.findViewById(R.id.statusChipGroup);
        Chip chipNotStarted = view.findViewById(R.id.chipNotStarted);
        Chip chipInProgress = view.findViewById(R.id.chipInProgress);
        Chip chipDone = view.findViewById(R.id.chipDone);
        MaterialButton btnSave = view.findViewById(R.id.btnSave);

        taskTitle.setText(title);

        if (!TextUtils.isEmpty(description)) {
            taskDescription.setText(description);
            taskDescription.setVisibility(View.VISIBLE);
            descriptionLabel.setVisibility(View.VISIBLE);
        } else {
            taskDescription.setText(getString(R.string.task_description_hint));
            taskDescription.setAlpha(0.5f);
        }

        taskAssignee.setText(!TextUtils.isEmpty(assigneeName) ? assigneeName : getString(R.string.task_assignee_unassigned));

        if (deadline != null) {
            taskDeadline.setText(dateFormat.format(deadline));
        } else {
            taskDeadline.setText(getString(R.string.project_deadline_placeholder));
        }

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

        statusChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.contains(R.id.chipNotStarted)) {
                selectedStatus = TaskStatus.NOT_STARTED;
            } else if (checkedIds.contains(R.id.chipInProgress)) {
                selectedStatus = TaskStatus.IN_PROGRESS;
            } else if (checkedIds.contains(R.id.chipDone)) {
                selectedStatus = TaskStatus.DONE;
            }
        });

        btnClose.setOnClickListener(v -> dismiss());

        btnSave.setOnClickListener(v -> {
            if (listener != null && selectedStatus != originalStatus) {
                listener.onGroupTaskStatusChanged(taskId, selectedStatus);
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
}
