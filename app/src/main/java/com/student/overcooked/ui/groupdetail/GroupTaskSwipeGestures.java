package com.student.overcooked.ui.groupdetail;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.student.overcooked.data.model.GroupTask;
import com.student.overcooked.ui.adapter.GroupTaskAdapter;

final class GroupTaskSwipeGestures {

    interface ToggleHandler {
        void toggle(@NonNull GroupTask task);
    }

    interface DeleteHandler {
        void delete(@NonNull GroupTask task);
    }

    static void attach(@NonNull RecyclerView recyclerView,
                       @NonNull GroupTaskAdapter adapter,
                       @NonNull ToggleHandler toggleHandler,
                       @NonNull DeleteHandler deleteHandler) {
        ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(0,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                if (position == RecyclerView.NO_POSITION || position >= adapter.getCurrentList().size()) {
                    return;
                }
                GroupTask task = adapter.getCurrentList().get(position);
                if (task == null) {
                    recyclerView.post(() -> adapter.notifyItemChanged(position));
                    return;
                }
                if (direction == ItemTouchHelper.RIGHT) {
                    toggleHandler.toggle(task);
                    recyclerView.post(() -> adapter.notifyItemChanged(position));
                } else if (direction == ItemTouchHelper.LEFT) {
                    recyclerView.post(() -> adapter.notifyItemChanged(position));
                    deleteHandler.delete(task);
                }
            }
        };
        new ItemTouchHelper(callback).attachToRecyclerView(recyclerView);
    }

    private GroupTaskSwipeGestures() {
    }
}
