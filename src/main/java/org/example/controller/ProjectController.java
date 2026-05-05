package org.example.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.dto.project.ProjectRequest;
import org.example.dto.project.ProjectResponse;
import org.example.service.ProjectService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping
    public List<ProjectResponse> getProjects(@AuthenticationPrincipal UserDetails user) {
        return projectService.getProjects(user.getUsername());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectResponse createProject(@AuthenticationPrincipal UserDetails user,
                                         @Valid @RequestBody ProjectRequest request) {
        return projectService.createProject(user.getUsername(), request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProject(@AuthenticationPrincipal UserDetails user,
                              @PathVariable Long id) {
        projectService.deleteProject(user.getUsername(), id);
    }
}
