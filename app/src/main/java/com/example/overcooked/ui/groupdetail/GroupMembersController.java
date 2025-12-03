package com.example.overcooked.ui.groupdetail;

import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.overcooked.R;
import com.example.overcooked.data.model.GroupMember;
import com.example.overcooked.data.repository.GroupRepository;
import com.example.overcooked.ui.adapter.GroupMemberAdapter;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

/**
 * Manages member list interactions, inviting, and removal.
 */
public class GroupMembersController {

    private final Fragment fragment;
    private final GroupRepository groupRepository;
    private final String groupId;
    private final RecyclerView membersRecycler;

    private final GroupMemberAdapter memberAdapter;
    private boolean isAdmin;
    private String groupName;

    public GroupMembersController(@NonNull Fragment fragment,
                                  @NonNull GroupRepository groupRepository,
                                  @NonNull String groupId,
                                  @Nullable RecyclerView membersRecycler) {
        this.fragment = fragment;
        this.groupRepository = groupRepository;
        this.groupId = groupId;
        this.membersRecycler = membersRecycler;
        this.groupName = "";
        this.memberAdapter = new GroupMemberAdapter(member -> {
            if (isAdmin) {
                showRemoveMemberDialog(member);
            }
        });
        if (this.membersRecycler != null) {
            this.membersRecycler.setLayoutManager(new LinearLayoutManager(fragment.requireContext()));
            this.membersRecycler.setAdapter(memberAdapter);
        }
    }

    public void setAdmin(boolean admin) {
        isAdmin = admin;
    }

    public void setGroupName(@Nullable String name) {
        this.groupName = name != null ? name : "";
    }

    public void submitMembers(@Nullable List<GroupMember> members) {
        memberAdapter.submitList(members);
    }

    public void configureFab(@Nullable FloatingActionButton fab) {
        if (fab == null) {
            return;
        }
        if (isAdmin) {
            fab.setVisibility(View.VISIBLE);
            fab.setImageResource(android.R.drawable.ic_menu_add);
            fab.setOnClickListener(v -> showInviteMemberDialog());
        } else {
            fab.setVisibility(View.GONE);
            fab.setOnClickListener(null);
        }
    }

    private void showInviteMemberDialog() {
        if (!fragment.isAdded()) {
            return;
        }
        final EditText input = new EditText(fragment.requireContext());
        input.setHint(R.string.invite_member_hint);
        int padding = (int) (16 * fragment.getResources().getDisplayMetrics().density);
        input.setPadding(padding, padding, padding, padding);

        new MaterialAlertDialogBuilder(fragment.requireContext())
                .setTitle(R.string.invite_member_title)
                .setMessage(R.string.invite_member_message)
                .setView(input)
                .setPositiveButton(R.string.send_invitation, (dialog, which) -> {
                    String usernameOrEmail = input.getText().toString().trim();
                    if (TextUtils.isEmpty(usernameOrEmail)) {
                        return;
                    }
                    groupRepository.sendProjectInvitation(groupId, groupName, usernameOrEmail,
                            invitation -> fragment.requireActivity().runOnUiThread(() ->
                                    Toast.makeText(fragment.requireContext(), R.string.invitation_sent, Toast.LENGTH_SHORT).show()),
                            e -> fragment.requireActivity().runOnUiThread(() ->
                                    Toast.makeText(fragment.requireContext(), fragment.getString(R.string.invitation_failed, e.getMessage()), Toast.LENGTH_SHORT).show()));
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showRemoveMemberDialog(@NonNull GroupMember member) {
        if (!fragment.isAdded()) {
            return;
        }
        new MaterialAlertDialogBuilder(fragment.requireContext())
                .setTitle("Remove Member")
                .setMessage("Remove " + member.getUserName() + " from the group?")
                .setPositiveButton("Remove", (dialog, which) ->
                        groupRepository.removeMember(groupId, member.getId(),
                                aVoid -> {
                                },
                                e -> fragment.requireActivity().runOnUiThread(() ->
                                        Toast.makeText(fragment.requireContext(), "Failed to remove member", Toast.LENGTH_SHORT).show())))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}
