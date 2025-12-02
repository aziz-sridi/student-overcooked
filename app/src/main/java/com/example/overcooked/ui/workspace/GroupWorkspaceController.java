package com.example.overcooked.ui.workspace;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.text.InputType;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.overcooked.R;
import com.example.overcooked.data.model.ProjectResource;
import com.example.overcooked.data.model.ProjectResourceType;
import com.example.overcooked.data.repository.GroupRepository;
import com.example.overcooked.ui.adapter.ProjectResourceAdapter;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.List;
import java.util.UUID;

/**
 * Encapsulates workspace-specific UI and logic to keep {@code GroupDetailFragment} smaller.
 */
public class GroupWorkspaceController {

    private final Fragment fragment;
    private final GroupRepository groupRepository;
    private final String groupId;
    private final RecyclerView workspaceRecycler;
    private final View workspaceEmptyState;
    private final View addWorkspaceButton;
    private final ProjectResourceAdapter resourceAdapter;
    private final WorkspaceFileSelection workspaceFileSelection = new WorkspaceFileSelection();

    private ActivityResultLauncher<String[]> filePickerLauncher;
    private TextView workspaceFileSummaryView;
    private View workspaceFileClearButton;
    private boolean isIndividualProject;

    public GroupWorkspaceController(@NonNull Fragment fragment,
                                    @NonNull GroupRepository groupRepository,
                                    @NonNull String groupId,
                                    @Nullable RecyclerView workspaceRecycler,
                                    @Nullable View workspaceEmptyState,
                                    @Nullable View addWorkspaceButton) {
        this.fragment = fragment;
        this.groupRepository = groupRepository;
        this.groupId = groupId;
        this.workspaceRecycler = workspaceRecycler;
        this.workspaceEmptyState = workspaceEmptyState;
        this.addWorkspaceButton = addWorkspaceButton;

        Context context = fragment.requireContext();
        resourceAdapter = new ProjectResourceAdapter(new ProjectResourceAdapter.ResourceActionListener() {
            @Override
            public void onResourceClick(ProjectResource resource) {
                handleResourceClick(resource);
            }

            @Override
            public void onResourceDelete(ProjectResource resource) {
                confirmDeleteResource(resource);
            }
        });

        if (this.workspaceRecycler != null) {
            this.workspaceRecycler.setLayoutManager(new LinearLayoutManager(context));
            this.workspaceRecycler.setAdapter(resourceAdapter);
        }
        if (this.addWorkspaceButton != null) {
            this.addWorkspaceButton.setOnClickListener(v -> showAddWorkspaceItemDialog());
        }
    }

    public void setFilePickerLauncher(@Nullable ActivityResultLauncher<String[]> launcher) {
        this.filePickerLauncher = launcher;
    }

    public void setIndividualProject(boolean individualProject) {
        this.isIndividualProject = individualProject;
    }

    public void submitResources(@Nullable List<ProjectResource> resources) {
        resourceAdapter.submitList(resources);
        boolean hasItems = resources != null && !resources.isEmpty();
        if (workspaceRecycler != null) {
            workspaceRecycler.setVisibility(hasItems ? View.VISIBLE : View.GONE);
        }
        if (workspaceEmptyState != null) {
            workspaceEmptyState.setVisibility(hasItems ? View.GONE : View.VISIBLE);
        }
    }

    public void handleFilePicked(@NonNull Uri uri) {
        if (!fragment.isAdded()) {
            return;
        }
        Context context = fragment.requireContext();
        String displayName = extractDisplayName(context, uri);
        long sizeBytes = extractFileSize(context, uri);
        String mimeType = context.getContentResolver().getType(uri);
        try {
            context.getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (SecurityException ignored) {}
        workspaceFileSelection.applyLocalSelection(uri, displayName, sizeBytes, mimeType);
        updateWorkspaceFileSummaryViews();
    }

    public void configureFab(@NonNull FloatingActionButton fab) {
        fab.setVisibility(View.VISIBLE);
        fab.setImageResource(R.drawable.ic_add);
        fab.setOnClickListener(v -> showAddWorkspaceItemDialog());
    }

    public ProjectResourceAdapter getAdapter() {
        return resourceAdapter;
    }

    private void showAddWorkspaceItemDialog() {
        if (!isIndividualProject) {
            Toast.makeText(requireContext(), R.string.error_occurred, Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_project_resource, null);
        TextInputEditText titleInput = dialogView.findViewById(R.id.resourceTitleInput);
        TextInputLayout contentLayout = dialogView.findViewById(R.id.resourceContentLayout);
        TextInputEditText contentInput = dialogView.findViewById(R.id.resourceContentInput);
        MaterialAutoCompleteTextView typeDropdown = dialogView.findViewById(R.id.resourceTypeDropdown);
        View filePickerSection = dialogView.findViewById(R.id.filePickerSection);
        MaterialButton pickFileButton = dialogView.findViewById(R.id.resourcePickFileButton);
        TextView fileSummary = dialogView.findViewById(R.id.resourceFileSummary);
        TextView clearFileButton = dialogView.findViewById(R.id.resourceClearFileButton);
        MaterialButton saveButton = dialogView.findViewById(R.id.resourceSaveButton);

        workspaceFileSelection.clear();
        workspaceFileSummaryView = fileSummary;
        workspaceFileClearButton = clearFileButton;
        updateWorkspaceFileSummaryViews();

        pickFileButton.setOnClickListener(v -> openWorkspaceFilePicker());
        clearFileButton.setOnClickListener(v -> {
            workspaceFileSelection.clear();
            updateWorkspaceFileSummaryViews();
        });

        String[] labels = new String[] {
                requireContext().getString(R.string.resource_type_note),
                requireContext().getString(R.string.resource_type_link),
                requireContext().getString(R.string.resource_type_file)
        };
        ProjectResourceType[] types = new ProjectResourceType[] {
                ProjectResourceType.NOTE,
                ProjectResourceType.LINK,
                ProjectResourceType.FILE
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, labels);
        typeDropdown.setAdapter(adapter);
        typeDropdown.setText(labels[0], false);
        final ProjectResourceType[] selectedType = {ProjectResourceType.NOTE};
        typeDropdown.setOnItemClickListener((parent, view1, position, id) -> {
            selectedType[0] = types[position];
            configureWorkspaceFieldsForType(selectedType[0], contentLayout, contentInput, filePickerSection);
        });
        configureWorkspaceFieldsForType(selectedType[0], contentLayout, contentInput, filePickerSection);

        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(dialogView);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        dialog.setOnDismissListener(d -> {
            workspaceFileSelection.clear();
            workspaceFileSummaryView = null;
            workspaceFileClearButton = null;
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

            if (type == ProjectResourceType.FILE && !workspaceFileSelection.hasLocalFile()) {
                Toast.makeText(requireContext(), R.string.workspace_file_required, Toast.LENGTH_SHORT).show();
                return;
            }

            saveButton.setEnabled(false);
            if (type == ProjectResourceType.FILE) {
                saveButton.setText(R.string.uploading_file);
                uploadWorkspaceFileThenSave(type, title, content, saveButton, dialog, defaultButtonText);
            } else {
                persistWorkspaceResource(type, title, content, saveButton, dialog, defaultButtonText);
            }
        });

        dialog.show();
    }

    private void configureWorkspaceFieldsForType(@NonNull ProjectResourceType type,
                                                 @Nullable TextInputLayout contentLayout,
                                                 @Nullable TextInputEditText contentInput,
                                                 @Nullable View fileSection) {
        if (contentLayout == null || contentInput == null) {
            return;
        }
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

        if (type != ProjectResourceType.FILE) {
            workspaceFileSelection.clear();
            updateWorkspaceFileSummaryViews();
        }
    }

    private void openWorkspaceFilePicker() {
        if (filePickerLauncher != null) {
            filePickerLauncher.launch(new String[]{"*/*"});
        }
    }

    private void uploadWorkspaceFileThenSave(ProjectResourceType type, String title, String content,
                                             MaterialButton saveButton, Dialog dialog,
                                             CharSequence defaultButtonText) {
        Uri fileUri = workspaceFileSelection.getFileUri();
        if (fileUri == null) {
            handleWorkspaceUploadFailure(saveButton, defaultButtonText);
            return;
        }

        String existingName = workspaceFileSelection.getDisplayName();
        final String resolvedName = TextUtils.isEmpty(existingName)
                ? "workspace_file_" + System.currentTimeMillis()
                : existingName;
        String safeName = resolvedName.replaceAll("[^a-zA-Z0-9._-]", "_");
        String storagePath = "workspace/" + groupId + "/" + UUID.randomUUID() + "_" + safeName;
        StorageReference storageReference = FirebaseStorage.getInstance().getReference().child(storagePath);
        storageReference.putFile(fileUri)
                .addOnSuccessListener(taskSnapshot -> storageReference.getDownloadUrl()
                        .addOnSuccessListener(downloadUri -> {
                            long size = workspaceFileSelection.getSizeBytes();
                            if (size <= 0 && taskSnapshot.getMetadata() != null) {
                                size = taskSnapshot.getMetadata().getSizeBytes();
                            }
                            String mimeType = workspaceFileSelection.getMimeType();
                            if (TextUtils.isEmpty(mimeType) && taskSnapshot.getMetadata() != null) {
                                mimeType = taskSnapshot.getMetadata().getContentType();
                            }
                            workspaceFileSelection.setDisplayName(resolvedName);
                            workspaceFileSelection.setSizeBytes(Math.max(0L, size));
                            workspaceFileSelection.setMimeType(mimeType);
                            workspaceFileSelection.setUploadMetadata(downloadUri.toString(), storagePath);
                            persistWorkspaceResource(type, title, content, saveButton, dialog, defaultButtonText);
                        })
                        .addOnFailureListener(e -> handleWorkspaceUploadFailure(saveButton, defaultButtonText)))
                .addOnFailureListener(e -> handleWorkspaceUploadFailure(saveButton, defaultButtonText));
    }

    private void persistWorkspaceResource(ProjectResourceType type, String title, String content,
                                          MaterialButton saveButton, Dialog dialog,
                                          CharSequence defaultButtonText) {
        final String fileName = type == ProjectResourceType.FILE
                ? (TextUtils.isEmpty(workspaceFileSelection.getDisplayName())
                ? requireContext().getString(R.string.untitled_resource)
                : workspaceFileSelection.getDisplayName())
                : null;
        final String fileUrl = type == ProjectResourceType.FILE ? workspaceFileSelection.getDownloadUrl() : null;
        final long fileSize = type == ProjectResourceType.FILE ? workspaceFileSelection.getSizeBytes() : 0;
        final String fileMime = type == ProjectResourceType.FILE ? workspaceFileSelection.getMimeType() : null;
        final String storagePath = type == ProjectResourceType.FILE ? workspaceFileSelection.getStoragePath() : null;

        groupRepository.addProjectResource(groupId, type, title, content,
                fileName, fileUrl, fileSize, fileMime, storagePath,
                resource -> runOnUiThread(() -> {
                    Toast.makeText(requireContext(), R.string.workspace_item_saved, Toast.LENGTH_SHORT).show();
                    saveButton.setEnabled(true);
                    saveButton.setText(defaultButtonText);
                    dialog.dismiss();
                }),
                e -> runOnUiThread(() -> {
                    saveButton.setEnabled(true);
                    saveButton.setText(defaultButtonText);
                    Toast.makeText(requireContext(), R.string.error_occurred, Toast.LENGTH_SHORT).show();
                }));
    }

    private void handleWorkspaceUploadFailure(MaterialButton saveButton, CharSequence defaultButtonText) {
        if (!fragment.isAdded()) {
            return;
        }
        saveButton.setEnabled(true);
        saveButton.setText(defaultButtonText);
        Toast.makeText(requireContext(), R.string.error_occurred, Toast.LENGTH_SHORT).show();
    }

    private void handleResourceClick(@Nullable ProjectResource resource) {
        if (resource == null) {
            return;
        }
        ProjectResourceType type = resource.getType() != null ? resource.getType() : ProjectResourceType.NOTE;
        if (type == ProjectResourceType.LINK) {
            openResourceLink(resource);
        } else {
            showResourceDetailsDialog(resource);
        }
    }

    private void showResourceDetailsDialog(@NonNull ProjectResource resource) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(resource.getTitle().isEmpty() ? requireContext().getString(R.string.untitled_resource) : resource.getTitle())
                .setMessage(resource.getContent())
                .setPositiveButton(R.string.close, null)
                .setNegativeButton(R.string.copy, (dialog, which) ->
                        copyToClipboard("resource", resource.getContent()))
                .show();
    }

    private void openResourceLink(ProjectResource resource) {
        try {
            String content = resource.getContent();
            Uri uri = Uri.parse(content);
            if (uri.getScheme() == null) {
                uri = Uri.parse("https://" + content);
            }
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            fragment.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(requireContext(), R.string.resource_open_error, Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmDeleteResource(@Nullable ProjectResource resource) {
        if (resource == null) {
            return;
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.delete)
                .setMessage(R.string.resource_delete_confirm)
                .setPositiveButton(R.string.delete, (dialog, which) ->
                        groupRepository.deleteProjectResource(resource,
                                aVoid -> runOnUiThread(() ->
                                        Toast.makeText(requireContext(), R.string.workspace_item_deleted, Toast.LENGTH_SHORT).show()),
                                e -> runOnUiThread(() ->
                                        Toast.makeText(requireContext(), R.string.error_occurred, Toast.LENGTH_SHORT).show())))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void copyToClipboard(String label, String content) {
        ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(label, content);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(requireContext(), R.string.resource_copy, Toast.LENGTH_SHORT).show();
    }

    private void updateWorkspaceFileSummaryViews() {
        if (workspaceFileSummaryView == null || !fragment.isAdded()) {
            return;
        }
        if (workspaceFileSelection.hasLocalFile()) {
            String displayName = workspaceFileSelection.getDisplayName();
            if (TextUtils.isEmpty(displayName)) {
                displayName = requireContext().getString(R.string.untitled_resource);
            }
            long size = Math.max(0L, workspaceFileSelection.getSizeBytes());
            String sizeLabel = Formatter.formatShortFileSize(requireContext(), size);
            workspaceFileSummaryView.setText(requireContext().getString(R.string.workspace_file_selected_template, displayName, sizeLabel));
            if (workspaceFileClearButton != null) {
                workspaceFileClearButton.setVisibility(View.VISIBLE);
            }
        } else {
            workspaceFileSummaryView.setText(R.string.workspace_file_not_selected);
            if (workspaceFileClearButton != null) {
                workspaceFileClearButton.setVisibility(View.GONE);
            }
        }
    }

    private String extractDisplayName(Context context, Uri uri) {
        String name = null;
        try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (index >= 0) {
                    name = cursor.getString(index);
                }
            }
        }
        if (TextUtils.isEmpty(name)) {
            name = uri.getLastPathSegment();
        }
        return TextUtils.isEmpty(name) ? requireContext().getString(R.string.untitled_resource) : name;
    }

    private long extractFileSize(Context context, Uri uri) {
        long size = -1L;
        try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(OpenableColumns.SIZE);
                if (index >= 0) {
                    size = cursor.getLong(index);
                }
            }
        }
        return size;
    }

    private Context requireContext() {
        return fragment.requireContext();
    }

    private void runOnUiThread(@NonNull Runnable runnable) {
        if (!fragment.isAdded()) {
            return;
        }
        fragment.requireActivity().runOnUiThread(runnable);
    }

    private static class WorkspaceFileSelection {
        private Uri fileUri;
        private String displayName;
        private long sizeBytes = -1L;
        private String mimeType;
        private String downloadUrl;
        private String storagePath;

        void applyLocalSelection(@NonNull Uri uri, @Nullable String name, long size, @Nullable String mime) {
            this.fileUri = uri;
            this.displayName = name;
            this.sizeBytes = size;
            this.mimeType = mime;
            this.downloadUrl = null;
            this.storagePath = null;
        }

        void clear() {
            fileUri = null;
            displayName = null;
            sizeBytes = -1L;
            mimeType = null;
            downloadUrl = null;
            storagePath = null;
        }

        boolean hasLocalFile() {
            return fileUri != null;
        }

        Uri getFileUri() {
            return fileUri;
        }

        String getDisplayName() {
            return displayName;
        }

        long getSizeBytes() {
            return sizeBytes;
        }

        String getMimeType() {
            return mimeType;
        }

        String getDownloadUrl() {
            return downloadUrl;
        }

        String getStoragePath() {
            return storagePath;
        }

        void setDisplayName(String name) {
            this.displayName = name;
        }

        void setSizeBytes(long size) {
            this.sizeBytes = size;
        }

        void setMimeType(String mime) {
            this.mimeType = mime;
        }

        void setUploadMetadata(String url, String path) {
            this.downloadUrl = url;
            this.storagePath = path;
        }
    }
}
