package kz.projem.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kz.projem.domain.model.ChatMessage;
import kz.projem.domain.model.ChatSession;
import kz.projem.domain.model.User;
import kz.projem.dto.request.ChatRequest;
import kz.projem.dto.response.ChatResponse;
import kz.projem.service.ChatService;
import kz.projem.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
@Tag(name = "Chat", description = "AI assistant chat")
@SecurityRequirement(name = "bearerAuth")
public class ChatController {

    private final ChatService chatService;
    private final UserService userService;

    @PostMapping
    @Operation(summary = "Send a message to the AI assistant")
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request,
                                             @AuthenticationPrincipal UserDetails principal) {
        User user = userService.getCurrentUser(principal.getUsername());
        return ResponseEntity.ok(chatService.chat(request, user));
    }

    @GetMapping("/sessions")
    @Operation(summary = "List chat sessions")
    public Page<ChatSession> getSessions(@AuthenticationPrincipal UserDetails principal,
                                         @PageableDefault(size = 20) Pageable pageable) {
        User user = userService.getCurrentUser(principal.getUsername());
        return chatService.getSessions(user, pageable);
    }

    @GetMapping("/sessions/{id}/messages")
    public List<ChatMessage> getMessages(@PathVariable Long id,
                                         @AuthenticationPrincipal UserDetails principal) {
        User user = userService.getCurrentUser(principal.getUsername());
        return chatService.getMessages(id, user);
    }

    @DeleteMapping("/sessions/{id}")
    public ResponseEntity<Void> deleteSession(@PathVariable Long id,
                                              @AuthenticationPrincipal UserDetails principal) {
        User user = userService.getCurrentUser(principal.getUsername());
        chatService.deleteSession(id, user);
        return ResponseEntity.noContent().build();
    }
}
