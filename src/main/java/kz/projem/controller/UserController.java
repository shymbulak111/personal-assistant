package kz.projem.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import kz.projem.domain.model.User;
import kz.projem.dto.response.UserDataExportResponse;
import kz.projem.service.GdprService;
import kz.projem.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "GDPR — account management and data export")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;
    private final GdprService gdprService;

    @GetMapping("/me/export")
    @Operation(summary = "GDPR — export all personal data as JSON")
    public ResponseEntity<UserDataExportResponse> exportMyData(
            @AuthenticationPrincipal UserDetails principal) {
        User user = userService.getCurrentUser(principal.getUsername());
        return ResponseEntity.ok(gdprService.exportUserData(user));
    }

    @DeleteMapping("/me")
    @Operation(summary = "GDPR — permanently delete account and all associated data")
    public ResponseEntity<Void> deleteMyAccount(
            @AuthenticationPrincipal UserDetails principal) {
        User user = userService.getCurrentUser(principal.getUsername());
        gdprService.deleteAccount(user);
        return ResponseEntity.noContent().build();
    }
}
