package com.example.overcooked.ui.adapter;

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

import com.example.overcooked.R;
import com.example.overcooked.data.model.Priority;
import com.example.overcooked.data.model.Task;
import com.example.overcooked.data.model.TaskStatus;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Adapter for Work Now tasks list in Home Fragment
 */
public class WorkNowTaskAdapter extends ListAdapter<Task, WorkNowTaskAdapter.TaskViewHolder> {

    public interface OnTaskClickListener {
        void onTaskClick(Task task);
    }

    public interface OnCompleteClickListener {
        void onCompleteClick(Task task);
    }

    public interface OnTaskMenuListener {
        void onEditTask(Task task);
        void onDeleteTask(Task task);
    }

    private final OnTaskClickListener onTaskClick;
    private final OnCompleteClickListener onCompleteClick;
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

    public WorkNowTaskAdapter(OnTaskClickListener onTaskClick, OnCompleteClickListener onCompleteClick, OnTaskMenuListener onTaskMenu) {
        super(DIFF_CALLBACK);
        this.onTaskClick = onTaskClick;
        this.onCompleteClick = onCompleteClick;
        this.onTaskMenu = onTaskMenu;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_work_now_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        holder.bind(getItem(position), onTaskClick, onCompleteClick, onTaskMenu);
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView cardView;
        private final TextView taskTitle;
        private final TextView taskDeadline;
        private final TextView taskSubject;
        private final View urgencyIndicator;
        private final CheckBox checkbox;
        private final TextView statusBadge;
        private final ImageButton btnMenu;

        private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault());

        TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (MaterialCardView) itemView;
            taskTitle = itemView.findViewById(R.id.taskTitle);
            taskDeadline = itemView.findViewById(R.id.taskDeadline);
            taskSubject = itemView.findViewById(R.id.taskSubject);
            urgencyIndicator = itemView.findViewById(R.id.urgencyIndicator);
            checkbox = itemView.findViewById(R.id.checkbox);
            statusBadge = itemView.findViewById(R.id.statusBadge);
            btnMenu = itemView.findViewById(R.id.btnMenu);
        }

        void bind(Task task, OnTaskClickListener onTaskClick, OnCompleteClickListener onCompleteClick, OnTaskMenuListener onTaskMenu) {
            taskTitle.setText(task.getTitle());
            taskDeadline.setText(getTimeRemaining(task));
            
            // Set priority indicator
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
            
            if (urgencyIndicator != null) {
                urgencyIndicator.setBackgroundColor(priorityColor);
            }

            // Overdue styling
            if (task.isOverdue()) {
                taskDeadline.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.tomatoRed));
            } else {
                taskDeadline.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.textSecondary));
            }

            cardView.setOnClickListener(v -> onTaskClick.onTaskClick(task));
            if (checkbox != null) {
                checkbox.setOnCheckedChangeListener(null);
                checkbox.setChecked(task.isCompleted());
                checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> onCompleteClick.onCompleteClick(task));
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

        private String getTimeRemaining(Task task) {
            if (task.getDeadline() == null) {
                return "No deadline";
            }
            long diff = task.getDeadline().getTime() - System.currentTimeMillis();
            
            if (diff < 0) {
                long days = TimeUnit.MILLISECONDS.toDays(Math.abs(diff));
                if (days > 0) {
                    return days + " day" + (days != 1 ? "s" : "") + " overdue";
                }
                long hours = TimeUnit.MILLISECONDS.toHours(Math.abs(diff));
                return hours + " hour" + (hours != 1 ? "s" : "") + " overdue";
            }

            long days = TimeUnit.MILLISECONDS.toDays(diff);
            if (days > 0) {
                return days + " day" + (days != 1 ? "s" : "") + " left";
            }
            
            long hours = TimeUnit.MILLISECONDS.toHours(diff);
            if (hours > 0) {
                return hours + " hour" + (hours != 1 ? "s" : "") + " left";
            }
            
            long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
            return minutes + " minute" + (minutes != 1 ? "s" : "") + " left";
        }
    }
}
