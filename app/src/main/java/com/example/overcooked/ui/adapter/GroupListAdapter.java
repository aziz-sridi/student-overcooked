package com.example.overcooked.ui.adapter;

import android.content.res.Resources;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.overcooked.R;
import com.example.overcooked.data.model.Group;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.Objects;

/**
 * Adapter for Groups List
 */
public class GroupListAdapter extends ListAdapter<Group, GroupListAdapter.GroupViewHolder> {

    public interface OnGroupClickListener {
        void onGroupClick(Group group);
    }

    private final OnGroupClickListener onGroupClick;

    private static final DiffUtil.ItemCallback<Group> DIFF_CALLBACK = new DiffUtil.ItemCallback<Group>() {
        @Override
        public boolean areItemsTheSame(@NonNull Group oldItem, @NonNull Group newItem) {
            return Objects.equals(oldItem.getId(), newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Group oldItem, @NonNull Group newItem) {
            return Objects.equals(oldItem.getName(), newItem.getName()) &&
                   Objects.equals(oldItem.getSubject(), newItem.getSubject()) &&
                   Objects.equals(oldItem.getDescription(), newItem.getDescription()) &&
                   oldItem.getMemberCount() == newItem.getMemberCount() &&
                   oldItem.getTotalTasks() == newItem.getTotalTasks() &&
                   oldItem.getCompletedTasks() == newItem.getCompletedTasks();
        }
    };

    public GroupListAdapter(OnGroupClickListener onGroupClick) {
        super(DIFF_CALLBACK);
        this.onGroupClick = onGroupClick;
    }

    @NonNull
    @Override
    public GroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_group_card, parent, false);
        return new GroupViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GroupViewHolder holder, int position) {
        holder.bind(getItem(position), onGroupClick);
    }

    static class GroupViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView cardView;
        private final TextView groupName;
        private final TextView groupSubject;
        private final TextView memberCount;
        private final TextView progressText;
        private final TextView progressPercent;
        private final LinearProgressIndicator progressBar;

        GroupViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (MaterialCardView) itemView;
            groupName = itemView.findViewById(R.id.groupName);
            groupSubject = itemView.findViewById(R.id.groupSubject);
            memberCount = itemView.findViewById(R.id.memberCount);
            progressText = itemView.findViewById(R.id.progressText);
            progressPercent = itemView.findViewById(R.id.progressPercent);
            progressBar = itemView.findViewById(R.id.progressBar);
        }

        void bind(Group group, OnGroupClickListener onGroupClick) {
            Resources res = itemView.getResources();
            groupName.setText(group.getName());

            String subject = group.getSubject();
            if (TextUtils.isEmpty(subject)) {
                groupSubject.setText(res.getString(R.string.group_subject_placeholder));
            } else {
                groupSubject.setText(subject);
            }

            // Member count text with plural support
            int count = Math.max(1, group.getMemberCount());
            memberCount.setText(res.getQuantityString(R.plurals.group_member_count, count, count));

            // Progress display
            int totalTasks = group.getTotalTasks();
            if (totalTasks <= 0) {
                progressText.setText(res.getString(R.string.group_no_tasks_short));
                progressPercent.setText(res.getString(R.string.group_progress_empty_hint));
                progressBar.setProgressCompat(0, false);
                progressBar.setIndicatorColor(ContextCompat.getColor(itemView.getContext(), R.color.primaryBlue));
            } else {
                int percentage = group.getCompletionPercentage();
                progressText.setText(group.getProgressText());
                progressPercent.setText(res.getString(R.string.group_progress_percent_template, percentage));
                progressBar.setProgressCompat(percentage, true);
                progressBar.setIndicatorColor(ContextCompat.getColor(
                        itemView.getContext(),
                        getProgressColor(percentage))
                );
            }

            cardView.setOnClickListener(v -> onGroupClick.onGroupClick(group));
        }

        private int getProgressColor(int percentage) {
            if (percentage >= 75) {
                return R.color.successGreen;
            } else if (percentage >= 40) {
                return R.color.mustardYellow;
            } else {
                return R.color.burntOrange;
            }
        }
    }
}
