package org.example.dto.context;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class ContextRequest {
    @NotBlank
    private String content;

    private String source;
    private String label;
}
