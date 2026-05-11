package kz.projem.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kz.projem.domain.enums.TaskPriority;
import kz.projem.domain.enums.TaskStatus;
import kz.projem.domain.model.User;
import kz.projem.dto.request.TaskRequest;
import kz.projem.dto.response.TaskResponse;
import kz.projem.service.TaskService;
import kz.projem.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
@Tag(name = "Tasks", description = "Task management")
@SecurityRequirement(name = "bearerAuth")
public class TaskController {

    private final TaskService taskService;
    private final UserService userService;

    @PostMapping
    @Operation(summary = "Create a new task")
    public ResponseEntity<TaskResponse> create(@Valid @RequestBody TaskRequest request,
                                               @AuthenticationPrincipal UserDetails principal) {
        User user = userService.getCurrentUser(principal.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.create(request, user));
    }

    @GetMapping
    @Operation(summary = "Get all tasks (paginated)")
    public Page<TaskResponse> getAll(@AuthenticationPrincipal UserDetails principal,
                                     @RequestParam(required = false) TaskStatus status,
                                     @RequestParam(required = false) TaskPriority priority,
                                     @RequestParam(required = false) String search,
                                     @PageableDefault(size = 20) Pageable pageable) {
        User user = userService.getCurrentUser(principal.getUsername());

        if (search != null && !search.isBlank()) return taskService.search(user, search, pageable);
        if (status != null) return taskService.getByStatus(user, status, pageable);
        if (priority != null) return taskService.getByPriority(user, priority, pageable);

        return taskService.getAll(user, pageable);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskResponse> getById(@PathVariable Long id,
                                                @AuthenticationPrincipal UserDetails principal) {
        User user = userService.getCurrentUser(principal.getUsername());
        return ResponseEntity.ok(taskService.getById(id, user));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ownerCheck.isTaskOwner(#id, authentication)")
    @Operation(summary = "Update task")
    public ResponseEntity<TaskResponse> update(@PathVariable Long id,
                                               @Valid @RequestBody TaskRequest request,
                                               @AuthenticationPrincipal UserDetails principal) {
        User user = userService.getCurrentUser(principal.getUsername());
        return ResponseEntity.ok(taskService.update(id, request, user));
    }

    @PatchMapping("/{id}/complete")
    @PreAuthorize("@ownerCheck.isTaskOwner(#id, authentication)")
    @Operation(summary = "Mark task as completed")
    public ResponseEntity<TaskResponse> complete(@PathVariable Long id,
                                                 @AuthenticationPrincipal UserDetails principal) {
        User user = userService.getCurrentUser(principal.getUsername());
        return ResponseEntity.ok(taskService.complete(id, user));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@ownerCheck.isTaskOwner(#id, authentication)")
    public ResponseEntity<Void> delete(@PathVariable Long id,
                                       @AuthenticationPrincipal UserDetails principal) {
        User user = userService.getCurrentUser(principal.getUsername());
        taskService.delete(id, user);
        return ResponseEntity.noContent().build();
    }
}
