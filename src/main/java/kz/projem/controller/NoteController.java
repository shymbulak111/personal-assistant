package kz.projem.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kz.projem.domain.model.User;
import kz.projem.dto.request.NoteRequest;
import kz.projem.dto.response.NoteResponse;
import kz.projem.service.NoteService;
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

import java.util.List;

@RestController
@RequestMapping("/api/v1/notes")
@RequiredArgsConstructor
@Tag(name = "Notes")
@SecurityRequirement(name = "bearerAuth")
public class NoteController {

    private final NoteService noteService;
    private final UserService userService;

    @PostMapping
    public ResponseEntity<NoteResponse> create(@Valid @RequestBody NoteRequest request,
                                               @AuthenticationPrincipal UserDetails principal) {
        User user = userService.getCurrentUser(principal.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(noteService.create(request, user));
    }

    @GetMapping
    public Page<NoteResponse> getAll(@AuthenticationPrincipal UserDetails principal,
                                     @RequestParam(defaultValue = "false") boolean archived,
                                     @RequestParam(required = false) String search,
                                     @PageableDefault(size = 20) Pageable pageable) {
        User user = userService.getCurrentUser(principal.getUsername());
        if (search != null && !search.isBlank()) {
            return noteService.search(user, search, pageable);
        }
        return noteService.getAll(user, archived, pageable);
    }

    @GetMapping("/pinned")
    public List<NoteResponse> getPinned(@AuthenticationPrincipal UserDetails principal) {
        User user = userService.getCurrentUser(principal.getUsername());
        return noteService.getPinned(user);
    }

    @GetMapping("/{id}")
    public ResponseEntity<NoteResponse> getById(@PathVariable Long id,
                                                @AuthenticationPrincipal UserDetails principal) {
        User user = userService.getCurrentUser(principal.getUsername());
        return ResponseEntity.ok(noteService.getById(id, user));
    }

    @PutMapping("/{id}")
    public ResponseEntity<NoteResponse> update(@PathVariable Long id,
                                               @Valid @RequestBody NoteRequest request,
                                               @AuthenticationPrincipal UserDetails principal) {
        User user = userService.getCurrentUser(principal.getUsername());
        return ResponseEntity.ok(noteService.update(id, request, user));
    }

    @PatchMapping("/{id}/pin")
    public ResponseEntity<NoteResponse> togglePin(@PathVariable Long id,
                                                  @AuthenticationPrincipal UserDetails principal) {
        User user = userService.getCurrentUser(principal.getUsername());
        return ResponseEntity.ok(noteService.togglePin(id, user));
    }

    @PatchMapping("/{id}/archive")
    public ResponseEntity<NoteResponse> toggleArchive(@PathVariable Long id,
                                                      @AuthenticationPrincipal UserDetails principal) {
        User user = userService.getCurrentUser(principal.getUsername());
        return ResponseEntity.ok(noteService.toggleArchive(id, user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id,
                                       @AuthenticationPrincipal UserDetails principal) {
        User user = userService.getCurrentUser(principal.getUsername());
        noteService.delete(id, user);
        return ResponseEntity.noContent().build();
    }
}
