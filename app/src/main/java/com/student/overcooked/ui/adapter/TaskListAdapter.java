package com.student.overcooked.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.student.overcooked.R;
import com.student.overcooked.data.model.Priority;
import com.student.overcooked.data.model.Task;
import com.student.overcooked.data.model.TaskStatus;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Objects;

/**
 * Adapter for Task List in Tasks Fragment
 */
public class TaskListAdapter extends ListAdapter<Task, TaskListAdapter.TaskViewHolder> {

    public interface OnTaskClickListener {
        void onTaskClick(Task task);
    }

    public interface OnCheckChangeListener {
        void onCheckChange(Task task);
    }

    public interface OnTaskMenuListener {
        void onEditTask(Task task);
        void onDeleteTask(Task task);
    }

    private final OnTaskClickListener onTaskClick;
    private final OnCheckChangeListener onCheckChange;
    private final OnTaskMenuListener onTaskMenu;

    private static final DiffUtil.ItemCallback<Task> DIFF_CALLBACK = new DiffUtil.ItemCallback<Task>() {
        @Override
        public boolean areItemsTheSame(@NonNull Task oldItem, @NonNull Task newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull Task oldItem, @NonNull Task newItem) {
            return Objects.equals(oldItem.getTitle(), newItem.getTitle()) &&
                   Objects.equals(oldItem.getDeadline(), newItem.getDeadline()) &&
                   oldItem.isCompleted() == newItem.isCompleted() &&
                   oldItem.getPriority() == newItem.getPriority() &&
                   oldItem.getStatus() == newItem.getStatus();
        }
    };

    public TaskListAdapter(OnTaskClickListener onTaskClick, OnCheckChangeListener onCheckChange, OnTaskMenuListener onTaskMenu) {
        super(DIFF_CALLBACK);
        this.onTaskClick = onTaskClick;
        this.onCheckChange = onCheckChange;
        this.onTaskMenu = onTaskMenu;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task_list, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        holder.bind(getItem(position), onTaskClick, onCheckChange, onTaskMenu);
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView cardView;
        private final CheckBox checkbox;
        private final TextView taskTitle;
        private final TextView taskSubject;
        private final TextView taskDeadline;
        private final View priorityIndicator;
        private final TextView statusBadge;
        private final ImageButton btnMenu;

        private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault());

        TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (MaterialCardView) itemView;
            checkbox = itemView.findViewById(R.id.checkbox);
            taskTitle = itemView.findViewById(R.id.taskTitle);
            taskSubject = itemView.findViewById(R.id.taskSubject);
            taskDeadline = itemView.findViewById(R.id.taskDeadline);
            priorityIndicator = itemView.findViewById(R.id.priorityIndicator);
            statusBadge = itemView.findViewById(R.id.statusBadge);
            btnMenu = itemView.findViewById(R.id.btnMenu);
        }

        void bind(Task task, OnTaskClickListener onTaskClick, OnCheckChangeListener onCheckChange, OnTaskMenuListener onTaskMenu) {
            taskTitle.setText(task.getTitle());
            if (taskSubject != null) {
                String taskTypeName = task.getTaskType() != null ? task.getTaskType().name() : "OTHER";
                taskSubject.setText(formatTaskType(taskTypeName));
            }
            taskDeadline.setText(task.getDeadline() != null ? dateFormat.format(task.getDeadline()) : "No deadline");

            // Set checkbox state without triggering listener
            checkbox.setOnCheckedChangeListener(null);
            checkbox.setChecked(task.isCompleted());
            checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> onCheckChange.onCheckChange(task));

            // Priority indicator
            int priorityColor;
            Priority priority = task.getPriority() != null ? task.getPriority() : Priority.MEDIUM;
            switch (priority) {
                case HIGH:
                    priorityColor = ContextCompat.getColor(itemView.getContext(), R.color.tomatoRed);
                    break;
                case MEDIUM:
                    priorityColor = ContextCompat.getColor(itemView.getContext(), R.color.mustardYellow);
                    break;
                case LOW:
                default:
                    priorityColor = ContextCompat.getColor(itemView.getContext(), R.color.successGreen);
                    break;
            }
            priorityIndicator.setBackgroundColor(priorityColor);

            // Completed styling
            if (task.isCompleted()) {
                taskTitle.setAlpha(0.5f);
            } else {
                taskTitle.setAlpha(1.0f);
            }

            // Overdue styling
            if (task.isOverdue() && !task.isCompleted()) {
                taskDeadline.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.tomatoRed));
            } else {
                taskDeadline.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.textSecondary));
            }

            // Status badge
            if (statusBadge != null) {
                TaskStatus status = task.getStatus() != null ? task.getStatus() : TaskStatus.NOT_STARTED;
                statusBadge.setText(status.getDisplayName());
                int statusColor;
                switch (status) {
                    case IN_PROGRESS:
                        statusColor = ContextCompat.getColor(itemView.getContext(), R.color.statusInProgress);
                        break;
                    case DONE:
                        statusColor = ContextCompat.getColor(itemView.getContext(), R.color.statusDone);
                        break;
                    case NOT_STARTED:
                    default:
                        statusColor = ContextCompat.getColor(itemView.getContext(), R.color.statusNotStarted);
                        break;
                }
                statusBadge.getBackground().setTint(statusColor);
            }

            cardView.setOnClickListener(v -> onTaskClick.onTaskClick(task));

            // Menu button for edit/delete
            if (btnMenu != null) {
                btnMenu.setOnClickListener(v -> {
                    PopupMenu popup = new PopupMenu(itemView.getContext(), btnMenu);
                    popup.inflate(R.menu.menu_task_options);
                    popup.setOnMenuItemClickListener(item -> {
                        int itemId = item.getItemId();
                        if (itemId == R.id.action_edit) {
                            onTaskMenu.onEditTask(task);
                            return true;
                        } else if (itemId == R.id.action_delete) {
                            onTaskMenu.onDeleteTask(task);
                            return true;
                        }
                        return false;
                    });
                    popup.show();
                });
            }
        }

        private String formatTaskType(String type) {
            // Convert HOMEWORK to Homework, etc.
            return type.charAt(0) + type.substring(1).toLowerCase().replace("_", " ");
        }
    }
}
