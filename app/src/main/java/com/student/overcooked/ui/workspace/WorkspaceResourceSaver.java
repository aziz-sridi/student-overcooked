package com.student.overcooked.ui.workspace;

import android.app.Dialog;
import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.student.overcooked.R;
import com.student.overcooked.data.model.ProjectResourceType;
import com.student.overcooked.data.repository.GroupRepository;

final class WorkspaceResourceSaver {

    private final Fragment fragment;
    private final GroupRepository groupRepository;
    private final String groupId;

    WorkspaceResourceSaver(@NonNull Fragment fragment, @NonNull GroupRepository groupRepository, @NonNull String groupId) {
        this.fragment = fragment;
        this.groupRepository = groupRepository;
        this.groupId = groupId;
    }

    void save(@NonNull ProjectResourceType type,
              @NonNull String title,
              @NonNull String content,
              @NonNull WorkspaceFileSelection fileSelection,
              @NonNull MaterialButton saveButton,
              @NonNull Dialog dialog,
              @NonNull CharSequence defaultButtonText) {
        if (type == ProjectResourceType.FILE) {
            saveButton.setText(R.string.uploading_file);
            uploadFileThenSave(type, title, content, fileSelection, saveButton, dialog, defaultButtonText);
        } else {
            persistResource(type, title, content, fileSelection, saveButton, dialog, defaultButtonText);
        }
    }

    private void uploadFileThenSave(@NonNull ProjectResourceType type,
                                   @NonNull String title,
                                   @NonNull String content,
                                   @NonNull WorkspaceFileSelection fileSelection,
                                   @NonNull MaterialButton saveButton,
                                   @NonNull Dialog dialog,
                                   @NonNull CharSequence defaultButtonText) {
        Uri fileUri = fileSelection.getFileUri();
        if (fileUri == null) {
            handleUploadFailure(saveButton, defaultButtonText);
            return;
        }

        String existingName = fileSelection.getDisplayName();
        final String resolvedName = TextUtils.isEmpty(existingName)
                ? "workspace_file_" + System.currentTimeMillis()
                : existingName;

        WorkspaceResourceUploader.uploadToFirebase(groupId, fileUri, resolvedName, new WorkspaceResourceUploader.Callback() {
            @Override
            public void onSuccess(@NonNull Uri downloadUri, @NonNull String storagePath, long sizeBytes, @Nullable String mimeType) {
                long finalSize = fileSelection.getSizeBytes();
                if (finalSize <= 0) {
                    finalSize = Math.max(0L, sizeBytes);
                }
                String finalMime = fileSelection.getMimeType();
                if (TextUtils.isEmpty(finalMime)) {
                    finalMime = mimeType;
                }

                fileSelection.setDisplayName(resolvedName);
                fileSelection.setSizeBytes(Math.max(0L, finalSize));
                fileSelection.setMimeType(finalMime);
                fileSelection.setUploadMetadata(downloadUri.toString(), storagePath);

                persistResource(type, title, content, fileSelection, saveButton, dialog, defaultButtonText);
            }

            @Override
            public void onFailure(@NonNull Exception e) {
                handleUploadFailure(saveButton, defaultButtonText);
            }
        });
    }

    private void persistResource(@NonNull ProjectResourceType type,
                                 @NonNull String title,
                                 @NonNull String content,
                                 @NonNull WorkspaceFileSelection fileSelection,
                                 @NonNull MaterialButton saveButton,
                                 @NonNull Dialog dialog,
                                 @NonNull CharSequence defaultButtonText) {
        final String fileName = type == ProjectResourceType.FILE
                ? (TextUtils.isEmpty(fileSelection.getDisplayName())
                ? requireContext().getString(R.string.untitled_resource)
                : fileSelection.getDisplayName())
                : null;
        final String fileUrl = type == ProjectResourceType.FILE ? fileSelection.getDownloadUrl() : null;
        final long fileSize = type == ProjectResourceType.FILE ? fileSelection.getSizeBytes() : 0;
        final String fileMime = type == ProjectResourceType.FILE ? fileSelection.getMimeType() : null;
        final String storagePath = type == ProjectResourceType.FILE ? fileSelection.getStoragePath() : null;

        groupRepository.addProjectResource(
                groupId,
                type,
                title,
                content,
                fileName,
                fileUrl,
                fileSize,
                fileMime,
                storagePath,
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
                })
        );
    }

    private void handleUploadFailure(@NonNull MaterialButton saveButton, @NonNull CharSequence defaultButtonText) {
        if (!fragment.isAdded()) return;

        saveButton.setEnabled(true);
        saveButton.setText(defaultButtonText);
        runOnUiThread(() -> Toast.makeText(
                requireContext(),
                "File upload failed. Please ensure Firebase Storage is enabled in your project settings.",
                Toast.LENGTH_LONG
        ).show());
    }

    @NonNull
    private Context requireContext() {
        return fragment.requireContext();
    }

    private void runOnUiThread(@NonNull Runnable runnable) {
        if (!fragment.isAdded()) return;
        fragment.requireActivity().runOnUiThread(runnable);
    }
}
