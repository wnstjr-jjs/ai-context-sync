package org.example.dto.context;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ContextResponse {
    private Long id;
    private String projectName;
    private String content;
    private String source;
    private String label;
    private LocalDateTime savedAt;
    private Integer version;
}
