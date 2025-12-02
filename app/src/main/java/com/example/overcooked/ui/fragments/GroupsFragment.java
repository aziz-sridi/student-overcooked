package com.example.overcooked.ui.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.overcooked.OvercookedApplication;
import com.example.overcooked.R;
import com.example.overcooked.data.model.Group;
import com.example.overcooked.data.repository.GroupRepository;
import com.example.overcooked.ui.GroupDetailActivity;
import com.example.overcooked.ui.adapter.GroupListAdapter;
import com.example.overcooked.ui.groups.GroupCreateController;
import com.example.overcooked.ui.groups.GroupJoinController;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

/**
 * Groups Fragment - Shows all groups user belongs to
 */
public class GroupsFragment extends Fragment {

    private RecyclerView groupsRecycler;
    private View emptyStateLayout;
    private FloatingActionButton fabAddGroup;
    private TextView groupCountText;
    private com.google.android.material.button.MaterialButton btnCreateGroup;
    private com.google.android.material.button.MaterialButton btnJoinGroup;

    private GroupListAdapter groupAdapter;

    private GroupRepository groupRepository;
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
        groupCreateController = new GroupCreateController(this, groupRepository, this::openGroupDetail);
        groupJoinController = new GroupJoinController(this, groupRepository, this::openGroupDetail);
        
        initializeViews(view);
        setupAdapters();
        setupClickListeners();
        observeData();
    }

    private void initializeViews(View view) {
        groupsRecycler = view.findViewById(R.id.groupsRecycler);
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout);
        fabAddGroup = view.findViewById(R.id.fabAddGroup);
        groupCountText = view.findViewById(R.id.groupCountText);
        btnCreateGroup = view.findViewById(R.id.btnCreateGroup);
        btnJoinGroup = view.findViewById(R.id.btnJoinGroup);
    }

    private void setupAdapters() {
        groupAdapter = new GroupListAdapter(this::openGroupDetail);
        groupsRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        groupsRecycler.setAdapter(groupAdapter);
    }

    private void setupClickListeners() {
        fabAddGroup.setOnClickListener(v -> showGroupOptionsDialog());
        btnCreateGroup.setOnClickListener(v -> {
            if (groupCreateController != null) {
                groupCreateController.showCreateProjectDialog();
            }
        });
        btnJoinGroup.setOnClickListener(v -> {
            if (groupJoinController != null) {
                groupJoinController.showJoinProjectDialog();
            }
        });
    }

    private void showGroupOptionsDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Projects")
                .setItems(new String[]{"Create New Project", "Join Existing Project"}, (dialog, which) -> {
                    if (which == 0) {
                        if (groupCreateController != null) {
                            groupCreateController.showCreateProjectDialog();
                        }
                    } else {
                        if (groupJoinController != null) {
                            groupJoinController.showJoinProjectDialog();
                        }
                    }
                })
                .show();
    }

    private void observeData() {
        groupRepository.getUserGroups().observe(getViewLifecycleOwner(), this::updateGroupsList);
    }

    private void updateGroupsList(List<Group> groups) {
        if (groups == null || groups.isEmpty()) {
            emptyStateLayout.setVisibility(View.VISIBLE);
            groupsRecycler.setVisibility(View.GONE);
            groupCountText.setText("No groups yet");
        } else {
            emptyStateLayout.setVisibility(View.GONE);
            groupsRecycler.setVisibility(View.VISIBLE);
            groupAdapter.submitList(groups);
            groupCountText.setText(groups.size() + " group" + (groups.size() != 1 ? "s" : ""));
        }
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
}
