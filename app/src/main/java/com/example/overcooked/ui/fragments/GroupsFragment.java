package com.example.overcooked.ui.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.overcooked.OvercookedApplication;
import com.example.overcooked.R;
import com.example.overcooked.data.model.Group;
import com.example.overcooked.data.model.ProjectInvitation;
import com.example.overcooked.data.repository.GroupRepository;
import com.example.overcooked.data.repository.UserRepository;
import com.example.overcooked.ui.GroupDetailActivity;
import com.example.overcooked.ui.adapter.GroupListAdapter;
import com.example.overcooked.ui.adapter.ProjectInvitationAdapter;
import com.example.overcooked.ui.groups.GroupCreateController;
import com.example.overcooked.ui.groups.GroupJoinController;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

/**
 * Groups Fragment - Shows all groups user belongs to
 */
public class GroupsFragment extends Fragment implements ProjectInvitationAdapter.InvitationActionListener {

    private RecyclerView groupsRecycler;
    private RecyclerView invitationsRecycler;
    private View emptyStateLayout;
    private View invitationsSection;
    private FloatingActionButton fabAddGroup;
    private TextView groupCountText;
    private com.google.android.material.button.MaterialButton btnCreateGroup;
    private com.google.android.material.button.MaterialButton btnJoinGroup;
    private TextView coinScoreText;

    private GroupListAdapter groupAdapter;
    private ProjectInvitationAdapter invitationAdapter;

    private GroupRepository groupRepository;
    private UserRepository userRepository;
    private GroupCreateController groupCreateController;
    private GroupJoinController groupJoinController;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_groups, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        groupRepository = ((OvercookedApplication) requireActivity().getApplication()).getGroupRepository();
        userRepository = ((OvercookedApplication) requireActivity().getApplication()).getUserRepository();
        groupCreateController = new GroupCreateController(this, groupRepository, this::openGroupDetail);
        groupJoinController = new GroupJoinController(this, groupRepository, this::openGroupDetail);
        
        initializeViews(view);
        setupAdapters();
        setupClickListeners();
        observeData();
    }

    private void initializeViews(View view) {
        groupsRecycler = view.findViewById(R.id.groupsRecycler);
        invitationsRecycler = view.findViewById(R.id.invitationsRecycler);
        invitationsSection = view.findViewById(R.id.invitationsSection);
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout);
        fabAddGroup = view.findViewById(R.id.fabAddGroup);
        groupCountText = view.findViewById(R.id.groupCountText);
        btnCreateGroup = view.findViewById(R.id.btnCreateGroup);
        btnJoinGroup = view.findViewById(R.id.btnJoinGroup);
        coinScoreText = view.findViewById(R.id.coinScoreText);
        
        // Setup coin score card click listener for shop
        View coinScoreCard = view.findViewById(R.id.coinScoreCard);
        if (coinScoreCard != null) {
            coinScoreCard.setOnClickListener(v -> {
                startActivity(new android.content.Intent(requireContext(), com.example.overcooked.ui.ShopActivity.class));
            });
        }
    }

    private void setupAdapters() {
        groupAdapter = new GroupListAdapter(this::openGroupDetail);
        groupsRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        groupsRecycler.setAdapter(groupAdapter);

        invitationAdapter = new ProjectInvitationAdapter(this);
        if (invitationsRecycler != null) {
            invitationsRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
            invitationsRecycler.setAdapter(invitationAdapter);
        }
    }

    private void setupClickListeners() {
        fabAddGroup.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Projects")
                    .setItems(new String[]{"Create New Project", "Join Existing Project"}, (dialog, which) -> {
                        if (which == 0) {
                            if (groupCreateController != null) groupCreateController.showCreateProjectDialog();
                        } else {
                            if (groupJoinController != null) groupJoinController.showJoinProjectDialog();
                        }
                    })
                    .show();
        });
        btnCreateGroup.setOnClickListener(v -> {
            if (groupCreateController != null) groupCreateController.showCreateProjectDialog();
        });
        btnJoinGroup.setOnClickListener(v -> {
            if (groupJoinController != null) groupJoinController.showJoinProjectDialog();
        });
    }

    private void observeData() {
        // Observe user data for coins
        userRepository.getCurrentUser().observe(getViewLifecycleOwner(), user -> {
            if (user != null && coinScoreText != null) {
                coinScoreText.setText(String.valueOf(user.getCoins()));
            }
        });
        
        groupRepository.getUserGroups().observe(getViewLifecycleOwner(), this::updateGroupsList);
        
        // Observe pending invitations
        groupRepository.getPendingInvitations().observe(getViewLifecycleOwner(), this::updateInvitationsList);
    }

    private void updateGroupsList(List<Group> groups) {
        boolean empty = groups == null || groups.isEmpty();
        setVisible(emptyStateLayout, empty);
        setVisible(groupsRecycler, !empty);
        groupCountText.setText(empty ? "No groups yet" : groups.size() + " group" + (groups.size() != 1 ? "s" : ""));
        if (!empty) groupAdapter.submitList(groups);
    }

    private void updateInvitationsList(List<ProjectInvitation> invitations) {
        boolean hasInvitations = invitations != null && !invitations.isEmpty();
        setVisible(invitationsSection, hasInvitations);
        if (hasInvitations) {
            invitationAdapter.submitList(invitations);
        }
    }

    private void setVisible(View v, boolean visible) {
        if (v != null) v.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private void openGroupDetail(Group group) {
        if (requireActivity() instanceof com.example.overcooked.ui.MainNavActivity) {
            ((com.example.overcooked.ui.MainNavActivity) requireActivity()).navigateToGroupDetail(group.getId());
        } else {
            Intent intent = new Intent(requireContext(), GroupDetailActivity.class);
            intent.putExtra("group_id", group.getId());
            startActivity(intent);
        }
    }

    @Override
    public void onAccept(ProjectInvitation invitation) {
        groupRepository.acceptInvitation(invitation,
                aVoid -> {
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() -> 
                            Toast.makeText(requireContext(), R.string.invitation_accepted, Toast.LENGTH_SHORT).show()
                        );
                    }
                },
                e -> {
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(), "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                        );
                    }
                });
    }

    @Override
    public void onDecline(ProjectInvitation invitation) {
        groupRepository.declineInvitation(invitation,
                aVoid -> {
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(), R.string.invitation_declined, Toast.LENGTH_SHORT).show()
                        );
                    }
                },
                e -> {
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(), "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                        );
                    }
                });
    }
}
