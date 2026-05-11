package kz.projem.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kz.projem.domain.model.User;
import kz.projem.dto.request.ReminderRequest;
import kz.projem.dto.response.ReminderResponse;
import kz.projem.service.ReminderService;
import kz.projem.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reminders")
@RequiredArgsConstructor
@Tag(name = "Reminders")
@SecurityRequirement(name = "bearerAuth")
public class ReminderController {

    private final ReminderService reminderService;
    private final UserService userService;

    @PostMapping
    public ResponseEntity<ReminderResponse> create(@Valid @RequestBody ReminderRequest request,
                                                   @AuthenticationPrincipal UserDetails principal) {
        User user = userService.getCurrentUser(principal.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(reminderService.create(request, user));
    }

    @GetMapping
    public Page<ReminderResponse> getAll(@AuthenticationPrincipal UserDetails principal,
                                         @PageableDefault(size = 20) Pageable pageable) {
        User user = userService.getCurrentUser(principal.getUsername());
        return reminderService.getAll(user, pageable);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReminderResponse> getById(@PathVariable Long id,
                                                    @AuthenticationPrincipal UserDetails principal) {
        User user = userService.getCurrentUser(principal.getUsername());
        return ResponseEntity.ok(reminderService.getById(id, user));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ReminderResponse> update(@PathVariable Long id,
                                                   @Valid @RequestBody ReminderRequest request,
                                                   @AuthenticationPrincipal UserDetails principal) {
        User user = userService.getCurrentUser(principal.getUsername());
        return ResponseEntity.ok(reminderService.update(id, request, user));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<Void> cancel(@PathVariable Long id,
                                       @AuthenticationPrincipal UserDetails principal) {
        User user = userService.getCurrentUser(principal.getUsername());
        reminderService.cancel(id, user);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id,
                                       @AuthenticationPrincipal UserDetails principal) {
        User user = userService.getCurrentUser(principal.getUsername());
        reminderService.delete(id, user);
        return ResponseEntity.noContent().build();
    }
}
