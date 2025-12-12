package com.student.overcooked.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.student.overcooked.R;
import com.student.overcooked.data.model.Task;
import com.student.overcooked.data.model.TaskStatus;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Objects;

/**
 * Adapter for Tasks in Project Details
 */
public class TaskAdapter extends ListAdapter<Task, TaskAdapter.TaskViewHolder> {

    public interface OnTaskClickListener {
        void onTaskClick(Task task);
    }

    public interface OnTaskMenuListener {
        void onEditTask(Task task);
        void onDeleteTask(Task task);
    }

    private final OnTaskClickListener onTaskClick;
    private final OnTaskMenuListener onTaskMenu;

    private static final DiffUtil.ItemCallback<Task> DIFF_CALLBACK = new DiffUtil.ItemCallback<Task>() {
        @Override
        public boolean areItemsTheSame(@NonNull Task oldItem, @NonNull Task newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull Task oldItem, @NonNull Task newItem) {
            return Objects.equals(oldItem.getTitle(), newItem.getTitle()) &&
                   oldItem.isCompleted() == newItem.isCompleted() &&
                   Objects.equals(oldItem.getStatus(), newItem.getStatus());
        }
    };

    public TaskAdapter(OnTaskClickListener onTaskClick, OnTaskMenuListener onTaskMenu) {
        super(DIFF_CALLBACK);
        this.onTaskClick = onTaskClick;
        this.onTaskMenu = onTaskMenu;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        holder.bind(getItem(position), onTaskClick, onTaskMenu);
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView cardView;
        private final TextView taskTitle;
        private final TextView taskDeadline;
        private final CheckBox taskCheckbox;
        private final View priorityIndicator;
        private final TextView statusBadge;
        private final ImageView taskMenu;

        private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd", Locale.getDefault());

        TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (MaterialCardView) itemView;
            taskTitle = itemView.findViewById(R.id.taskTitle);
            taskDeadline = itemView.findViewById(R.id.taskDeadline);
            taskCheckbox = itemView.findViewById(R.id.taskCheckbox);
            priorityIndicator = itemView.findViewById(R.id.priorityIndicator);
            statusBadge = itemView.findViewById(R.id.statusBadge);
            taskMenu = itemView.findViewById(R.id.taskMenu);
        }

        void bind(Task task, OnTaskClickListener onTaskClick, OnTaskMenuListener onTaskMenu) {
            taskTitle.setText(task.getTitle());
            taskDeadline.setText(task.getDeadline() != null ? dateFormat.format(task.getDeadline()) : "No deadline");

            if (task.isCompleted()) {
                taskTitle.setAlpha(0.5f);
            } else {
                taskTitle.setAlpha(1.0f);
            }

            if (taskCheckbox != null) {
                taskCheckbox.setOnCheckedChangeListener(null);
                taskCheckbox.setChecked(task.isCompleted());
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
            if (taskMenu != null) {
                taskMenu.setOnClickListener(v -> {
                    PopupMenu popup = new PopupMenu(itemView.getContext(), taskMenu);
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
    }
}
