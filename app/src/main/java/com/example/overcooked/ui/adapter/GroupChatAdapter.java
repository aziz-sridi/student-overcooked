package com.example.overcooked.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.overcooked.R;
import com.example.overcooked.data.model.GroupMessage;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Objects;

/**
 * Adapter for Group Chat Messages
 */
public class GroupChatAdapter extends ListAdapter<GroupMessage, RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;

    private final String currentUserId;

    private static final DiffUtil.ItemCallback<GroupMessage> DIFF_CALLBACK = new DiffUtil.ItemCallback<GroupMessage>() {
        @Override
        public boolean areItemsTheSame(@NonNull GroupMessage oldItem, @NonNull GroupMessage newItem) {
            return Objects.equals(oldItem.getId(), newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull GroupMessage oldItem, @NonNull GroupMessage newItem) {
            return Objects.equals(oldItem.getMessage(), newItem.getMessage());
        }
    };

    public GroupChatAdapter() {
        super(DIFF_CALLBACK);
        this.currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null 
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
    }

    @Override
    public int getItemViewType(int position) {
        GroupMessage message = getItem(position);
        if (Objects.equals(message.getSenderId(), currentUserId)) {
            return VIEW_TYPE_SENT;
        } else {
            return VIEW_TYPE_RECEIVED;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_SENT) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_chat_sent, parent, false);
            return new SentMessageViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_chat_received, parent, false);
            return new ReceivedMessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        GroupMessage message = getItem(position);
        if (holder instanceof SentMessageViewHolder) {
            ((SentMessageViewHolder) holder).bind(message);
        } else if (holder instanceof ReceivedMessageViewHolder) {
            ((ReceivedMessageViewHolder) holder).bind(message);
        }
    }

    static class SentMessageViewHolder extends RecyclerView.ViewHolder {
        private final TextView messageText;
        private final TextView timeText;

        private final SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());

        SentMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.messageText);
            timeText = itemView.findViewById(R.id.timeText);
        }

        void bind(GroupMessage message) {
            messageText.setText(message.getMessage());
            if (message.getTimestamp() != null) {
                timeText.setText(timeFormat.format(message.getTimestamp()));
            }
        }
    }

    static class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {
        private final TextView senderName;
        private final TextView messageText;
        private final TextView timeText;
        private final TextView senderInitials;

        private final SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());

        ReceivedMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            senderName = itemView.findViewById(R.id.senderName);
            messageText = itemView.findViewById(R.id.messageText);
            timeText = itemView.findViewById(R.id.timeText);
            senderInitials = itemView.findViewById(R.id.senderInitials);
        }

        void bind(GroupMessage message) {
            senderName.setText(message.getSenderName());
            messageText.setText(message.getMessage());
            if (message.getTimestamp() != null) {
                timeText.setText(timeFormat.format(message.getTimestamp()));
            }

            // Bind initials
            String name = message.getSenderName() != null ? message.getSenderName() : "";
            String[] parts = name.split(" ");
            StringBuilder initials = new StringBuilder();
            for (int i = 0; i < Math.min(2, parts.length); i++) {
                if (!parts[i].isEmpty()) {
                    initials.append(Character.toUpperCase(parts[i].charAt(0)));
                }
            }
            if (senderInitials != null) {
                senderInitials.setText(initials.toString());
            }
        }
    }
}
