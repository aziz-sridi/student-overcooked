package com.student.overcooked.ui.groupdetail;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.student.overcooked.data.model.GroupTask;
import com.student.overcooked.data.model.TaskStatus;
import com.student.overcooked.ui.dialog.GroupTaskDetailsDialog;

final class GroupTaskDetailsLauncher {

    interface FindTaskById {
        @Nullable GroupTask find(@NonNull String taskId);
    }

    interface StatusUpdater {
        void update(@NonNull GroupTask task, @NonNull TaskStatus status);
    }

    static void show(@NonNull Fragment fragment,
                     @NonNull GroupTask task,
                     @NonNull FindTaskById findTaskById,
                     @NonNull StatusUpdater statusUpdater) {
        GroupTaskDetailsDialog dialog = GroupTaskDetailsDialog.newInstance(
                task.getId(),
                task.getTitle() != null ? task.getTitle() : "",
                task.getDescription(),
                task.getDeadline(),
                task.getPriority(),
                task.getAssigneeName(),
                task.getStatus()
        );

        dialog.setOnGroupTaskActionListener((taskId, newStatus) -> {
            GroupTask target = findTaskById.find(taskId);
            if (target != null) {
                statusUpdater.update(target, newStatus);
            }
        });

        dialog.show(fragment.getChildFragmentManager(), "GroupTaskDetailsDialog");
    }

    private GroupTaskDetailsLauncher() {
    }
}
