package com.gymhub.controller;

import com.gymhub.domain.user.User;
import com.gymhub.domain.user.UserRole;
import com.gymhub.dto.request.LoginRequest;
import com.gymhub.dto.request.RegisterRequest;
import com.gymhub.dto.response.AuthResponse;
import com.gymhub.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Register, login, and multi-role management")
public class AuthController {

    private final AuthService authService;

    /**
     * Register a new unified user account.
     * The caller chooses an initial role (SERVICE_PROVIDER, EMPLOYEE, or CUSTOMER).
     * A JWT is returned immediately so the user can start using the app.
     */
    @PostMapping("/register")
    @Operation(summary = "Create a new account and receive a JWT token")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.register(request));
    }

    /**
     * Login with email + password.
     * Returns access token + refresh token.
     */
    @PostMapping("/login")
    @Operation(summary = "Login with email and password — returns JWT access + refresh tokens")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * Add an additional role to the currently authenticated user.
     * The principal is the full {@link User} entity injected by the JWT filter.
     *
     * Example: a gym owner (SERVICE_PROVIDER) can also add the CUSTOMER role
     * to use another gym as a member — all under the same account.
     */
    @PostMapping("/add-role")
    @Operation(summary = "Add an additional role to the authenticated user's account")
    public ResponseEntity<AuthResponse> addRole(
            @AuthenticationPrincipal User currentUser,
            @RequestParam UserRole role) {
        return ResponseEntity.ok(authService.addRole(currentUser.getId(), role));
    }

    /**
     * Get the profile of the currently authenticated user.
     */
    @GetMapping("/me")
    @Operation(summary = "Get current user's profile and active roles")
    public ResponseEntity<AuthResponse> me(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(authService.getProfile(currentUser));
    }
}
