package org.example.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.dto.context.ContextRequest;
import org.example.dto.context.ContextResponse;
import org.example.service.ContextService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}/context")
@RequiredArgsConstructor
public class ContextController {

    private final ContextService contextService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ContextResponse saveContext(@AuthenticationPrincipal UserDetails user,
                                       @PathVariable Long projectId,
                                       @Valid @RequestBody ContextRequest request) throws Exception {
        return contextService.saveContext(projectId, user.getUsername(), request);
    }

    @GetMapping("/latest")
    public ContextResponse getLatest(@AuthenticationPrincipal UserDetails user,
                                     @PathVariable Long projectId) throws Exception {
        return contextService.getLatestContext(projectId, user.getUsername());
    }

    @GetMapping("/history")
    public List<ContextResponse> getHistory(@AuthenticationPrincipal UserDetails user,
                                            @PathVariable Long projectId) throws Exception {
        return contextService.getHistory(projectId, user.getUsername());
    }

    @DeleteMapping("/{contextId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteContext(@AuthenticationPrincipal UserDetails user,
                              @PathVariable Long projectId,
                              @PathVariable Long contextId) {
        contextService.deleteContext(projectId, contextId, user.getUsername());
    }
}
