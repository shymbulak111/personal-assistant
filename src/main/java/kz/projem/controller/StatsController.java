package kz.projem.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import kz.projem.domain.model.User;
import kz.projem.dto.response.StatsResponse;
import kz.projem.service.StatsService;
import kz.projem.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/stats")
@RequiredArgsConstructor
@Tag(name = "Statistics")
@SecurityRequirement(name = "bearerAuth")
public class StatsController {

    private final StatsService statsService;
    private final UserService userService;

    @GetMapping
    @Operation(summary = "Get dashboard statistics for current user")
    public ResponseEntity<StatsResponse> getStats(@AuthenticationPrincipal UserDetails principal) {
        User user = userService.getCurrentUser(principal.getUsername());
        return ResponseEntity.ok(statsService.getStats(user));
    }
}
