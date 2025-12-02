package com.example.overcooked.ui.groups;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.overcooked.data.model.Group;
import com.example.overcooked.data.repository.GroupRepository;
import com.example.overcooked.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.util.function.Consumer;

/**
 * Handles the Join Project dialog lifecycle for {@link com.example.overcooked.ui.fragments.GroupsFragment}.
 */
public class GroupJoinController {

    private final Fragment fragment;
    private final GroupRepository groupRepository;
    private final Consumer<Group> onGroupSelected;

    public GroupJoinController(@NonNull Fragment fragment,
                               @NonNull GroupRepository groupRepository,
                               @NonNull Consumer<Group> onGroupSelected) {
        this.fragment = fragment;
        this.groupRepository = groupRepository;
        this.onGroupSelected = onGroupSelected;
    }

    public void showJoinProjectDialog() {
        if (!fragment.isAdded()) {
            return;
        }
        View dialogView = LayoutInflater.from(fragment.requireContext()).inflate(R.layout.dialog_join_project, null);
        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(fragment.requireContext())
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        TextInputEditText codeInput = dialogView.findViewById(R.id.joinCodeInput);
        View btnClose = dialogView.findViewById(R.id.btnClose);
        View btnJoin = dialogView.findViewById(R.id.btnJoin);

        btnClose.setOnClickListener(v -> dialog.dismiss());
        btnJoin.setOnClickListener(v -> attemptJoinProject(dialog, codeInput));
        dialog.show();
    }

    private void attemptJoinProject(@NonNull androidx.appcompat.app.AlertDialog dialog,
                                    TextInputEditText codeInput) {
        if (!fragment.isAdded()) {
            return;
        }
        String code = codeInput != null && codeInput.getText() != null
                ? codeInput.getText().toString().trim()
                : "";
        if (code.isEmpty()) {
            Toast.makeText(fragment.requireContext(), "Code is required", Toast.LENGTH_SHORT).show();
            return;
        }
        groupRepository.joinGroup(code,
                group -> {
                    if (!fragment.isAdded()) {
                        return;
                    }
                    if (group != null) {
                        Toast.makeText(fragment.requireContext(), "Joined project!", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        onGroupSelected.accept(group);
                    } else {
                        Toast.makeText(fragment.requireContext(), "Project not found", Toast.LENGTH_SHORT).show();
                    }
                },
                e -> {
                    if (!fragment.isAdded()) {
                        return;
                    }
                    Toast.makeText(fragment.requireContext(), "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
