package com.student.overcooked.ui.focus;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import androidx.annotation.NonNull;

import java.util.Locale;

final class FocusSoundUtils {

    private FocusSoundUtils() {
    }

    static int resolveRawSoundResId(@NonNull Context context, @NonNull String presetName) {
        // Map UI name -> raw resource name.
        // Your files are: res/raw/work.mp3, master.mp3, slaves.mp3
        String rawName;
        String lower = presetName.toLowerCase(Locale.ROOT);
        if (lower.contains("work")) {
            rawName = "work";
        } else if (lower.contains("master")) {
            rawName = "master";
        } else if (lower.contains("slaves")) {
            rawName = "slaves";
        } else {
            rawName = lower.replace(" ", "_");
        }
        return context.getResources().getIdentifier(rawName, "raw", context.getPackageName());
    }

    @NonNull
    static String getDisplayNameForUri(@NonNull Context context, @NonNull Uri uri) {
        try {
            ContentResolver resolver = context.getContentResolver();
            try (Cursor cursor = resolver.query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIdx >= 0) {
                        String name = cursor.getString(nameIdx);
                        if (name != null && !name.trim().isEmpty()) return name;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return "Imported Sound";
    }
}
