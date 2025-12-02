package com.example.overcooked.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.overcooked.R;
import com.example.overcooked.data.model.TeamMember;
import com.google.android.material.card.MaterialCardView;

import java.util.Objects;

/**
 * Adapter for Team Members List
 */
public class TeamMemberAdapter extends ListAdapter<TeamMember, TeamMemberAdapter.MemberViewHolder> {

    public interface OnMemberClickListener {
        void onMemberClick(TeamMember member);
    }

    private final OnMemberClickListener onMemberClick;

    private static final DiffUtil.ItemCallback<TeamMember> DIFF_CALLBACK = new DiffUtil.ItemCallback<TeamMember>() {
        @Override
        public boolean areItemsTheSame(@NonNull TeamMember oldItem, @NonNull TeamMember newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull TeamMember oldItem, @NonNull TeamMember newItem) {
            return Objects.equals(oldItem.getName(), newItem.getName()) &&
                   Objects.equals(oldItem.getRole(), newItem.getRole());
        }
    };

    public TeamMemberAdapter(OnMemberClickListener onMemberClick) {
        super(DIFF_CALLBACK);
        this.onMemberClick = onMemberClick;
    }

    @NonNull
    @Override
    public MemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_team_member, parent, false);
        return new MemberViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MemberViewHolder holder, int position) {
        holder.bind(getItem(position), onMemberClick);
    }

    /**
     * Convenience method to update members list
     */
    public void updateMembers(java.util.List<TeamMember> members) {
        submitList(members);
    }

    static class MemberViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView cardView;
        private final TextView memberAvatar;
        private final TextView memberName;
        private final TextView memberEmail;

        MemberViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (MaterialCardView) itemView;
            memberAvatar = itemView.findViewById(R.id.memberAvatar);
            memberName = itemView.findViewById(R.id.memberName);
            memberEmail = itemView.findViewById(R.id.memberEmail);
        }

        void bind(TeamMember member, OnMemberClickListener onMemberClick) {
            memberName.setText(member.getName());
            if (memberEmail != null) {
                memberEmail.setText(formatRole(member.getRole().name()));
            }
            
            // Set initials in avatar
            if (memberAvatar != null) {
                String[] nameParts = member.getName().split(" ");
                StringBuilder initials = new StringBuilder();
                for (int i = 0; i < Math.min(2, nameParts.length); i++) {
                    if (!nameParts[i].isEmpty()) {
                        initials.append(Character.toUpperCase(nameParts[i].charAt(0)));
                    }
                }
                memberAvatar.setText(initials.length() > 0 ? initials.toString() : "?");
            }

            cardView.setOnClickListener(v -> onMemberClick.onMemberClick(member));
        }

        private String formatRole(String role) {
            return role.charAt(0) + role.substring(1).toLowerCase().replace("_", " ");
        }
    }
}
