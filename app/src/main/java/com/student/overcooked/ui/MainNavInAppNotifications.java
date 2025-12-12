package com.student.overcooked.ui;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;

import com.student.overcooked.data.model.Group;
import com.student.overcooked.data.model.GroupMessage;
import com.student.overcooked.data.model.GroupTask;
import com.student.overcooked.data.repository.GroupRepository;
import com.student.overcooked.util.NotificationHelper;
import com.student.overcooked.util.NotificationSettings;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class MainNavInAppNotifications {

    private final LifecycleOwner owner;
    private final GroupRepository groupRepository;
    private final NotificationHelper notificationHelper;

    private final Map<String, Boolean> initializedMessages = new HashMap<>();
    private final Map<String, Date> lastMessageTimestamp = new HashMap<>();
    private final Map<String, Boolean> initializedGroupTasks = new HashMap<>();
    private final Map<String, Date> lastGroupTaskCreatedAt = new HashMap<>();

    private final Map<String, Boolean> attachedMessageObservers = new HashMap<>();
    private final Map<String, Boolean> attachedGroupTaskObservers = new HashMap<>();

    MainNavInAppNotifications(@NonNull LifecycleOwner owner,
                             @NonNull GroupRepository groupRepository,
                             @NonNull NotificationHelper notificationHelper) {
        this.owner = owner;
        this.groupRepository = groupRepository;
        this.notificationHelper = notificationHelper;
    }

    void setupIfEnabled(@NonNull android.content.Context context) {
        if (!NotificationSettings.areNotificationsEnabled(context)) {
            return;
        }

        groupRepository.getUserGroups().observe(owner, groups -> {
            if (groups == null) return;
            for (Group group : groups) {
                if (group == null || group.getId() == null) continue;
                attachGroupMessageObserver(group);
                attachGroupTaskObserver(group);
            }
        });
    }

    private void attachGroupMessageObserver(@NonNull Group group) {
        final String groupId = group.getId();
        if (Boolean.TRUE.equals(attachedMessageObservers.get(groupId))) {
            return;
        }
        attachedMessageObservers.put(groupId, true);
        groupRepository.getGroupMessages(groupId).observe(owner, messages -> {
            if (messages == null || messages.isEmpty()) return;

            GroupMessage newest = newestMessage(messages);
            if (newest == null) return;

            if (!initializedMessages.containsKey(groupId)) {
                initializedMessages.put(groupId, true);
                lastMessageTimestamp.put(groupId, newest.getTimestamp());
                return;
            }

            Date lastSeen = lastMessageTimestamp.get(groupId);
            Date current = newest.getTimestamp();
            if (current == null) return;
            if (lastSeen != null && !current.after(lastSeen)) return;

            String myUid = FirebaseAuth.getInstance().getCurrentUser() != null
                    ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                    : null;
            if (myUid != null && myUid.equals(newest.getSenderId())) {
                lastMessageTimestamp.put(groupId, current);
                return;
            }

            notificationHelper.showMessageNotification(
                    groupId,
                    group.getName() != null ? group.getName() : "Group",
                    newest.getSenderName() != null ? newest.getSenderName() : "Someone",
                    newest.getMessage() != null ? newest.getMessage() : ""
            );
            lastMessageTimestamp.put(groupId, current);
        });
    }

    private void attachGroupTaskObserver(@NonNull Group group) {
        final String groupId = group.getId();
        if (Boolean.TRUE.equals(attachedGroupTaskObservers.get(groupId))) {
            return;
        }
        attachedGroupTaskObservers.put(groupId, true);
        groupRepository.getGroupTasks(groupId).observe(owner, tasks -> {
            if (tasks == null || tasks.isEmpty()) return;

            GroupTask newest = newestGroupTask(tasks);
            if (newest == null) return;

            if (!initializedGroupTasks.containsKey(groupId)) {
                initializedGroupTasks.put(groupId, true);
                lastGroupTaskCreatedAt.put(groupId, newest.getCreatedAt());
                return;
            }

            Date lastSeen = lastGroupTaskCreatedAt.get(groupId);
            Date current = newest.getCreatedAt();
            if (current == null) return;
            if (lastSeen != null && !current.after(lastSeen)) return;

            String myUid = FirebaseAuth.getInstance().getCurrentUser() != null
                    ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                    : null;
            if (myUid != null && myUid.equals(newest.getCreatedBy())) {
                lastGroupTaskCreatedAt.put(groupId, current);
                return;
            }

            notificationHelper.showGroupTaskNotification(
                    groupId,
                    group.getName() != null ? group.getName() : "Group",
                    newest.getTitle() != null ? newest.getTitle() : "(untitled)"
            );
            lastGroupTaskCreatedAt.put(groupId, current);
        });
    }

    private static GroupMessage newestMessage(@NonNull List<GroupMessage> messages) {
        GroupMessage best = null;
        for (GroupMessage m : messages) {
            if (m == null || m.getTimestamp() == null) continue;
            if (best == null || m.getTimestamp().after(best.getTimestamp())) {
                best = m;
            }
        }
        return best;
    }

    private static GroupTask newestGroupTask(@NonNull List<GroupTask> tasks) {
        GroupTask best = null;
        for (GroupTask t : tasks) {
            if (t == null || t.getCreatedAt() == null) continue;
            if (best == null || t.getCreatedAt().after(best.getCreatedAt())) {
                best = t;
            }
        }
        return best;
    }
}
