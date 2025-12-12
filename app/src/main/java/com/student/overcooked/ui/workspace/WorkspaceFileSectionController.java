package com.student.overcooked.ui.workspace;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.student.overcooked.R;
import com.student.overcooked.data.model.ProjectResourceType;

final class WorkspaceFileSectionController {

    private final Fragment fragment;
    private final WorkspaceFileSelection fileSelection;

    private @Nullable TextView fileSummaryView;
    private @Nullable View clearFileButton;
    private @Nullable ImageView filePickerIcon;

    WorkspaceFileSectionController(@NonNull Fragment fragment, @NonNull WorkspaceFileSelection fileSelection) {
        this.fragment = fragment;
        this.fileSelection = fileSelection;
    }

    void bindViews(@NonNull TextView fileSummary, @NonNull View clearFile, @NonNull ImageView pickerIcon) {
        this.fileSummaryView = fileSummary;
        this.clearFileButton = clearFile;
        this.filePickerIcon = pickerIcon;
        updateViews();
    }

    void clearSelection() {
        fileSelection.clear();
        updateViews();
    }

    void onTypeChanged(@NonNull ProjectResourceType type) {
        if (type != ProjectResourceType.FILE) {
            clearSelection();
        }
    }

    void handleFilePicked(@NonNull Uri uri) {
        if (!fragment.isAdded()) return;

        Context context = requireContext();
        String displayName = WorkspaceFileUtils.extractDisplayName(
                context,
                uri,
                requireContext().getString(R.string.untitled_resource)
        );
        long sizeBytes = WorkspaceFileUtils.extractFileSize(context, uri);
        String mimeType = context.getContentResolver().getType(uri);

        try {
            context.getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (SecurityException ignored) {
        }

        fileSelection.applyLocalSelection(uri, displayName, sizeBytes, mimeType);
        updateViews();
    }

    void unbind() {
        fileSummaryView = null;
        clearFileButton = null;
        filePickerIcon = null;
    }

    private void updateViews() {
        if (fileSummaryView == null || !fragment.isAdded()) return;

        if (fileSelection.hasLocalFile()) {
            String displayName = fileSelection.getDisplayName();
            if (TextUtils.isEmpty(displayName)) {
                displayName = requireContext().getString(R.string.untitled_resource);
            }
            long size = Math.max(0L, fileSelection.getSizeBytes());
            String sizeLabel = Formatter.formatShortFileSize(requireContext(), size);

            fileSummaryView.setText(displayName + "\n" + sizeLabel);
            fileSummaryView.setTextColor(ContextCompat.getColor(requireContext(), R.color.textPrimary));

            if (clearFileButton != null) {
                clearFileButton.setVisibility(View.VISIBLE);
            }
            if (filePickerIcon != null) {
                filePickerIcon.setImageResource(R.drawable.ic_check);
                filePickerIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.successGreen));
            }
        } else {
            fileSummaryView.setText(R.string.workspace_file_not_selected);
            fileSummaryView.setTextColor(ContextCompat.getColor(requireContext(), R.color.textSecondary));

            if (clearFileButton != null) {
                clearFileButton.setVisibility(View.GONE);
            }
            if (filePickerIcon != null) {
                filePickerIcon.setImageResource(R.drawable.ic_resource_file);
                filePickerIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.textSecondary));
            }
        }
    }

    @NonNull
    private Context requireContext() {
        return fragment.requireContext();
    }
}
