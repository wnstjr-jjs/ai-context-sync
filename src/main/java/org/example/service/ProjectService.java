package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.dto.project.ProjectRequest;
import org.example.dto.project.ProjectResponse;
import org.example.entity.Project;
import org.example.entity.User;
import org.example.entity.UserPlan;
import org.example.exception.ApiException;
import org.example.exception.ErrorCode;
import org.example.repository.ProjectRepository;
import org.example.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private static final int FREE_PROJECT_LIMIT = 3;

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    public List<ProjectResponse> getProjects(String email) {
        User user = getUser(email);
        return projectRepository.findByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(ProjectResponse::from)
                .toList();
    }

    @Transactional
    public ProjectResponse createProject(String email, ProjectRequest request) {
        User user = getUser(email);

        if (user.getPlan() == UserPlan.FREE) {
            long count = projectRepository.countByUser(user);
            if (count >= FREE_PROJECT_LIMIT) {
                throw new ApiException(ErrorCode.FREE_PLAN_LIMIT_EXCEEDED);
            }
        }

        Project project = Project.builder()
                .user(user)
                .name(request.getName())
                .description(request.getDescription())
                .build();

        return ProjectResponse.from(projectRepository.save(project));
    }

    @Transactional
    public void deleteProject(String email, Long projectId) {
        User user = getUser(email);
        Project project = projectRepository.findByIdAndUser(projectId, user)
                .orElseThrow(() -> new ApiException(ErrorCode.PROJECT_NOT_FOUND));
        projectRepository.delete(project);
    }

    public Project getProjectForUser(Long projectId, String email) {
        User user = getUser(email);
        return projectRepository.findByIdAndUser(projectId, user)
                .orElseThrow(() -> new ApiException(ErrorCode.PROJECT_NOT_FOUND));
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
    }
}
