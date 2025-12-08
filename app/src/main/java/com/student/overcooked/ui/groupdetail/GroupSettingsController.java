package com.student.overcooked.ui.groupdetail;

import android.app.DatePickerDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.student.overcooked.R;
import com.student.overcooked.data.model.Group;
import com.student.overcooked.data.repository.GroupRepository;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Handles project-level actions like editing metadata or deleting the group.
 */
public class GroupSettingsController {

    private final Fragment fragment;
    private final GroupRepository groupRepository;
    private final String groupId;
    private final MaterialButton btnEditProject;
    private final MaterialButton btnDeleteProject;

    private Group currentGroup;
    private boolean canManageProject;
    private boolean isIndividualProject;

    public GroupSettingsController(@NonNull Fragment fragment,
                                   @NonNull GroupRepository groupRepository,
                                   @NonNull String groupId,
                                   @Nullable MaterialButton btnEditProject,
                                   @Nullable MaterialButton btnDeleteProject) {
        this.fragment = fragment;
        this.groupRepository = groupRepository;
        this.groupId = groupId;
        this.btnEditProject = btnEditProject;
        this.btnDeleteProject = btnDeleteProject;
        if (this.btnEditProject != null) {
            this.btnEditProject.setOnClickListener(v -> showEditProjectDialog());
        }
        if (this.btnDeleteProject != null) {
            this.btnDeleteProject.setOnClickListener(v -> confirmDeleteProject());
        }
        updateButtonsState();
    }

    public void setGroup(@Nullable Group group) {
        this.currentGroup = group;
    }

    public void setAccess(boolean canManageProject, boolean isIndividualProject) {
        this.canManageProject = canManageProject;
        this.isIndividualProject = isIndividualProject;
        updateButtonsState();
    }

    private void updateButtonsState() {
        boolean enabled = canManageProject && isIndividualProject;
        if (btnEditProject != null) {
            btnEditProject.setEnabled(enabled);
            btnEditProject.setAlpha(enabled ? 1f : 0.4f);
        }
        if (btnDeleteProject != null) {
            btnDeleteProject.setEnabled(enabled);
            btnDeleteProject.setAlpha(enabled ? 1f : 0.4f);
        }
    }

    private void showEditProjectDialog() {
        if (currentGroup == null || !fragment.isAdded()) {
            return;
        }

        View dialogView = LayoutInflater.from(fragment.requireContext()).inflate(R.layout.dialog_create_project, null);
        TextInputEditText nameInput = dialogView.findViewById(R.id.projectNameInput);
        MaterialAutoCompleteTextView subjectInput = dialogView.findViewById(R.id.subjectInput);
        TextInputEditText descInput = dialogView.findViewById(R.id.descriptionInput);
        RadioGroup typeGroup = dialogView.findViewById(R.id.projectTypeGroup);
        TextInputLayout deadlineInputLayout = dialogView.findViewById(R.id.deadlineInputLayout);
        TextInputEditText deadlineValue = dialogView.findViewById(R.id.deadlineValue);
        View deadlineButton = dialogView.findViewById(R.id.btnPickDeadline);
        SwitchMaterial workspaceSwitch = dialogView.findViewById(R.id.createWorkspaceSwitch);
        View workspaceHelper = dialogView.findViewById(R.id.workspaceHelperText);
        MaterialButton actionButton = dialogView.findViewById(R.id.btnCreate);
        LinearProgressIndicator progressIndicator = dialogView.findViewById(R.id.createProjectProgress);
        View btnClose = dialogView.findViewById(R.id.btnClose);

        nameInput.setText(currentGroup.getName());
        subjectInput.setText(currentGroup.getSubject(), false);
        descInput.setText(currentGroup.getDescription());
        typeGroup.check(currentGroup.isIndividualProject() ? R.id.radioIndividual : R.id.radioGroup);
        actionButton.setText(R.string.save_changes);
        if (workspaceSwitch != null) {
            workspaceSwitch.setVisibility(View.GONE);
        }
        if (workspaceHelper != null) {
            workspaceHelper.setVisibility(View.GONE);
        }

        final Calendar deadlineCalendar = Calendar.getInstance();
        if (currentGroup.getDeadline() != null) {
            deadlineCalendar.setTime(currentGroup.getDeadline());
            deadlineValue.setText(DateFormat.getDateInstance(DateFormat.MEDIUM).format(currentGroup.getDeadline()));
        }

        final Date[] selectedDeadline = {currentGroup.getDeadline()};
        View.OnClickListener showPicker = v -> {
            DatePickerDialog picker = new DatePickerDialog(fragment.requireContext(), (view, year, month, dayOfMonth) -> {
                deadlineCalendar.set(Calendar.YEAR, year);
                deadlineCalendar.set(Calendar.MONTH, month);
                deadlineCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                selectedDeadline[0] = deadlineCalendar.getTime();
                deadlineValue.setText(DateFormat.getDateInstance(DateFormat.MEDIUM).format(selectedDeadline[0]));
            }, deadlineCalendar.get(Calendar.YEAR), deadlineCalendar.get(Calendar.MONTH), deadlineCalendar.get(Calendar.DAY_OF_MONTH));
            picker.show();
        };
        deadlineButton.setOnClickListener(showPicker);
        if (deadlineInputLayout != null) {
            deadlineInputLayout.setEndIconOnClickListener(showPicker);
        }

        android.app.Dialog dialog = new android.app.Dialog(fragment.requireContext());
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        dialog.setContentView(dialogView);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        actionButton.setOnClickListener(v -> {
            String name = nameInput.getText() != null ? nameInput.getText().toString().trim() : "";
            String subject = subjectInput.getText() != null ? subjectInput.getText().toString().trim() : "";
            String description = descInput.getText() != null ? descInput.getText().toString().trim() : "";
            boolean individual = typeGroup.getCheckedRadioButtonId() == R.id.radioIndividual;

            if (TextUtils.isEmpty(name)) {
                nameInput.setError(fragment.getString(R.string.group_name_hint));
                return;
            }

            actionButton.setEnabled(false);
            if (progressIndicator != null) {
                progressIndicator.setVisibility(View.VISIBLE);
            }
            groupRepository.updateGroupDetails(groupId, name, subject, description, selectedDeadline[0], individual,
                    group -> fragment.requireActivity().runOnUiThread(() -> {
                        Toast.makeText(fragment.requireContext(), R.string.project_updated, Toast.LENGTH_SHORT).show();
                        if (progressIndicator != null) {
                            progressIndicator.setVisibility(View.GONE);
                        }
                        dialog.dismiss();
                    }),
                    e -> fragment.requireActivity().runOnUiThread(() -> {
                        actionButton.setEnabled(true);
                        if (progressIndicator != null) {
                            progressIndicator.setVisibility(View.GONE);
                        }
                        Toast.makeText(fragment.requireContext(), R.string.error_occurred, Toast.LENGTH_SHORT).show();
                    }));
        });

        dialog.show();
    }

    private void confirmDeleteProject() {
        if (!fragment.isAdded()) {
            return;
        }
        new MaterialAlertDialogBuilder(fragment.requireContext())
                .setTitle(R.string.confirm_delete_project_title)
                .setMessage(R.string.confirm_delete_project_message)
                .setPositiveButton(R.string.delete, (dialog, which) ->
                        groupRepository.deleteGroup(groupId,
                                aVoid -> {
                                    Toast.makeText(fragment.requireContext(), R.string.project_deleted, Toast.LENGTH_SHORT).show();
                                    fragment.requireActivity().getOnBackPressedDispatcher().onBackPressed();
                                },
                                e -> Toast.makeText(fragment.requireContext(), R.string.error_occurred, Toast.LENGTH_SHORT).show()))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}
