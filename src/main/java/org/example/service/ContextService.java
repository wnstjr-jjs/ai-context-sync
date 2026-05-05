package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.dto.context.ContextRequest;
import org.example.dto.context.ContextResponse;
import org.example.entity.Context;
import org.example.entity.Project;
import org.example.entity.User;
import org.example.entity.UserPlan;
import org.example.exception.ApiException;
import org.example.exception.ErrorCode;
import org.example.repository.ContextRepository;
import org.example.repository.UserRepository;
import org.example.util.EncryptionUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ContextService {

    private final ContextRepository contextRepository;
    private final ProjectService projectService;
    private final UserRepository userRepository;
    private final EncryptionUtil encryptionUtil;

    @Transactional
    public ContextResponse saveContext(Long projectId, String email, ContextRequest request) throws Exception {
        Project project = projectService.getProjectForUser(projectId, email);

        int nextVersion = contextRepository.findMaxVersionByProjectId(projectId) + 1;
        String encrypted = encryptionUtil.encrypt(request.getContent());

        Context context = Context.builder()
                .project(project)
                .content(encrypted)
                .source(request.getSource())
                .label(request.getLabel())
                .version(nextVersion)
                .build();

        Context saved = contextRepository.save(context);
        return toResponse(saved, project.getName(), request.getContent());
    }

    public ContextResponse getLatestContext(Long projectId, String email) throws Exception {
        projectService.getProjectForUser(projectId, email); // access check

        Context context = contextRepository.findTopByProjectIdOrderByVersionDesc(projectId)
                .orElseThrow(() -> new ApiException(ErrorCode.CONTEXT_NOT_FOUND));

        String decrypted = encryptionUtil.decrypt(context.getContent());
        return toResponse(context, context.getProject().getName(), decrypted);
    }

    public List<ContextResponse> getHistory(Long projectId, String email) throws Exception {
        Project project = projectService.getProjectForUser(projectId, email);
        User user = getUser(email);

        List<Context> contexts;
        if (user.getPlan() == UserPlan.FREE) {
            LocalDateTime cutoff = LocalDateTime.now().minusDays(7);
            contexts = contextRepository.findByProjectIdAndCreatedAtAfterOrderByVersionDesc(projectId, cutoff);
        } else {
            contexts = contextRepository.findByProjectIdOrderByVersionDesc(projectId);
        }

        List<ContextResponse> result = new java.util.ArrayList<>();
        for (Context c : contexts) {
            result.add(toResponse(c, project.getName(), encryptionUtil.decrypt(c.getContent())));
        }
        return result;
    }

    @Transactional
    public void deleteContext(Long projectId, Long contextId, String email) {
        projectService.getProjectForUser(projectId, email); // access check

        Context context = contextRepository.findByIdAndProjectId(contextId, projectId)
                .orElseThrow(() -> new ApiException(ErrorCode.CONTEXT_NOT_FOUND));
        contextRepository.delete(context);
    }

    private ContextResponse toResponse(Context c, String projectName, String plainContent) {
        return ContextResponse.builder()
                .id(c.getId())
                .projectName(projectName)
                .content(plainContent)
                .source(c.getSource())
                .label(c.getLabel())
                .savedAt(c.getCreatedAt())
                .version(c.getVersion())
                .build();
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
    }
}
