package com.example.overcooked.ui.groupdetail;

import android.text.TextUtils;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.overcooked.R;
import com.example.overcooked.data.model.GroupMessage;
import com.example.overcooked.data.repository.GroupRepository;
import com.example.overcooked.ui.adapter.GroupChatAdapter;

import java.util.List;

/**
 * Handles chat list rendering and message sending logic for group detail.
 */
public class GroupChatController {

    private final Fragment fragment;
    private final GroupRepository groupRepository;
    private final String groupId;
    private final RecyclerView chatRecycler;
    private final EditText messageInput;
    private final ImageButton sendButton;

    private final GroupChatAdapter chatAdapter = new GroupChatAdapter();

    public GroupChatController(@NonNull Fragment fragment,
                               @NonNull GroupRepository groupRepository,
                               @NonNull String groupId,
                               @Nullable RecyclerView chatRecycler,
                               @Nullable EditText messageInput,
                               @Nullable ImageButton sendButton) {
        this.fragment = fragment;
        this.groupRepository = groupRepository;
        this.groupId = groupId;
        this.chatRecycler = chatRecycler;
        this.messageInput = messageInput;
        this.sendButton = sendButton;
        if (this.chatRecycler != null) {
            LinearLayoutManager manager = new LinearLayoutManager(fragment.requireContext());
            manager.setStackFromEnd(true);
            this.chatRecycler.setLayoutManager(manager);
            this.chatRecycler.setAdapter(chatAdapter);
        }
        if (this.sendButton != null) {
            this.sendButton.setOnClickListener(v -> sendMessage());
        }
    }

    public void submitMessages(@Nullable List<GroupMessage> messages) {
        chatAdapter.submitList(messages);
        if (messages != null && !messages.isEmpty() && chatRecycler != null) {
            chatRecycler.scrollToPosition(messages.size() - 1);
        }
    }

    private void sendMessage() {
        if (messageInput == null) {
            return;
        }
        String message = messageInput.getText().toString().trim();
        if (TextUtils.isEmpty(message)) {
            return;
        }
        groupRepository.sendMessage(groupId, message,
            aVoid -> fragment.requireActivity().runOnUiThread(() -> messageInput.setText("")),
            e -> fragment.requireActivity().runOnUiThread(() ->
                Toast.makeText(fragment.requireContext(), "Failed to send message", Toast.LENGTH_SHORT).show()));
    }
}
