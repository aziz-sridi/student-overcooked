package com.example.overcooked.ui.adapter;

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
import com.example.overcooked.data.model.Project;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Objects;

/**
 * Adapter for Projects List
 */
public class ProjectAdapter extends ListAdapter<Project, ProjectAdapter.ProjectViewHolder> {

    public interface OnProjectClickListener {
        void onProjectClick(Project project);
    }

    private final OnProjectClickListener onProjectClick;

    private static final DiffUtil.ItemCallback<Project> DIFF_CALLBACK = new DiffUtil.ItemCallback<Project>() {
        @Override
        public boolean areItemsTheSame(@NonNull Project oldItem, @NonNull Project newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull Project oldItem, @NonNull Project newItem) {
            return Objects.equals(oldItem.getName(), newItem.getName()) &&
                   Objects.equals(oldItem.getDeadline(), newItem.getDeadline());
        }
    };

    public ProjectAdapter(OnProjectClickListener onProjectClick) {
        super(DIFF_CALLBACK);
        this.onProjectClick = onProjectClick;
    }

    @NonNull
    @Override
    public ProjectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_project_card, parent, false);
        return new ProjectViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProjectViewHolder holder, int position) {
        holder.bind(getItem(position), onProjectClick);
    }

    static class ProjectViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView cardView;
        private final TextView projectTitle;
        private final TextView projectTypeLabel;
        private final TextView projectTasksCount;
        private final LinearProgressIndicator progressBar;

        private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

        ProjectViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (MaterialCardView) itemView;
            projectTitle = itemView.findViewById(R.id.projectTitle);
            projectTypeLabel = itemView.findViewById(R.id.projectTypeLabel);
            projectTasksCount = itemView.findViewById(R.id.projectTasksCount);
            progressBar = itemView.findViewById(R.id.projectProgress);
        }

        void bind(Project project, OnProjectClickListener onProjectClick) {
            projectTitle.setText(project.getName());
            projectTypeLabel.setText(project.isTeamProject() ? "Team" : "Solo");

            // Task count placeholder
            projectTasksCount.setText("0 tasks");

            // Calculate progress (placeholder - would need tasks count)
            int progress = 0;
            if (progressBar != null) {
                progressBar.setProgressCompat(progress, true);
            }

            cardView.setOnClickListener(v -> onProjectClick.onProjectClick(project));
        }
    }
}
