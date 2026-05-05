package org.example.dto.project;

import lombok.Builder;
import lombok.Getter;
import org.example.entity.Project;

import java.time.LocalDateTime;

@Getter
@Builder
public class ProjectResponse {
    private Long id;
    private String name;
    private String description;
    private LocalDateTime createdAt;

    public static ProjectResponse from(Project p) {
        return ProjectResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .description(p.getDescription())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
