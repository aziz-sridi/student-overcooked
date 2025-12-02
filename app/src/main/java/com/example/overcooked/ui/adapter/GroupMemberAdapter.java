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
import com.example.overcooked.data.model.GroupMember;
import com.example.overcooked.data.model.GroupRole;

import java.util.Objects;

/**
 * Adapter for Group Members List
 */
public class GroupMemberAdapter extends ListAdapter<GroupMember, GroupMemberAdapter.MemberViewHolder> {

    public interface OnMemberClickListener {
        void onMemberClick(GroupMember member);
    }

    private final OnMemberClickListener onMemberClick;

    private static final DiffUtil.ItemCallback<GroupMember> DIFF_CALLBACK = new DiffUtil.ItemCallback<GroupMember>() {
        @Override
        public boolean areItemsTheSame(@NonNull GroupMember oldItem, @NonNull GroupMember newItem) {
            return Objects.equals(oldItem.getUserId(), newItem.getUserId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull GroupMember oldItem, @NonNull GroupMember newItem) {
            return Objects.equals(oldItem.getUserName(), newItem.getUserName()) &&
                   oldItem.getRole() == newItem.getRole();
        }
    };

    public GroupMemberAdapter(OnMemberClickListener onMemberClick) {
        super(DIFF_CALLBACK);
        this.onMemberClick = onMemberClick;
    }

    @NonNull
    @Override
    public MemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_group_member, parent, false);
        return new MemberViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MemberViewHolder holder, int position) {
        holder.bind(getItem(position), onMemberClick);
    }

    static class MemberViewHolder extends RecyclerView.ViewHolder {
        private final View itemContainer;
        private final TextView memberInitials;
        private final TextView memberName;
        private final TextView roleBadge;

        MemberViewHolder(@NonNull View itemView) {
            super(itemView);
            itemContainer = itemView;
            memberInitials = itemView.findViewById(R.id.memberInitials);
            memberName = itemView.findViewById(R.id.memberName);
            roleBadge = itemView.findViewById(R.id.roleBadge);
        }

        void bind(GroupMember member, OnMemberClickListener onMemberClick) {
            memberName.setText(member.getUserName());
            
            // Set role text
            String roleText;
            GroupRole role = member.getRole();
            if (role == GroupRole.ADMIN) {
                roleText = "Admin";
            } else {
                roleText = "Member";
            }
            
            if (roleBadge != null) {
                roleBadge.setText(roleText);
                if (role == GroupRole.ADMIN) {
                    roleBadge.setVisibility(View.VISIBLE);
                } else {
                    roleBadge.setVisibility(View.GONE);
                }
            }

            // Set initials
            String userName = member.getUserName() != null ? member.getUserName() : "";
            String[] nameParts = userName.split(" ");
            StringBuilder initials = new StringBuilder();
            for (int i = 0; i < Math.min(2, nameParts.length); i++) {
                if (!nameParts[i].isEmpty()) {
                    initials.append(Character.toUpperCase(nameParts[i].charAt(0)));
                }
            }
            memberInitials.setText(initials.length() > 0 ? initials.toString() : "?");

            itemContainer.setOnClickListener(v -> onMemberClick.onMemberClick(member));
        }
    }
}
