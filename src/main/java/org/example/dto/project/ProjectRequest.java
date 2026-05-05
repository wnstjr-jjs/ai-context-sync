package org.example.dto.project;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class ProjectRequest {
    @NotBlank
    private String name;

    private String description;
}
