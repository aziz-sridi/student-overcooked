package com.student.overcooked.ui.groupdetail;

import android.view.View;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.student.overcooked.R;
import com.student.overcooked.data.model.GroupTask;
import com.student.overcooked.data.model.TaskStatus;

final class GroupTaskActionsMenu {

    interface TaskHandler {
        void handle(@NonNull GroupTask task);
    }

    interface TaskStatusHandler {
        void handle(@NonNull GroupTask task, @NonNull TaskStatus status);
    }

    static void show(@NonNull Fragment fragment,
                     @NonNull View anchor,
                     @NonNull GroupTask task,
                     @NonNull TaskHandler viewHandler,
                     @NonNull TaskStatusHandler statusHandler,
                     @NonNull TaskHandler toggleHandler,
                     @NonNull TaskHandler editHandler,
                     @NonNull TaskHandler deleteHandler) {
        PopupMenu popupMenu = new PopupMenu(fragment.requireContext(), anchor);
        popupMenu.inflate(R.menu.menu_group_task_actions);
        if (task.isCompleted()) {
            popupMenu.getMenu().findItem(R.id.action_toggle_complete)
                    .setTitle(R.string.group_task_mark_incomplete);
        } else {
            popupMenu.getMenu().findItem(R.id.action_toggle_complete)
                    .setTitle(R.string.group_task_mark_complete);
        }
        popupMenu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.action_view) {
                viewHandler.handle(task);
                return true;
            } else if (itemId == R.id.action_set_not_started) {
                statusHandler.handle(task, TaskStatus.NOT_STARTED);
                return true;
            } else if (itemId == R.id.action_set_in_progress) {
                statusHandler.handle(task, TaskStatus.IN_PROGRESS);
                return true;
            } else if (itemId == R.id.action_set_done) {
                statusHandler.handle(task, TaskStatus.DONE);
                return true;
            } else if (itemId == R.id.action_toggle_complete) {
                toggleHandler.handle(task);
                return true;
            } else if (itemId == R.id.action_edit) {
                editHandler.handle(task);
                return true;
            } else if (itemId == R.id.action_delete) {
                deleteHandler.handle(task);
                return true;
            }
            return false;
        });
        popupMenu.show();
    }

    private GroupTaskActionsMenu() {
    }
}
