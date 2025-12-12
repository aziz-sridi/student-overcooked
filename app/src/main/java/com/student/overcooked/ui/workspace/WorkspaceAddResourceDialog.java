package com.student.overcooked.ui.workspace;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.student.overcooked.R;
import com.student.overcooked.data.model.ProjectResourceType;
import com.student.overcooked.data.repository.GroupRepository;

final class WorkspaceAddResourceDialog {

    private final Fragment fragment;
    private final GroupRepository groupRepository;
    private final String groupId;
    private final @Nullable ActivityResultLauncher<String[]> filePickerLauncher;

    private final WorkspaceResourceSaver saver;
    private WorkspaceFileSelection fileSelection;
    private WorkspaceFileSectionController fileSection;

    private @Nullable Dialog dialog;

    WorkspaceAddResourceDialog(@NonNull Fragment fragment,
                              @NonNull GroupRepository groupRepository,
                              @NonNull String groupId,
                              @Nullable ActivityResultLauncher<String[]> filePickerLauncher) {
        this.fragment = fragment;
        this.groupRepository = groupRepository;
        this.groupId = groupId;
        this.filePickerLauncher = filePickerLauncher;

        this.saver = new WorkspaceResourceSaver(fragment, groupRepository, groupId);
    }

    void show(@NonNull Runnable onDismiss) {
        if (!fragment.isAdded()) return;

        View dialogView = LayoutInflater.from(requireContext()).inflate(
                R.layout.dialog_add_project_resource,
                new FrameLayout(requireContext()),
                false
        );

        TextInputEditText titleInput = dialogView.findViewById(R.id.resourceTitleInput);
        TextInputLayout contentLayout = dialogView.findViewById(R.id.resourceContentLayout);
        TextInputEditText contentInput = dialogView.findViewById(R.id.resourceContentInput);
        MaterialAutoCompleteTextView typeDropdown = dialogView.findViewById(R.id.resourceTypeDropdown);

        View filePickerSection = dialogView.findViewById(R.id.filePickerSection);
        MaterialButton pickFileButton = dialogView.findViewById(R.id.resourcePickFileButton);
        TextView fileSummary = dialogView.findViewById(R.id.resourceFileSummary);
        View clearFile = dialogView.findViewById(R.id.resourceClearFileButton);
        android.widget.ImageView pickerIcon = dialogView.findViewById(R.id.filePickerIcon);

        MaterialButton saveButton = dialogView.findViewById(R.id.resourceSaveButton);

        fileSelection = new WorkspaceFileSelection();
        fileSection = new WorkspaceFileSectionController(fragment, fileSelection);
        fileSection.bindViews(fileSummary, clearFile, pickerIcon);

        pickFileButton.setOnClickListener(v -> openFilePicker());
        clearFile.setOnClickListener(v -> {
            if (fileSection != null) fileSection.clearSelection();
        });

        String[] labels = new String[]{
                requireContext().getString(R.string.resource_type_note),
                requireContext().getString(R.string.resource_type_link),
                requireContext().getString(R.string.resource_type_file)
        };
        ProjectResourceType[] types = new ProjectResourceType[]{
                ProjectResourceType.NOTE,
                ProjectResourceType.LINK,
                ProjectResourceType.FILE
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, labels);
        typeDropdown.setAdapter(adapter);
        typeDropdown.setText(labels[0], false);

        final ProjectResourceType[] selectedType = {ProjectResourceType.NOTE};
        typeDropdown.setOnItemClickListener((parent, view, position, id) -> {
            selectedType[0] = types[Math.max(0, Math.min(position, types.length - 1))];
            configureFieldsForType(selectedType[0], contentLayout, contentInput, filePickerSection);
        });
        configureFieldsForType(selectedType[0], contentLayout, contentInput, filePickerSection);

        Dialog localDialog = new Dialog(requireContext());
        localDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        localDialog.setContentView(dialogView);
        if (localDialog.getWindow() != null) {
            localDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        localDialog.setOnDismissListener(d -> {
            dialog = null;
            if (fileSection != null) {
                fileSection.unbind();
            }
            fileSection = null;
            fileSelection = null;
            onDismiss.run();
        });

        final CharSequence defaultButtonText = saveButton.getText();
        saveButton.setOnClickListener(v -> {
            contentLayout.setError(null);
            String title = titleInput != null && titleInput.getText() != null ? titleInput.getText().toString().trim() : "";
            String content = contentInput != null && contentInput.getText() != null ? contentInput.getText().toString().trim() : "";
            ProjectResourceType type = selectedType[0];

            if (type == ProjectResourceType.NOTE && TextUtils.isEmpty(content)) {
                contentLayout.setError(requireContext().getString(R.string.resource_note_required));
                return;
            }

            if (type == ProjectResourceType.LINK) {
                if (TextUtils.isEmpty(content)) {
                    contentLayout.setError(requireContext().getString(R.string.resource_link_required));
                    return;
                }
                String normalized = content.startsWith("http://") || content.startsWith("https://")
                        ? content
                        : "https://" + content;
                if (!Patterns.WEB_URL.matcher(normalized).matches()) {
                    contentLayout.setError(requireContext().getString(R.string.resource_link_required));
                    return;
                }
                content = normalized;
            }

            if (type == ProjectResourceType.FILE && !fileSelection.hasLocalFile()) {
                Toast.makeText(requireContext(), R.string.workspace_file_required, Toast.LENGTH_SHORT).show();
                return;
            }

            saveButton.setEnabled(false);
            if (fileSelection == null) return;
            saver.save(type, title, content, fileSelection, saveButton, localDialog, defaultButtonText);
        });

        this.dialog = localDialog;
        localDialog.show();
    }

    void handleFilePicked(@NonNull Uri uri) {
        if (fileSection != null) {
            fileSection.handleFilePicked(uri);
        }
    }

    private void configureFieldsForType(@NonNull ProjectResourceType type,
                                        @Nullable TextInputLayout contentLayout,
                                        @Nullable TextInputEditText contentInput,
                                        @Nullable View fileSection) {
        if (contentLayout == null || contentInput == null) return;

        contentLayout.setError(null);
        if (fileSection != null) {
            fileSection.setVisibility(type == ProjectResourceType.FILE ? View.VISIBLE : View.GONE);
        }

        if (type == ProjectResourceType.LINK) {
            contentLayout.setHint(requireContext().getString(R.string.resource_link_hint));
            contentInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
            contentInput.setSingleLine(true);
            contentInput.setMinLines(1);
        } else if (type == ProjectResourceType.FILE) {
            contentLayout.setHint(requireContext().getString(R.string.resource_file_note_hint));
            contentInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
            contentInput.setSingleLine(false);
            contentInput.setMinLines(2);
        } else {
            contentLayout.setHint(requireContext().getString(R.string.resource_note_hint));
            contentInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
            contentInput.setSingleLine(false);
            contentInput.setMinLines(3);
        }

        if (this.fileSection != null) {
            this.fileSection.onTypeChanged(type);
        }
    }

    private void openFilePicker() {
        if (filePickerLauncher != null) {
            filePickerLauncher.launch(new String[]{"*/*"});
        }
    }

    @NonNull
    private Context requireContext() {
        return fragment.requireContext();
    }
}
