package com.student.overcooked.ui.adapter;

import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.student.overcooked.R;
import com.student.overcooked.data.model.ProjectResource;
import com.student.overcooked.data.model.ProjectResourceType;

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
                    Objects.equals(oldItem.getType(), newItem.getType()) &&
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
        private final TextView typeBadge;
        private final TextView timestampText;
        private final ImageButton menuButton;
        private final ImageView openIcon;
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

        ResourceViewHolder(@NonNull View itemView) {
            super(itemView);
            iconView = itemView.findViewById(R.id.resourceIcon);
            titleText = itemView.findViewById(R.id.resourceTitle);
            subtitleText = itemView.findViewById(R.id.resourceSubtitle);
            typeBadge = itemView.findViewById(R.id.resourceTypeBadge);
            timestampText = itemView.findViewById(R.id.resourceTimestamp);
            menuButton = itemView.findViewById(R.id.resourceMenuButton);
            openIcon = itemView.findViewById(R.id.resourceOpenIcon);
        }

        void bind(ProjectResource resource, ResourceActionListener listener) {
            // Set title
            String title = resource.getTitle();
            if (TextUtils.isEmpty(title)) {
                title = itemView.getContext().getString(R.string.untitled_resource);
            }
            titleText.setText(title);

            ProjectResourceType type = resource.getType() != null ? resource.getType() : ProjectResourceType.NOTE;
            
            // Build subtitle based on type
            String subtitle;
            if (type == ProjectResourceType.FILE) {
                String displayName = !TextUtils.isEmpty(resource.getFileName())
                        ? resource.getFileName()
                        : itemView.getContext().getString(R.string.untitled_resource);
                if (resource.getFileSizeBytes() > 0) {
                    String sizeLabel = Formatter.formatShortFileSize(itemView.getContext(), resource.getFileSizeBytes());
                    subtitle = displayName + " • " + sizeLabel;
                } else {
                    subtitle = displayName;
                }
            } else if (type == ProjectResourceType.LINK) {
                subtitle = resource.getContent() != null ? resource.getContent() : "";
            } else {
                subtitle = resource.getContent() != null ? resource.getContent() : "";
            }
            subtitleText.setText(subtitle);

            // Set timestamp
            if (resource.getCreatedAt() != null) {
                timestampText.setText(dateFormat.format(resource.getCreatedAt()));
                timestampText.setVisibility(View.VISIBLE);
            } else {
                timestampText.setVisibility(View.GONE);
            }

            // Set icon based on type
            int iconRes;
            int badgeColor;
            String badgeText;
            switch (type) {
                case FILE:
                    iconRes = R.drawable.ic_resource_file;
                    badgeColor = ContextCompat.getColor(itemView.getContext(), R.color.burntOrange);
                    badgeText = "FILE";
                    break;
                case LINK:
                    iconRes = R.drawable.ic_resource_link;
                    badgeColor = ContextCompat.getColor(itemView.getContext(), R.color.successGreen);
                    badgeText = "LINK";
                    break;
                default:
                    iconRes = R.drawable.ic_resource_note;
                    badgeColor = ContextCompat.getColor(itemView.getContext(), R.color.mustardYellow);
                    badgeText = "NOTE";
                    break;
            }
            iconView.setImageResource(iconRes);

            // Set type badge
            if (typeBadge != null) {
                typeBadge.setText(badgeText);
                Drawable badgeBackground = typeBadge.getBackground();
                if (badgeBackground != null) {
                    Drawable wrapped = DrawableCompat.wrap(badgeBackground.mutate());
                    DrawableCompat.setTint(wrapped, badgeColor);
                    typeBadge.setBackground(wrapped);
                }
            }

            // Show open icon for files and links
            if (openIcon != null) {
                openIcon.setVisibility(type == ProjectResourceType.FILE || type == ProjectResourceType.LINK 
                        ? View.VISIBLE : View.GONE);
            }

            // Click listeners
            itemView.setOnClickListener(v -> listener.onResourceClick(resource));
            menuButton.setOnClickListener(v -> listener.onResourceDelete(resource));
        }
    }
}
