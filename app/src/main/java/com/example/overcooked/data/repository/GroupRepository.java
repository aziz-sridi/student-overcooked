package com.example.overcooked.data.repository;

import androidx.lifecycle.LiveData;

import com.example.overcooked.data.dao.GroupDao;
import com.example.overcooked.data.model.Group;
import com.example.overcooked.data.model.GroupMember;
import com.example.overcooked.data.model.GroupMessage;
import com.example.overcooked.data.model.GroupTask;
import com.example.overcooked.data.model.Priority;
import com.example.overcooked.data.model.ProjectInvitation;
import com.example.overcooked.data.model.ProjectResource;
import com.example.overcooked.data.model.ProjectResourceType;
import com.example.overcooked.data.repository.group.GroupInfoDataSource;
import com.example.overcooked.data.repository.group.GroupInvitationDataSource;
import com.example.overcooked.data.repository.group.GroupMemberDataSource;
import com.example.overcooked.data.repository.group.GroupMessageDataSource;
import com.example.overcooked.data.repository.group.GroupResourceDataSource;
import com.example.overcooked.data.repository.group.GroupTasksDataSource;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Repository for managing groups and group-related data.
 * Acts as a facade delegating work to smaller collaborators.
 */
public class GroupRepository {

    private final FirebaseFirestore firestore;
    private final FirebaseAuth auth;
    private final ExecutorService executorService;
    private final StorageReference storageRoot;

    private final com.google.firebase.firestore.CollectionReference groupsCollection;
    private final com.google.firebase.firestore.CollectionReference membersCollection;
    private final com.google.firebase.firestore.CollectionReference groupTasksCollection;
    private final com.google.firebase.firestore.CollectionReference messagesCollection;
    private final com.google.firebase.firestore.CollectionReference usersCollection;
    private final com.google.firebase.firestore.CollectionReference resourcesCollection;
    private final com.google.firebase.firestore.CollectionReference invitationsCollection;

    private final GroupInfoDataSource groupInfoDataSource;
    private final GroupTasksDataSource groupTasksDataSource;
    private final GroupResourceDataSource groupResourceDataSource;
    private final GroupMessageDataSource groupMessageDataSource;
    private final GroupMemberDataSource groupMemberDataSource;
    private final GroupInvitationDataSource groupInvitationDataSource;

    public GroupRepository(GroupDao groupDao) {
        this.firestore = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
        this.executorService = Executors.newSingleThreadExecutor();
        FirebaseStorage storage = FirebaseStorage.getInstance();
        this.storageRoot = storage.getReference();

        this.groupsCollection = firestore.collection("groups");
        this.membersCollection = firestore.collection("group_members");
        this.groupTasksCollection = firestore.collection("group_tasks");
        this.messagesCollection = firestore.collection("group_messages");
        this.usersCollection = firestore.collection("users");
        this.resourcesCollection = firestore.collection("group_resources");
        this.invitationsCollection = firestore.collection("project_invitations");

        this.groupInfoDataSource = new GroupInfoDataSource(
                auth,
                firestore,
                groupDao,
                executorService,
                groupsCollection,
                membersCollection,
                groupTasksCollection,
                messagesCollection,
                resourcesCollection
        );
        this.groupTasksDataSource = new GroupTasksDataSource(
                auth,
                groupsCollection,
                groupTasksCollection,
                groupInfoDataSource::getGroup
        );
        this.groupResourceDataSource = new GroupResourceDataSource(auth, storageRoot, resourcesCollection);
        this.groupMessageDataSource = new GroupMessageDataSource(auth, messagesCollection);
        this.groupMemberDataSource = new GroupMemberDataSource(auth, membersCollection, usersCollection, groupsCollection);
        this.groupInvitationDataSource = new GroupInvitationDataSource(auth, invitationsCollection, usersCollection, membersCollection, groupsCollection);
    }

    public LiveData<List<Group>> getUserGroups() {
        return groupInfoDataSource.getUserGroups();
    }

    public void createGroup(String name, String subject, String description,
                            boolean individualProject, java.util.Date deadline,
                            OnSuccessListener<Group> onSuccess,
                            OnFailureListener onFailure) {
        groupInfoDataSource.createGroup(name, subject, description, individualProject, deadline, onSuccess, onFailure);
    }

    public void joinGroup(String joinCode, OnSuccessListener<Group> onSuccess, OnFailureListener onFailure) {
        groupInfoDataSource.joinGroup(joinCode, onSuccess, onFailure);
    }

    public void getGroup(String groupId, OnSuccessListener<Group> onSuccess, OnFailureListener onFailure) {
        groupInfoDataSource.getGroup(groupId, onSuccess, onFailure);
    }

    public LiveData<List<GroupMember>> getGroupMembers(String groupId) {
        return groupMemberDataSource.getGroupMembers(groupId);
    }

    public LiveData<List<GroupTask>> getGroupTasks(String groupId) {
        return groupTasksDataSource.getGroupTasks(groupId);
    }

    public void createGroupTask(String groupId, String title, String description, java.util.Date deadline,
                                String assigneeId, String assigneeName, Priority priority,
                                OnSuccessListener<GroupTask> onSuccess, OnFailureListener onFailure) {
        groupTasksDataSource.createGroupTask(groupId, title, description, deadline, assigneeId, assigneeName, priority, onSuccess, onFailure);
    }

    public void updateGroupTask(GroupTask task, String title, String description, java.util.Date deadline,
                                String assigneeId, String assigneeName, Priority priority,
                                OnSuccessListener<GroupTask> onSuccess, OnFailureListener onFailure) {
        groupTasksDataSource.updateGroupTask(task, title, description, deadline, assigneeId, assigneeName, priority, onSuccess, onFailure);
    }

    public void deleteGroupTask(GroupTask task, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        groupTasksDataSource.deleteGroupTask(task, onSuccess, onFailure);
    }

    public LiveData<List<ProjectResource>> getProjectResources(String groupId) {
        return groupResourceDataSource.getProjectResources(groupId);
    }

    public void addProjectResource(String groupId, ProjectResourceType type, String title, String content,
                                   String fileName, String fileUrl, long fileSizeBytes, String fileMimeType,
                                   String storagePath,
                                   OnSuccessListener<ProjectResource> onSuccess, OnFailureListener onFailure) {
        groupResourceDataSource.addProjectResource(groupId, type, title, content, fileName, fileUrl, fileSizeBytes, fileMimeType, storagePath, onSuccess, onFailure);
    }

    public void deleteProjectResource(ProjectResource resource, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        groupResourceDataSource.deleteProjectResource(resource, onSuccess, onFailure);
    }

    public void updateGroupDetails(String groupId, String name, String subject, String description,
                                   java.util.Date deadline, boolean individualProject,
                                   OnSuccessListener<Group> onSuccess, OnFailureListener onFailure) {
        groupInfoDataSource.updateGroupDetails(groupId, name, subject, description, deadline, individualProject, onSuccess, onFailure);
    }

    public void deleteGroup(String groupId, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        groupInfoDataSource.deleteGroup(groupId, onSuccess, onFailure);
    }

    public void toggleGroupTaskCompletion(GroupTask task, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        groupTasksDataSource.toggleGroupTaskCompletion(task, onSuccess, onFailure);
    }

    public LiveData<List<GroupMessage>> getGroupMessages(String groupId) {
        return groupMessageDataSource.getGroupMessages(groupId);
    }

    public void sendMessage(String groupId, String message, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        groupMessageDataSource.sendMessage(groupId, message, onSuccess, onFailure);
    }

    public void isGroupAdmin(String groupId, OnSuccessListener<Boolean> onSuccess, OnFailureListener onFailure) {
        groupMemberDataSource.isGroupAdmin(groupId, onSuccess, onFailure);
    }

    public void removeMember(String groupId, String memberId, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        groupMemberDataSource.removeMember(groupId, memberId, onSuccess, onFailure);
    }

    public void addMemberByUsername(String groupId, String username, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        groupMemberDataSource.addMemberByUsername(groupId, username, onSuccess, onFailure);
    }

    // ==================== INVITATION METHODS ====================

    public void sendProjectInvitation(String groupId, String groupName, String usernameOrEmail,
                                      OnSuccessListener<ProjectInvitation> onSuccess, OnFailureListener onFailure) {
        groupInvitationDataSource.sendInvitation(groupId, groupName, usernameOrEmail, onSuccess, onFailure);
    }

    public LiveData<List<ProjectInvitation>> getPendingInvitations() {
        return groupInvitationDataSource.getPendingInvitations();
    }

    public void acceptInvitation(ProjectInvitation invitation, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        groupInvitationDataSource.acceptInvitation(invitation, onSuccess, onFailure);
    }

    public void declineInvitation(ProjectInvitation invitation, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        groupInvitationDataSource.declineInvitation(invitation, onSuccess, onFailure);
    }
}
