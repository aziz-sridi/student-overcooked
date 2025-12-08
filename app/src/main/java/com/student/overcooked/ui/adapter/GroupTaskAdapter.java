package com.student.overcooked.ui.adapter;

import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.student.overcooked.R;
import com.student.overcooked.data.model.GroupTask;
import com.student.overcooked.data.model.Priority;
import com.google.android.material.chip.Chip;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

/**
 * Adapter for Group Tasks List
 */
public class GroupTaskAdapter extends ListAdapter<GroupTask, GroupTaskAdapter.TaskViewHolder> {

    public interface TaskInteractionListener {
        void onTaskSelected(GroupTask task);
        void onTaskCompletionToggle(GroupTask task);
        void onTaskMenuRequested(@NonNull View anchor, GroupTask task);
        void onTaskLongPressed(@NonNull View anchor, GroupTask task);
    }

    private final TaskInteractionListener interactionListener;

    private static final DiffUtil.ItemCallback<GroupTask> DIFF_CALLBACK = new DiffUtil.ItemCallback<GroupTask>() {
        @Override
        public boolean areItemsTheSame(@NonNull GroupTask oldItem, @NonNull GroupTask newItem) {
            return Objects.equals(oldItem.getId(), newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull GroupTask oldItem, @NonNull GroupTask newItem) {
            return Objects.equals(oldItem.getTitle(), newItem.getTitle()) &&
                   oldItem.isCompleted() == newItem.isCompleted() &&
                   Objects.equals(oldItem.getDeadline(), newItem.getDeadline()) &&
                   Objects.equals(oldItem.getDescription(), newItem.getDescription()) &&
                   Objects.equals(oldItem.getAssigneeName(), newItem.getAssigneeName()) &&
                   oldItem.getPriority() == newItem.getPriority();
        }
    };

    public GroupTaskAdapter(TaskInteractionListener interactionListener) {
        super(DIFF_CALLBACK);
        this.interactionListener = interactionListener;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_group_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        holder.bind(getItem(position), interactionListener);
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView cardView;
        private final CheckBox checkbox;
        private final TextView taskTitle;
        private final TextView taskDeadline;
        private final Chip assigneeChip;
        private final TextView statusBadge;
        private final TextView descriptionText;
        private final View priorityIndicator;
        private final ImageView menuButton;

        private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

        TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (MaterialCardView) itemView;
            checkbox = itemView.findViewById(R.id.taskCheckbox);
            taskTitle = itemView.findViewById(R.id.taskTitle);
            taskDeadline = itemView.findViewById(R.id.taskDeadline);
            assigneeChip = itemView.findViewById(R.id.assigneeChip);
            statusBadge = itemView.findViewById(R.id.statusBadge);
            descriptionText = itemView.findViewById(R.id.taskDescription);
            priorityIndicator = itemView.findViewById(R.id.priorityIndicator);
            menuButton = itemView.findViewById(R.id.taskMenu);
        }

        void bind(GroupTask task, TaskInteractionListener interactionListener) {
            taskTitle.setText(task.getTitle());

            if (task.getDeadline() != null) {
                taskDeadline.setText(dateFormat.format(task.getDeadline()));
            } else {
                taskDeadline.setText(itemView.getContext().getString(R.string.project_deadline_placeholder));
            }

            Date now = new Date();
            boolean overdue = !task.isCompleted() && task.getDeadline() != null && task.getDeadline().before(now);
            taskDeadline.setTextColor(ContextCompat.getColor(itemView.getContext(), overdue ? R.color.tomatoRed : R.color.textSecondary));

            if (assigneeChip != null) {
                if (task.getAssignedToName() != null && !task.getAssignedToName().isEmpty()) {
                    assigneeChip.setText(itemView.getContext().getString(R.string.task_assigned_to_template, task.getAssignedToName()));
                } else {
                    assigneeChip.setText(itemView.getContext().getString(R.string.task_assignee_unassigned));
                }
            }

            if (descriptionText != null) {
                if (task.getDescription() != null && !task.getDescription().isEmpty()) {
                    descriptionText.setVisibility(View.VISIBLE);
                    descriptionText.setText(task.getDescription());
                } else {
                    descriptionText.setVisibility(View.GONE);
                }
            }

            Priority priority = task.getPriority() != null ? task.getPriority() : Priority.MEDIUM;
            int priorityColor;
            if (priority == Priority.HIGH) {
                priorityColor = ContextCompat.getColor(itemView.getContext(), R.color.tomatoRed);
            } else if (priority == Priority.MEDIUM) {
                priorityColor = ContextCompat.getColor(itemView.getContext(), R.color.mustardYellow);
            } else {
                priorityColor = ContextCompat.getColor(itemView.getContext(), R.color.successGreen);
            }
            if (priorityIndicator != null) {
                priorityIndicator.setBackgroundColor(priorityColor);
            }

            if (statusBadge != null) {
                int badgeColor;
                if (task.isCompleted()) {
                    statusBadge.setText(R.string.status_done);
                    badgeColor = ContextCompat.getColor(itemView.getContext(), R.color.successGreen);
                } else if (overdue) {
                    statusBadge.setText(R.string.status_overdue);
                    badgeColor = ContextCompat.getColor(itemView.getContext(), R.color.tomatoRed);
                } else {
                    statusBadge.setText(R.string.status_in_progress);
                    badgeColor = ContextCompat.getColor(itemView.getContext(), R.color.mustardYellow);
                }

                Drawable badgeBackground = statusBadge.getBackground();
                if (badgeBackground != null) {
                    Drawable wrapped = DrawableCompat.wrap(badgeBackground.mutate());
                    DrawableCompat.setTint(wrapped, badgeColor);
                    statusBadge.setBackground(wrapped);
                }
            }

            checkbox.setOnCheckedChangeListener(null);
            checkbox.setChecked(task.isCompleted());
            checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> interactionListener.onTaskCompletionToggle(task));

            taskTitle.setAlpha(task.isCompleted() ? 0.5f : 1f);
            if (descriptionText != null) {
                descriptionText.setAlpha(task.isCompleted() ? 0.5f : 1f);
            }

            cardView.setOnClickListener(v -> interactionListener.onTaskSelected(task));
            cardView.setOnLongClickListener(v -> {
                interactionListener.onTaskLongPressed(cardView, task);
                return true;
            });

            if (menuButton != null) {
                menuButton.setOnClickListener(v -> interactionListener.onTaskMenuRequested(menuButton, task));
            }
        }
    }
}
