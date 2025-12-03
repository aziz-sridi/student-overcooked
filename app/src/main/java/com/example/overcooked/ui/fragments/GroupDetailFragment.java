package com.example.overcooked.ui.fragments;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.example.overcooked.OvercookedApplication;
import com.example.overcooked.R;
import com.example.overcooked.data.model.Group;
import com.example.overcooked.data.repository.GroupRepository;
import com.example.overcooked.ui.groupdetail.GroupChatController;
import com.example.overcooked.ui.groupdetail.GroupMembersController;
import com.example.overcooked.ui.groupdetail.GroupSettingsController;
import com.example.overcooked.ui.groupdetail.GroupTasksController;
import com.example.overcooked.ui.workspace.GroupWorkspaceController;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles tasks, chat, members,
 * 
 * workspace resources, and project settings inside a single shared layout.
 */
public class GroupDetailFragment extends Fragment {

    private static final String ARG_GROUP_ID = "group_id";

    public enum Section {
        TASKS,
        CHAT,
        MEMBERS,
        WORKSPACE,
        SETTINGS
    }

    private String groupId = "";
    private GroupRepository groupRepository;
    private Group currentGroup;
    private boolean isAdmin;
    private boolean canManageProject;
    private boolean isIndividualProject;
    private Section activeSection = Section.TASKS;

    private final List<Section> tabSections = new ArrayList<>();

    // Header UI
    private TextView groupNameText;
    private TextView groupSubjectText;
    private TextView groupProgressText;
    private TextView joinCodeText;
    private TabLayout tabLayout;

    // Containers
    private View tasksContainer;
    private View chatContainer;
    private View membersContainer;
    private View workspaceContainer;
    private View settingsContainer;

    // Tasks tab
    private RecyclerView tasksRecycler;
    private FloatingActionButton fabAddTask;

    // Chat tab
    private RecyclerView chatRecycler;
    private EditText messageInput;
    private ImageButton sendButton;

    // Members tab
    private RecyclerView membersRecycler;

    // Settings tab
    private MaterialButton btnEditProject;
    private MaterialButton btnDeleteProject;

    private GroupWorkspaceController workspaceController;
    private ActivityResultLauncher<String[]> workspaceFilePickerLauncher;
    private GroupTasksController tasksController;
    private GroupChatController chatController;
    private GroupMembersController membersController;
    private GroupSettingsController settingsController;

    private static final String STATE_ACTIVE_SECTION = "active_section";

    public static GroupDetailFragment newInstance(@NonNull String groupId) {
        GroupDetailFragment fragment = new GroupDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_GROUP_ID, groupId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            groupId = args.getString(ARG_GROUP_ID, "");
        }
        if (TextUtils.isEmpty(groupId)) {
            groupId = "";
        }
        groupRepository = ((OvercookedApplication) requireActivity().getApplication()).getGroupRepository();
        workspaceFilePickerLauncher = registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
            if (uri != null && workspaceController != null) {
                workspaceController.handleFilePicked(uri);
            }
        });
        if (savedInstanceState != null) {
            int ordinal = savedInstanceState.getInt(STATE_ACTIVE_SECTION, Section.TASKS.ordinal());
            ordinal = Math.max(0, Math.min(ordinal, Section.values().length - 1));
            activeSection = Section.values()[ordinal];
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.activity_group_detail, container, false);
        initializeViews(root);
        initializeWorkspaceController(root);
        initializeSectionControllers();
        setupTabs();
        loadGroupData();
        return root;
    }

    private void initializeViews(@NonNull View root) {
        groupNameText = root.findViewById(R.id.groupNameText);
        groupSubjectText = root.findViewById(R.id.groupSubjectText);
        groupProgressText = root.findViewById(R.id.groupProgressText);
        joinCodeText = root.findViewById(R.id.joinCodeText);
        tabLayout = root.findViewById(R.id.tabLayout);

        tasksContainer = root.findViewById(R.id.tasksContainer);
        chatContainer = root.findViewById(R.id.chatContainer);
        membersContainer = root.findViewById(R.id.membersContainer);
        workspaceContainer = root.findViewById(R.id.workspaceContainer);
        settingsContainer = root.findViewById(R.id.settingsContainer);

        tasksRecycler = root.findViewById(R.id.tasksRecycler);
        fabAddTask = root.findViewById(R.id.fabAddTask);

        chatRecycler = root.findViewById(R.id.chatRecycler);
        messageInput = root.findViewById(R.id.messageInput);
        sendButton = root.findViewById(R.id.sendButton);

        membersRecycler = root.findViewById(R.id.membersRecycler);

        btnEditProject = root.findViewById(R.id.btnEditProject);
        btnDeleteProject = root.findViewById(R.id.btnDeleteProject);

        if (joinCodeText != null) {
            joinCodeText.setOnClickListener(v -> copyJoinCodeToClipboard());
        }

        ImageView backButton = root.findViewById(R.id.backButton);
        if (backButton != null) {
            backButton.setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());
        }
    }

    private void initializeWorkspaceController(@NonNull View root) {
        if (groupRepository == null) {
            return;
        }
        RecyclerView workspaceRecycler = root.findViewById(R.id.workspaceRecycler);
        View workspaceEmptyState = root.findViewById(R.id.workspaceEmptyState);
        View addWorkspaceButton = root.findViewById(R.id.btnAddWorkspaceItem);
        workspaceController = new GroupWorkspaceController(
                this,
                groupRepository,
                groupId,
                workspaceRecycler,
                workspaceEmptyState,
                addWorkspaceButton
        );
        workspaceController.setFilePickerLauncher(workspaceFilePickerLauncher);
    }

    private void initializeSectionControllers() {
        if (groupRepository == null) {
            return;
        }
        if (tasksRecycler != null) {
            tasksController = new GroupTasksController(this, groupRepository, groupId, tasksRecycler);
        }
        if (chatRecycler != null && messageInput != null && sendButton != null) {
            chatController = new GroupChatController(this, groupRepository, groupId, chatRecycler, messageInput, sendButton);
        }
        if (membersRecycler != null) {
            membersController = new GroupMembersController(this, groupRepository, groupId, membersRecycler);
        }
        if (btnEditProject != null && btnDeleteProject != null) {
            settingsController = new GroupSettingsController(this, groupRepository, groupId, btnEditProject, btnDeleteProject);
        }
    }

    private void setupTabs() {
        if (tabLayout == null) {
            return;
        }
        // Clear existing listeners to avoid duplicates after view recreation
        tabLayout.clearOnTabSelectedListeners();

        rebuildTabs();
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                showSection(resolveSectionForTab(tab.getPosition()));
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                // no-op
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                showSection(resolveSectionForTab(tab.getPosition()));
            }
        });
        showSection(activeSection);
    }

    private void rebuildTabs() {
        if (tabLayout == null) {
            return;
        }
        if (!isIndividualProject && (activeSection == Section.WORKSPACE || activeSection == Section.SETTINGS)) {
            activeSection = Section.TASKS;
        }
        tabSections.clear();
        tabLayout.removeAllTabs();
        addTabForSection(Section.TASKS, R.string.tab_tasks);
        addTabForSection(Section.CHAT, R.string.tab_chat);
        addTabForSection(Section.MEMBERS, R.string.tab_members);
        if (isIndividualProject) {
            addTabForSection(Section.WORKSPACE, R.string.workspace_tab_title);
            addTabForSection(Section.SETTINGS, R.string.tab_settings);
        }
        int index = tabSections.indexOf(activeSection);
        if (index < 0) {
            index = 0;
            activeSection = tabSections.isEmpty() ? Section.TASKS : tabSections.get(0);
        }
        TabLayout.Tab selected = tabLayout.getTabAt(index);
        if (selected != null) {
            selected.select();
        }
    }

    private void addTabForSection(@NonNull Section section, int titleRes) {
        if (tabLayout == null) {
            return;
        }
        tabSections.add(section);
        TabLayout.Tab tab = tabLayout.newTab();
        tab.setText(titleRes);
        tabLayout.addTab(tab, false);
    }

    private void loadGroupData() {
        if (groupRepository == null || TextUtils.isEmpty(groupId)) {
            return;
        }

        groupRepository.getGroup(groupId, group -> {
            currentGroup = group;
            if (!isAdded() || group == null) {
                return;
            }
            requireActivity().runOnUiThread(() -> {
                bindGroupInfo(group);
                isIndividualProject = group.isIndividualProject();
                if (workspaceController != null) {
                    workspaceController.setIndividualProject(isIndividualProject);
                }
                if (tasksController != null) {
                    tasksController.setIndividualProject(isIndividualProject);
                }
                if (settingsController != null) {
                    settingsController.setGroup(group);
                }
                if (membersController != null) {
                    membersController.setGroupName(group.getName());
                }
                rebuildTabs();
                updateManagementFlags();
            });
        }, e -> {
            if (isAdded()) {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "Failed to load group.", Toast.LENGTH_SHORT).show()
                );
            }
        });

        groupRepository.isGroupAdmin(groupId, admin -> {
            isAdmin = admin != null && admin;
            updateManagementFlags();
        }, e -> {});

        groupRepository.getGroupTasks(groupId).observe(getViewLifecycleOwner(), tasks -> {
            if (tasksController != null) {
                tasksController.submitTasks(tasks);
            }
        });

        groupRepository.getGroupMessages(groupId).observe(getViewLifecycleOwner(), messages -> {
            if (chatController != null) {
                chatController.submitMessages(messages);
            }
        });

        groupRepository.getGroupMembers(groupId).observe(getViewLifecycleOwner(), members -> {
            if (membersController != null) {
                membersController.submitMembers(members);
            }
            if (tasksController != null) {
                tasksController.setMembers(members);
            }
        });

        groupRepository.getProjectResources(groupId).observe(getViewLifecycleOwner(), resources -> {
            if (workspaceController != null) {
                workspaceController.submitResources(resources);
            }
        });
    }

    private void bindGroupInfo(@NonNull Group group) {
        if (groupNameText != null) {
            groupNameText.setText(group.getName());
        }
        if (groupSubjectText != null) {
            String subject = group.getSubject();
            groupSubjectText.setText(TextUtils.isEmpty(subject) ? getString(R.string.not_connected) : subject);
        }
        if (groupProgressText != null) {
            groupProgressText.setText(group.getProgressText());
        }
        if (joinCodeText != null) {
            String joinCode = !TextUtils.isEmpty(group.getJoinCode()) ? group.getJoinCode() : getString(R.string.not_connected);
            joinCodeText.setText("Code: " + joinCode);
        }
    }

    private void updateManagementFlags() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        boolean isOwner = currentGroup != null && user != null && TextUtils.equals(user.getUid(), currentGroup.getCreatedBy());
        canManageProject = isAdmin || isOwner;
        if (isAdded()) {
            requireActivity().runOnUiThread(() -> {
                if (settingsController != null) {
                    settingsController.setAccess(canManageProject, isIndividualProject);
                }
                if (membersController != null) {
                    membersController.setAdmin(isAdmin);
                }
                showSection(activeSection);
            });
        }
    }

    private void copyJoinCodeToClipboard() {
        if (currentGroup == null || getContext() == null) {
            return;
        }
        String joinCode = currentGroup.getJoinCode();
        if (TextUtils.isEmpty(joinCode)) {
            Toast.makeText(requireContext(), "No join code available.", Toast.LENGTH_SHORT).show();
            return;
        }
        ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            ClipData clip = ClipData.newPlainText("Join Code", joinCode);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(requireContext(), "Join code copied!", Toast.LENGTH_SHORT).show();
        }
    }

    private Section resolveSectionForTab(int position) {
        if (position >= 0 && position < tabSections.size()) {
            return tabSections.get(position);
        }
        return Section.TASKS;
    }

    // Small helpers to reduce visibility boilerplate
    private void setVisibility(View v, int visibility) { if (v != null) v.setVisibility(visibility); }
    private void hideFab() {
        if (fabAddTask != null) {
            fabAddTask.setVisibility(View.GONE);
            fabAddTask.setOnClickListener(null);
        }
    }

    // Data-driven section rendering
    private interface FabConfigurator { void configure(FloatingActionButton fab); }
    private void renderSection(@NonNull View visibleContainer, @Nullable FabConfigurator configurator) {
        setVisibility(tasksContainer, visibleContainer == tasksContainer ? View.VISIBLE : View.GONE);
        setVisibility(chatContainer, visibleContainer == chatContainer ? View.VISIBLE : View.GONE);
        setVisibility(membersContainer, visibleContainer == membersContainer ? View.VISIBLE : View.GONE);
        setVisibility(workspaceContainer, visibleContainer == workspaceContainer ? View.VISIBLE : View.GONE);
        setVisibility(settingsContainer, visibleContainer == settingsContainer ? View.VISIBLE : View.GONE);
        if (configurator != null && fabAddTask != null) {
            configurator.configure(fabAddTask);
        } else {
            hideFab();
        }
    }

    private void showSection(@NonNull Section section) {
        activeSection = section;
        switch (section) {
            case CHAT:
                renderSection(chatContainer, null);
                break;
            case MEMBERS:
                renderSection(membersContainer, membersController != null ? fab -> membersController.configureFab(fab) : null);
                break;
            case WORKSPACE:
                if (!isIndividualProject) { showSection(Section.TASKS); break; }
                renderSection(workspaceContainer, workspaceController != null ? fab -> workspaceController.configureFab(fab) : null);
                break;
            case SETTINGS:
                if (!isIndividualProject) { showSection(Section.TASKS); break; }
                renderSection(settingsContainer, null);
                break;
            case TASKS:
            default:
                renderSection(tasksContainer, tasksController != null ? fab -> tasksController.configureFab(fab) : null);
                break;
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_ACTIVE_SECTION, activeSection.ordinal());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (tabLayout != null) {
            tabLayout.clearOnTabSelectedListeners();
        }
        workspaceController = null;
        tasksController = null;
        chatController = null;
        membersController = null;
        settingsController = null;
    }
}
