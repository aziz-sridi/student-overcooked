package com.example.overcooked.ui.dialog;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.overcooked.R;
import com.example.overcooked.data.model.Task;

import java.util.Calendar;

/**
 * Dialog for adding or editing tasks
 */
public class AddEditTaskDialog extends DialogFragment {

    static final String ARG_MODE = "mode";
    static final String ARG_TASK_ID = "task_id";
    static final String ARG_TASK_TITLE = "task_title";
    static final String ARG_TASK_DESCRIPTION = "task_description";
    static final String ARG_TASK_COURSE = "task_course";
    static final String ARG_TASK_TYPE = "task_type";
    static final String ARG_TASK_DEADLINE = "task_deadline";
    static final String ARG_TASK_PRIORITY = "task_priority";
    static final String ARG_TASK_STATUS = "task_status";
    static final String ARG_TASK_COMPLETED = "task_completed";
    static final String ARG_TASK_PROJECT_ID = "task_project_id";

    public static final int MODE_ADD = 0;
    public static final int MODE_EDIT = 1;

    private AddEditTaskFormState formState;
    private AddEditTaskViewBinder viewBinder;
    private OnTaskSavedListener listener;

    public interface OnTaskSavedListener {
        void onTaskSaved(Task task, boolean isNew);
    }

    /**
     * Create dialog for adding a new task
     */
    public static AddEditTaskDialog newInstanceAdd() {
        AddEditTaskDialog dialog = new AddEditTaskDialog();
        Bundle args = new Bundle();
        args.putInt(ARG_MODE, MODE_ADD);
        dialog.setArguments(args);
        return dialog;
    }

    /**
     * Create dialog for editing an existing task
     */
    public static AddEditTaskDialog newInstanceEdit(Task task) {
        AddEditTaskDialog dialog = new AddEditTaskDialog();
        Bundle args = new Bundle();
        args.putInt(ARG_MODE, MODE_EDIT);
        args.putLong(ARG_TASK_ID, task.getId());
        args.putString(ARG_TASK_TITLE, task.getTitle());
        args.putString(ARG_TASK_DESCRIPTION, task.getDescription());
        args.putString(ARG_TASK_COURSE, task.getCourse());
        args.putString(ARG_TASK_TYPE, task.getTaskType() != null ? task.getTaskType().name() : "HOMEWORK");
        args.putLong(ARG_TASK_DEADLINE, task.getDeadline() != null ? task.getDeadline().getTime() : 0);
        args.putString(ARG_TASK_PRIORITY, task.getPriority() != null ? task.getPriority().name() : "MEDIUM");
        args.putString(ARG_TASK_STATUS, task.getStatus() != null ? task.getStatus().name() : "NOT_STARTED");
        args.putBoolean(ARG_TASK_COMPLETED, task.isCompleted());
        args.putLong(ARG_TASK_PROJECT_ID, task.getProjectId() != null ? task.getProjectId() : 0L);
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (getParentFragment() instanceof OnTaskSavedListener) {
            listener = (OnTaskSavedListener) getParentFragment();
        } else if (context instanceof OnTaskSavedListener) {
            listener = (OnTaskSavedListener) context;
        }
    }

    public void setOnTaskSavedListener(OnTaskSavedListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, com.google.android.material.R.style.Theme_MaterialComponents_Light_Dialog);

        formState = AddEditTaskFormState.fromArguments(getArguments());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_add_edit_task, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewBinder = AddEditTaskViewBinder.bind(view);
        setupCourseDropdown();
        setupDateTimePickers();
        setupButtons();
        formState.bindInitialState(viewBinder);
    }

    private void setupCourseDropdown() {
        String[] courses = {
                "Software Engineering",
                "Data Structures",
                "Web Development",
                "Mobile Development",
                "Database Systems",
                "Algorithms",
                "Computer Networks",
                "Operating Systems",
                "Machine Learning"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                courses
        );
        viewBinder.taskCourseInput.setAdapter(adapter);
    }

    private void setupDateTimePickers() {
        viewBinder.datePickerCard.setOnClickListener(v -> showDatePicker());
        viewBinder.timePickerCard.setOnClickListener(v -> showTimePicker());
    }

    private void showDatePicker() {
        DatePickerDialog dialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    formState.getSelectedDeadline().set(Calendar.YEAR, year);
                    formState.getSelectedDeadline().set(Calendar.MONTH, month);
                    formState.getSelectedDeadline().set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    formState.updateDateDisplay(viewBinder);
                },
                formState.getSelectedDeadline().get(Calendar.YEAR),
                formState.getSelectedDeadline().get(Calendar.MONTH),
                formState.getSelectedDeadline().get(Calendar.DAY_OF_MONTH)
        );
        dialog.show();
    }

    private void showTimePicker() {
        TimePickerDialog dialog = new TimePickerDialog(
                requireContext(),
                (view, hourOfDay, minute) -> {
                    formState.getSelectedDeadline().set(Calendar.HOUR_OF_DAY, hourOfDay);
                    formState.getSelectedDeadline().set(Calendar.MINUTE, minute);
                    formState.updateTimeDisplay(viewBinder);
                },
                formState.getSelectedDeadline().get(Calendar.HOUR_OF_DAY),
                formState.getSelectedDeadline().get(Calendar.MINUTE),
                false
        );
        dialog.show();
    }

    private void setupButtons() {
        viewBinder.closeButton.setOnClickListener(v -> dismiss());
        viewBinder.saveTaskButton.setOnClickListener(v -> saveTask());
    }

    private void saveTask() {
        AddEditTaskFormState.SaveResult result = formState.buildTask(viewBinder);
        if (!result.isSuccess()) {
            viewBinder.taskTitleInput.setError(result.errorMessage);
            viewBinder.taskTitleInput.requestFocus();
            return;
        }
        if (listener != null && result.task != null) {
            listener.onTaskSaved(result.task, result.isNew);
        }
        dismiss();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.bg_dialog);
        }
    }
}
