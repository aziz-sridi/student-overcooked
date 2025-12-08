package com.student.overcooked.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.student.overcooked.R;
import com.student.overcooked.data.model.ProjectInvitation;
import com.google.android.material.button.MaterialButton;

/**
 * Adapter for displaying project invitations
 */
public class ProjectInvitationAdapter extends ListAdapter<ProjectInvitation, ProjectInvitationAdapter.InvitationViewHolder> {

    public interface InvitationActionListener {
        void onAccept(ProjectInvitation invitation);
        void onDecline(ProjectInvitation invitation);
    }

    private final InvitationActionListener listener;

    public ProjectInvitationAdapter(InvitationActionListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<ProjectInvitation> DIFF_CALLBACK = new DiffUtil.ItemCallback<ProjectInvitation>() {
        @Override
        public boolean areItemsTheSame(@NonNull ProjectInvitation oldItem, @NonNull ProjectInvitation newItem) {
            return oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull ProjectInvitation oldItem, @NonNull ProjectInvitation newItem) {
            return oldItem.getStatus().equals(newItem.getStatus()) &&
                   oldItem.getGroupName().equals(newItem.getGroupName());
        }
    };

    @NonNull
    @Override
    public InvitationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_project_invitation, parent, false);
        return new InvitationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull InvitationViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static class InvitationViewHolder extends RecyclerView.ViewHolder {
        private final TextView projectNameText;
        private final TextView invitedByText;
        private final MaterialButton btnAccept;
        private final MaterialButton btnDecline;

        InvitationViewHolder(@NonNull View itemView) {
            super(itemView);
            projectNameText = itemView.findViewById(R.id.projectNameText);
            invitedByText = itemView.findViewById(R.id.invitedByText);
            btnAccept = itemView.findViewById(R.id.btnAccept);
            btnDecline = itemView.findViewById(R.id.btnDecline);
        }

        void bind(ProjectInvitation invitation, InvitationActionListener listener) {
            projectNameText.setText(invitation.getGroupName());
            invitedByText.setText(itemView.getContext().getString(
                    R.string.invitation_from, invitation.getInvitedByUserName()));

            btnAccept.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAccept(invitation);
                }
            });

            btnDecline.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDecline(invitation);
                }
            });
        }
    }
}
