package com.student.overcooked.data.repository;

import androidx.lifecycle.LiveData;

import com.student.overcooked.data.dao.ProjectDao;
import com.student.overcooked.data.dao.TeamMemberDao;
import com.student.overcooked.data.model.Project;
import com.student.overcooked.data.model.ProjectWithTasks;
import com.student.overcooked.data.model.TeamMember;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Repository for Project data operations
 * Abstracts data access from the rest of the app
 */
public class ProjectRepository {
    private final ProjectDao projectDao;
    private final TeamMemberDao teamMemberDao;
    private final ExecutorService executorService;

    // Observable LiveData
    private final LiveData<List<Project>> allProjects;
    private final LiveData<List<Project>> activeProjects;
    private final LiveData<List<Project>> completedProjects;
    private final LiveData<List<Project>> teamProjects;
    private final LiveData<List<Project>> individualProjects;
    private final LiveData<Integer> activeProjectCount;
    private final LiveData<List<ProjectWithTasks>> allProjectsWithTasks;
    private final LiveData<List<ProjectWithTasks>> activeProjectsWithTasks;

    public ProjectRepository(ProjectDao projectDao, TeamMemberDao teamMemberDao) {
        this.projectDao = projectDao;
        this.teamMemberDao = teamMemberDao;
        this.executorService = Executors.newSingleThreadExecutor();

        this.allProjects = projectDao.getAllProjects();
        this.activeProjects = projectDao.getActiveProjects();
        this.completedProjects = projectDao.getCompletedProjects();
        this.teamProjects = projectDao.getTeamProjects();
        this.individualProjects = projectDao.getIndividualProjects();
        this.activeProjectCount = projectDao.getActiveProjectCount();
        this.allProjectsWithTasks = projectDao.getAllProjectsWithTasks();
        this.activeProjectsWithTasks = projectDao.getActiveProjectsWithTasks();
    }

    // ================= Observe Projects =================

    public LiveData<List<Project>> getAllProjects() { return allProjects; }
    public LiveData<List<Project>> getActiveProjects() { return activeProjects; }
    public LiveData<List<Project>> getCompletedProjects() { return completedProjects; }
    public LiveData<List<Project>> getTeamProjects() { return teamProjects; }
    public LiveData<List<Project>> getIndividualProjects() { return individualProjects; }
    public LiveData<Integer> getActiveProjectCount() { return activeProjectCount; }
    public LiveData<List<ProjectWithTasks>> getAllProjectsWithTasks() { return allProjectsWithTasks; }
    public LiveData<List<ProjectWithTasks>> getActiveProjectsWithTasks() { return activeProjectsWithTasks; }

    public LiveData<List<Project>> getProjectsByCourse(String course) {
        return projectDao.getProjectsByCourse(course);
    }

    public LiveData<ProjectWithTasks> getProjectWithTasks(long projectId) {
        return projectDao.getProjectWithTasks(projectId);
    }

    // ================= Single Project Operations =================

    public void getProjectById(long projectId, Callback<Project> callback) {
        executorService.execute(() -> {
            Project project = projectDao.getProjectById(projectId);
            callback.onResult(project);
        });
    }

    public void insertProject(Project project, Callback<Long> callback) {
        executorService.execute(() -> {
            long id = projectDao.insertProject(project);
            callback.onResult(id);
        });
    }

    public void updateProject(Project project) {
        executorService.execute(() -> projectDao.updateProject(project));
    }

    public void deleteProject(Project project) {
        executorService.execute(() -> projectDao.deleteProject(project));
    }

    public void deleteProjectById(long projectId) {
        executorService.execute(() -> projectDao.deleteProjectById(projectId));
    }

    public void toggleProjectCompletion(long projectId, boolean isCompleted) {
        executorService.execute(() -> {
            Date completedAt = isCompleted ? new Date() : null;
            projectDao.updateProjectCompletion(projectId, isCompleted, completedAt);
        });
    }

    // ================= Team Member Operations =================

    public LiveData<List<TeamMember>> getMembersByProject(long projectId) {
        return teamMemberDao.getMembersByProject(projectId);
    }

    public LiveData<Integer> getMemberCountByProject(long projectId) {
        return teamMemberDao.getMemberCountByProject(projectId);
    }

    public void addTeamMember(TeamMember member, Callback<Long> callback) {
        executorService.execute(() -> {
            long id = teamMemberDao.insertMember(member);
            callback.onResult(id);
        });
    }

    public void addTeamMembers(List<TeamMember> members) {
        executorService.execute(() -> teamMemberDao.insertMembers(members));
    }

    public void updateTeamMember(TeamMember member) {
        executorService.execute(() -> teamMemberDao.updateMember(member));
    }

    public void removeTeamMember(TeamMember member) {
        executorService.execute(() -> teamMemberDao.deleteMember(member));
    }

    public void removeTeamMemberById(long memberId) {
        executorService.execute(() -> teamMemberDao.deleteMemberById(memberId));
    }

    public void removeAllTeamMembers(long projectId) {
        executorService.execute(() -> teamMemberDao.deleteMembersByProject(projectId));
    }

    /**
     * Callback interface for async operations
     */
    public interface Callback<T> {
        void onResult(T result);
    }
}
