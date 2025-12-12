package com.student.overcooked.ui.workspace;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.text.TextUtils;

import androidx.annotation.NonNull;

final class WorkspaceFileUtils {

    private WorkspaceFileUtils() {
    }

    @NonNull
    static String extractDisplayName(@NonNull Context context, @NonNull Uri uri, @NonNull String untitledFallback) {
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

        return TextUtils.isEmpty(name) ? untitledFallback : name;
    }

    static long extractFileSize(@NonNull Context context, @NonNull Uri uri) {
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
}
