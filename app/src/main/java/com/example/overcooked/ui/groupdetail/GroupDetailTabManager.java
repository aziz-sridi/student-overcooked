package com.example.overcooked.ui.groupdetail;

import androidx.annotation.NonNull;
import com.example.overcooked.R;
import com.example.overcooked.ui.fragments.GroupDetailFragment;
import com.example.overcooked.ui.fragments.GroupDetailFragment.Section;
import com.google.android.material.tabs.TabLayout;
import java.util.ArrayList;
import java.util.List;

public class GroupDetailTabManager {
    public interface OnSectionChangeListener {
        void onSectionChanged(@NonNull Section section);
    }

    private final TabLayout tabLayout;
    private final OnSectionChangeListener listener;
    private final List<Section> tabSections = new ArrayList<>();
    private boolean isIndividualProject;
    private Section activeSection;

    public GroupDetailTabManager(@NonNull TabLayout tabLayout,
                                 boolean isIndividualProject,
                                 @NonNull Section initialSection,
                                 @NonNull OnSectionChangeListener listener) {
        this.tabLayout = tabLayout;
        this.isIndividualProject = isIndividualProject;
        this.activeSection = initialSection;
        this.listener = listener;
    }

    public void initialize() {
        rebuildTabs();
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                listener.onSectionChanged(resolveSectionForTab(tab.getPosition()));
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) { /* no-op */ }
            @Override public void onTabReselected(TabLayout.Tab tab) {
                listener.onSectionChanged(resolveSectionForTab(tab.getPosition()));
            }
        });
    }

    public void rebuildTabs() {
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

    public Section resolveSectionForTab(int position) {
        if (position >= 0 && position < tabSections.size()) {
            return tabSections.get(position);
        }
        return Section.TASKS;
    }

    public void setIsIndividualProject(boolean individual) {
        this.isIndividualProject = individual;
    }

    public void setActiveSection(@NonNull Section section) {
        this.activeSection = section;
    }

    public Section getActiveSection() {
        return activeSection;
    }

    private void addTabForSection(@NonNull Section section, int titleRes) {
        tabSections.add(section);
        TabLayout.Tab tab = tabLayout.newTab();
        tab.setText(titleRes);
        tabLayout.addTab(tab, false);
    }
}
