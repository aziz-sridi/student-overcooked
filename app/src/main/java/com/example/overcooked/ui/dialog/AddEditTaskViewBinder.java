package com.example.overcooked.ui.dialog;

import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.example.overcooked.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

/**
 * Utility class that binds and exposes Add/Edit Task dialog views.
 */
public class AddEditTaskViewBinder {

    public final TextView dialogTitle;
    public final TextInputEditText taskTitleInput;
    public final TextInputEditText taskDescriptionInput;
    public final AutoCompleteTextView taskCourseInput;
    public final ChipGroup taskTypeChipGroup;
    public final ChipGroup priorityChipGroup;
    public final TextView selectedDateText;
    public final TextView selectedTimeText;
    public final MaterialCardView datePickerCard;
    public final MaterialCardView timePickerCard;
    public final MaterialButton saveTaskButton;
    public final ImageButton closeButton;

    private AddEditTaskViewBinder(@NonNull View root) {
        dialogTitle = root.findViewById(R.id.dialogTitle);
        taskTitleInput = root.findViewById(R.id.taskTitleInput);
        taskDescriptionInput = root.findViewById(R.id.taskDescriptionInput);
        taskCourseInput = root.findViewById(R.id.taskCourseInput);
        taskTypeChipGroup = root.findViewById(R.id.taskTypeChipGroup);
        priorityChipGroup = root.findViewById(R.id.priorityChipGroup);
        selectedDateText = root.findViewById(R.id.selectedDateText);
        selectedTimeText = root.findViewById(R.id.selectedTimeText);
        datePickerCard = root.findViewById(R.id.datePickerCard);
        timePickerCard = root.findViewById(R.id.timePickerCard);
        saveTaskButton = root.findViewById(R.id.saveTaskButton);
        closeButton = root.findViewById(R.id.btnClose);
    }

    public static AddEditTaskViewBinder bind(@NonNull View root) {
        return new AddEditTaskViewBinder(root);
    }

    public void applyModeText(int mode) {
        if (mode == AddEditTaskDialog.MODE_EDIT) {
            dialogTitle.setText(R.string.edit_task_title);
            saveTaskButton.setText(R.string.save_changes);
        } else {
            dialogTitle.setText(R.string.add_task_title);
            saveTaskButton.setText(R.string.save_task_button);
        }
    }
}
