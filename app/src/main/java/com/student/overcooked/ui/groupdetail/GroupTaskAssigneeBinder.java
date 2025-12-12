package com.student.overcooked.ui.groupdetail;

import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputLayout;
import com.student.overcooked.R;
import com.student.overcooked.data.model.GroupMember;
import com.student.overcooked.data.model.GroupTask;

import java.util.ArrayList;
import java.util.List;

final class GroupTaskAssigneeBinder {

    static void bind(@NonNull Fragment fragment,
                     boolean isIndividualProject,
                     @NonNull List<GroupMember> members,
                     @Nullable GroupTask taskToEdit,
                     @Nullable TextView assigneeLabel,
                     @Nullable TextInputLayout assigneeLayout,
                     @Nullable MaterialAutoCompleteTextView assigneeDropdown,
                     @NonNull String[] selectedAssigneeId,
                     @NonNull String[] selectedAssigneeName) {
        if (assigneeLabel == null || assigneeLayout == null) {
            return;
        }

        if (isIndividualProject || members.isEmpty()) {
            assigneeLabel.setVisibility(android.view.View.GONE);
            assigneeLayout.setVisibility(android.view.View.GONE);
            return;
        }

        assigneeLabel.setVisibility(android.view.View.VISIBLE);
        assigneeLayout.setVisibility(android.view.View.VISIBLE);

        List<String> options = new ArrayList<>();
        options.add(fragment.getString(R.string.task_assignee_unassigned));
        for (GroupMember member : members) {
            options.add(member.getUserName());
        }

        boolean isEditing = taskToEdit != null;
        boolean hasExistingAssignee = isEditing && !TextUtils.isEmpty(taskToEdit.getAssigneeName());
        if (hasExistingAssignee && !options.contains(taskToEdit.getAssigneeName())) {
            options.add(taskToEdit.getAssigneeName());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                fragment.requireContext(),
                android.R.layout.simple_list_item_1,
                options
        );

        if (assigneeDropdown == null) {
            return;
        }

        assigneeDropdown.setAdapter(adapter);
        if (hasExistingAssignee) {
            assigneeDropdown.setText(taskToEdit.getAssigneeName(), false);
        } else {
            assigneeDropdown.setText(options.get(0), false);
        }

        assigneeDropdown.setOnItemClickListener((parent, view, position, id) -> {
            if (position == 0) {
                selectedAssigneeId[0] = null;
                selectedAssigneeName[0] = null;
            } else {
                int memberIndex = position - 1;
                if (memberIndex < members.size()) {
                    GroupMember member = members.get(memberIndex);
                    selectedAssigneeId[0] = member.getUserId();
                    selectedAssigneeName[0] = member.getUserName();
                }
            }
        });
    }

    private GroupTaskAssigneeBinder() {
    }
}
