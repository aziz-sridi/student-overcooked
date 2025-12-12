package com.student.overcooked.ui.fragments;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.student.overcooked.OvercookedApplication;
import com.student.overcooked.R;
import com.student.overcooked.data.LocalCoinStore;
import com.student.overcooked.data.repository.GroupRepository;
import com.student.overcooked.ui.common.CoinTopBarController;
import com.student.overcooked.ui.groupdetail.GroupChatController;
import com.student.overcooked.ui.groupdetail.GroupDetailControllersFactory;
import com.student.overcooked.ui.groupdetail.GroupDetailDataLoader;
import com.student.overcooked.ui.groupdetail.GroupDetailSectionNavigator;
import com.student.overcooked.ui.groupdetail.GroupDetailTabManager;
import com.student.overcooked.ui.groupdetail.GroupDetailViews;
import com.student.overcooked.ui.groupdetail.GroupMembersController;
import com.student.overcooked.ui.groupdetail.GroupSettingsController;
import com.student.overcooked.ui.groupdetail.GroupTasksController;
import com.student.overcooked.ui.workspace.GroupWorkspaceController;

/**
 * Handles tasks, chat, members, workspace resources, and project settings inside a single shared layout.
 */
public class GroupDetailFragment extends Fragment {

    private static final String ARG_GROUP_ID = "group_id";
    private static final String STATE_ACTIVE_SECTION = "active_section";

    public enum Section {
        TASKS,
        CHAT,
        MEMBERS,
        WORKSPACE,
        SETTINGS
    }

    private String groupId = "";
    private GroupRepository groupRepository;
    private Section activeSection = Section.TASKS;

    private GroupDetailViews views;

    private ActivityResultLauncher<String[]> workspaceFilePickerLauncher;

    private GroupWorkspaceController workspaceController;
    private GroupTasksController tasksController;
    private GroupChatController chatController;
    private GroupMembersController membersController;
    private GroupSettingsController settingsController;

    private CoinTopBarController coinTopBar;
    private GroupDetailTabManager tabManager;
    private GroupDetailSectionNavigator sectionNavigator;
    private GroupDetailDataLoader dataLoader;

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
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.activity_group_detail, container, false);

        views = GroupDetailViews.bind(root);

        if (views.backButton != null) {
            views.backButton.setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());
        }

        workspaceController = GroupDetailControllersFactory.createWorkspaceController(
            this,
            groupRepository,
            groupId,
            root,
            workspaceFilePickerLauncher
        );

        GroupDetailControllersFactory.SectionControllers controllers = GroupDetailControllersFactory.createSectionControllers(
            this,
            groupRepository,
            groupId,
            views.tasksRecycler,
            views.groupTaskStatusFilterChipGroup,
            views.chatRecycler,
            views.messageInput,
            views.sendButton,
            views.membersRecycler,
            views.btnEditProject,
            views.btnDeleteProject
        );
        tasksController = controllers.tasksController;
        chatController = controllers.chatController;
        membersController = controllers.membersController;
        settingsController = controllers.settingsController;

        sectionNavigator = new GroupDetailSectionNavigator(
            views.tasksContainer,
            views.chatContainer,
            views.membersContainer,
            views.workspaceContainer,
            views.settingsContainer,
            views.fabAddTask
        );
        sectionNavigator.attachControllers(tasksController, chatController, membersController, workspaceController, settingsController);

        setupTabs();

        coinTopBar = new CoinTopBarController(
                this,
                new LocalCoinStore(requireContext()),
                ((OvercookedApplication) requireActivity().getApplication()).getUserRepository()
        );
        coinTopBar.bind(root);

        dataLoader = new GroupDetailDataLoader(
                this,
                groupRepository,
                groupId,
                views.groupNameText,
                views.groupSubjectText,
                views.groupProgressText,
                views.joinCodeText,
                workspaceController,
                tasksController,
                chatController,
                membersController,
                settingsController,
                tabManager,
                sectionNavigator
        );

        if (views.joinCodeText != null) {
            views.joinCodeText.setOnClickListener(v -> {
                if (dataLoader != null) {
                    dataLoader.copyJoinCodeToClipboard();
                }
            });
        }

        dataLoader.start(new GroupDetailDataLoader.ActiveSectionAccessor() {
            @NonNull
            @Override
            public Section get() {
                return activeSection;
            }

            @Override
            public void set(@NonNull Section section) {
                activeSection = section;
                if (tabManager != null) {
                    tabManager.setActiveSection(section);
                }
            }
        });

        return root;
    }

    private void setupTabs() {
        if (views == null || views.tabLayout == null) return;

        views.tabLayout.clearOnTabSelectedListeners();

        tabManager = new GroupDetailTabManager(
            views.tabLayout,
                false,
                activeSection,
                section -> {
                    activeSection = section;
                    boolean individual = dataLoader != null && dataLoader.isIndividualProject();
                    if (sectionNavigator != null) {
                        sectionNavigator.showSection(section, individual);
                    }
                }
        );

        tabManager.initialize();
        activeSection = tabManager.getActiveSection();

        if (sectionNavigator != null) {
            boolean individual = dataLoader != null && dataLoader.isIndividualProject();
            sectionNavigator.showSection(activeSection, individual);
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

        if (views != null && views.tabLayout != null) {
            views.tabLayout.clearOnTabSelectedListeners();
        }

        workspaceController = null;
        tasksController = null;
        chatController = null;
        membersController = null;
        settingsController = null;

        sectionNavigator = null;
        tabManager = null;
        dataLoader = null;
        views = null;
    }
}
