package com.student.overcooked.ui.groups;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.function.Consumer;

/**
 * Manages the Create Project dialog for {@link com.student.overcooked.ui.fragments.GroupsFragment}.
 */
public class GroupCreateController {

    private final Fragment fragment;
    private final GroupRepository groupRepository;
    private final Consumer<Group> onGroupCreated;

    public GroupCreateController(@NonNull Fragment fragment,
                                 @NonNull GroupRepository groupRepository,
                                 @NonNull Consumer<Group> onGroupCreated) {
        this.fragment = fragment;
        this.groupRepository = groupRepository;
        this.onGroupCreated = onGroupCreated;
    }

    public void showCreateProjectDialog() {
        if (!fragment.isAdded()) {
            return;
        }
        View dialogView = LayoutInflater.from(fragment.requireContext()).inflate(R.layout.dialog_create_project, null);
        Dialog dialog = new Dialog(fragment.requireContext());
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        dialog.setContentView(dialogView);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        TextInputEditText nameInput = dialogView.findViewById(R.id.projectNameInput);
        MaterialAutoCompleteTextView subjectInput = dialogView.findViewById(R.id.subjectInput);
        TextInputEditText descInput = dialogView.findViewById(R.id.descriptionInput);
        RadioGroup projectTypeGroup = dialogView.findViewById(R.id.projectTypeGroup);
        TextInputLayout deadlineInputLayout = dialogView.findViewById(R.id.deadlineInputLayout);
        TextView deadlineValue = dialogView.findViewById(R.id.deadlineValue);
        View deadlineButton = dialogView.findViewById(R.id.btnPickDeadline);
        SwitchMaterial createWorkspaceSwitch = dialogView.findViewById(R.id.createWorkspaceSwitch);
        LinearProgressIndicator progressIndicator = dialogView.findViewById(R.id.createProjectProgress);
        View btnClose = dialogView.findViewById(R.id.btnClose);
        View btnCreate = dialogView.findViewById(R.id.btnCreate);

        if (createWorkspaceSwitch != null) {
            createWorkspaceSwitch.setChecked(true);
        }

        DateFormat dateFormatter = DateFormat.getDateInstance(DateFormat.MEDIUM);
        Calendar deadlineCalendar = Calendar.getInstance();
        final Date[] selectedDeadline = new Date[1];

        View.OnClickListener showDeadlinePicker = v -> {
            DatePickerDialog picker = new DatePickerDialog(fragment.requireContext(), (view, year, month, dayOfMonth) -> {
                deadlineCalendar.set(Calendar.YEAR, year);
                deadlineCalendar.set(Calendar.MONTH, month);
                deadlineCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                deadlineCalendar.set(Calendar.HOUR_OF_DAY, 23);
                deadlineCalendar.set(Calendar.MINUTE, 59);
                deadlineCalendar.set(Calendar.SECOND, 0);
                deadlineCalendar.set(Calendar.MILLISECOND, 0);
                selectedDeadline[0] = deadlineCalendar.getTime();
                deadlineValue.setText(dateFormatter.format(selectedDeadline[0]));
            }, deadlineCalendar.get(Calendar.YEAR), deadlineCalendar.get(Calendar.MONTH), deadlineCalendar.get(Calendar.DAY_OF_MONTH));
            picker.getDatePicker().setMinDate(System.currentTimeMillis());
            picker.show();
        };

        deadlineButton.setOnClickListener(showDeadlinePicker);
        if (deadlineInputLayout != null) {
            deadlineInputLayout.setEndIconOnClickListener(showDeadlinePicker);
        }

        btnClose.setOnClickListener(v -> dialog.dismiss());
        btnCreate.setOnClickListener(v -> attemptCreateProject(dialog, btnCreate, progressIndicator,
                nameInput, subjectInput, descInput, projectTypeGroup, selectedDeadline));
        dialog.show();
    }

    private void attemptCreateProject(@NonNull Dialog dialog,
                                      @NonNull View btnCreate,
                                      @Nullable LinearProgressIndicator progressIndicator,
                                      @Nullable TextInputEditText nameInput,
                                      @Nullable MaterialAutoCompleteTextView subjectInput,
                                      @Nullable TextInputEditText descInput,
                                      @Nullable RadioGroup projectTypeGroup,
                                      @NonNull Date[] selectedDeadline) {
        if (!fragment.isAdded()) {
            return;
        }
        String name = nameInput != null && nameInput.getText() != null
                ? nameInput.getText().toString().trim()
                : "";
        String subject = subjectInput != null && subjectInput.getText() != null
                ? subjectInput.getText().toString().trim()
                : "";
        String description = descInput != null && descInput.getText() != null
                ? descInput.getText().toString().trim()
                : "";
        boolean isIndividual = projectTypeGroup != null && projectTypeGroup.getCheckedRadioButtonId() == R.id.radioIndividual;
        Date deadline = selectedDeadline[0];

        if (name.isEmpty()) {
            Toast.makeText(fragment.requireContext(), "Project name is required", Toast.LENGTH_SHORT).show();
            return;
        }
        if (deadline == null) {
            Toast.makeText(fragment.requireContext(), fragment.getString(R.string.project_deadline_required), Toast.LENGTH_SHORT).show();
            return;
        }

        btnCreate.setEnabled(false);
        if (progressIndicator != null) {
            progressIndicator.setVisibility(View.VISIBLE);
        }
        Toast.makeText(fragment.requireContext(), "Creating project...", Toast.LENGTH_SHORT).show();

        groupRepository.createGroup(name, subject, description, isIndividual, deadline,
                group -> {
                    if (!fragment.isAdded()) {
                        return;
                    }
                    Toast.makeText(fragment.requireContext(), "Project created!", Toast.LENGTH_SHORT).show();
                    if (progressIndicator != null) {
                        progressIndicator.setVisibility(View.GONE);
                    }
                    dialog.dismiss();
                    onGroupCreated.accept(group);
                },
                e -> {
                    if (!fragment.isAdded()) {
                        return;
                    }
                    btnCreate.setEnabled(true);
                    if (progressIndicator != null) {
                        progressIndicator.setVisibility(View.GONE);
                    }
                    Toast.makeText(fragment.requireContext(), "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
