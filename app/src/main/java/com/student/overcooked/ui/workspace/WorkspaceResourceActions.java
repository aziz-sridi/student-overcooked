package com.student.overcooked.ui.workspace;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.student.overcooked.R;
import com.student.overcooked.data.model.ProjectResource;
import com.student.overcooked.data.model.ProjectResourceType;
import com.student.overcooked.data.repository.GroupRepository;

final class WorkspaceResourceActions {

    private final Fragment fragment;
    private final GroupRepository groupRepository;

    WorkspaceResourceActions(@NonNull Fragment fragment, @NonNull GroupRepository groupRepository) {
        this.fragment = fragment;
        this.groupRepository = groupRepository;
    }

    void handleResourceClick(@Nullable ProjectResource resource) {
        if (resource == null || !fragment.isAdded()) return;

        ProjectResourceType type = resource.getType() != null ? resource.getType() : ProjectResourceType.NOTE;
        if (type == ProjectResourceType.LINK) {
            openResourceLink(resource);
        } else if (type == ProjectResourceType.FILE) {
            openResourceFile(resource);
        } else {
            showResourceDetailsDialog(resource);
        }
    }

    void confirmDeleteResource(@Nullable ProjectResource resource) {
        if (resource == null || !fragment.isAdded()) return;

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.delete)
                .setMessage(R.string.resource_delete_confirm)
                .setPositiveButton(R.string.delete, (dialog, which) ->
                        groupRepository.deleteProjectResource(
                                resource,
                                aVoid -> runOnUiThread(() -> Toast.makeText(requireContext(), R.string.workspace_item_deleted, Toast.LENGTH_SHORT).show()),
                                e -> runOnUiThread(() -> Toast.makeText(requireContext(), R.string.error_occurred, Toast.LENGTH_SHORT).show())
                        ))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void openResourceFile(@NonNull ProjectResource resource) {
        String fileUrl = resource.getFileUrl();
        if (TextUtils.isEmpty(fileUrl)) {
            Toast.makeText(requireContext(), R.string.resource_open_error, Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(fileUrl));
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            fragment.startActivity(intent);
        } catch (Exception e) {
            // If no app can handle it, try opening in browser
            try {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(fileUrl));
                fragment.startActivity(browserIntent);
            } catch (Exception e2) {
                Toast.makeText(requireContext(), R.string.resource_open_error, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showResourceDetailsDialog(@NonNull ProjectResource resource) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(resource.getTitle().isEmpty() ? requireContext().getString(R.string.untitled_resource) : resource.getTitle())
                .setMessage(resource.getContent())
                .setPositiveButton(R.string.close, null)
                .setNegativeButton(R.string.copy, (dialog, which) -> copyToClipboard("resource", resource.getContent()))
                .show();
    }

    private void openResourceLink(@NonNull ProjectResource resource) {
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

    private void copyToClipboard(@NonNull String label, @NonNull String content) {
        ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(label, content);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(requireContext(), R.string.resource_copy, Toast.LENGTH_SHORT).show();
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
