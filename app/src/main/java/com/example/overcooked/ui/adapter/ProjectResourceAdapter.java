package com.example.overcooked.ui.adapter;

import android.text.TextUtils;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.overcooked.R;
import com.example.overcooked.data.model.ProjectResource;
import com.example.overcooked.data.model.ProjectResourceType;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Objects;

/**
 * Adapter to render workspace resources for individual projects
 */
public class ProjectResourceAdapter extends ListAdapter<ProjectResource, ProjectResourceAdapter.ResourceViewHolder> {

    public interface ResourceActionListener {
        void onResourceClick(ProjectResource resource);
        void onResourceDelete(ProjectResource resource);
    }

    private final ResourceActionListener listener;

    private static final DiffUtil.ItemCallback<ProjectResource> DIFF_CALLBACK = new DiffUtil.ItemCallback<ProjectResource>() {
        @Override
        public boolean areItemsTheSame(@NonNull ProjectResource oldItem, @NonNull ProjectResource newItem) {
            return Objects.equals(oldItem.getId(), newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull ProjectResource oldItem, @NonNull ProjectResource newItem) {
                return Objects.equals(oldItem.getTitle(), newItem.getTitle()) &&
                    Objects.equals(oldItem.getContent(), newItem.getContent()) &&
                    oldItem.getType() == newItem.getType() &&
                    Objects.equals(oldItem.getFileName(), newItem.getFileName()) &&
                    Objects.equals(oldItem.getFileUrl(), newItem.getFileUrl()) &&
                    oldItem.getFileSizeBytes() == newItem.getFileSizeBytes();
        }
    };

    public ProjectResourceAdapter(ResourceActionListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ResourceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_project_resource, parent, false);
        return new ResourceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ResourceViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static class ResourceViewHolder extends RecyclerView.ViewHolder {
        private final ImageView iconView;
        private final TextView titleText;
        private final TextView subtitleText;
        private final TextView timestampText;
        private final ImageButton menuButton;
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

        ResourceViewHolder(@NonNull View itemView) {
            super(itemView);
            iconView = itemView.findViewById(R.id.resourceIcon);
            titleText = itemView.findViewById(R.id.resourceTitle);
            subtitleText = itemView.findViewById(R.id.resourceSubtitle);
            timestampText = itemView.findViewById(R.id.resourceTimestamp);
            menuButton = itemView.findViewById(R.id.resourceMenuButton);
        }

        void bind(ProjectResource resource, ResourceActionListener listener) {
            titleText.setText(resource.getTitle().isEmpty() ? itemView.getContext().getString(R.string.untitled_resource) : resource.getTitle());

            ProjectResourceType type = resource.getType() != null ? resource.getType() : ProjectResourceType.NOTE;
            String subtitle = resource.getContent() != null ? resource.getContent() : "";
            if (type == ProjectResourceType.FILE) {
                String displayName = !TextUtils.isEmpty(resource.getFileName())
                        ? resource.getFileName()
                        : itemView.getContext().getString(R.string.untitled_resource);
                if (resource.getFileSizeBytes() > 0) {
                    String sizeLabel = Formatter.formatShortFileSize(itemView.getContext(), resource.getFileSizeBytes());
                    subtitle = itemView.getContext().getString(R.string.workspace_file_selected_template, displayName, sizeLabel);
                } else {
                    subtitle = displayName;
                }
                if (!TextUtils.isEmpty(resource.getContent())) {
                    subtitle = subtitle + " | " + resource.getContent();
                }
            }
            subtitleText.setText(subtitle);

            if (resource.getCreatedAt() != null) {
                timestampText.setText(dateFormat.format(resource.getCreatedAt()));
            } else {
                timestampText.setText("");
            }

            int iconRes;
            switch (type) {
                case FILE:
                    iconRes = R.drawable.ic_resource_file;
                    break;
                case LINK:
                    iconRes = R.drawable.ic_resource_link;
                    break;
                default:
                    iconRes = R.drawable.ic_resource_note;
                    break;
            }
            iconView.setImageResource(iconRes);

            itemView.setOnClickListener(v -> listener.onResourceClick(resource));
            menuButton.setOnClickListener(v -> listener.onResourceDelete(resource));
        }
    }
}
