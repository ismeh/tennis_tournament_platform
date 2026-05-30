package com.tfm.tennis_platform.infrastructure.controller;

import com.tfm.tennis_platform.application.commands.CompleteProfileCommand;
import com.tfm.tennis_platform.application.services.AuthService;
import com.tfm.tennis_platform.infrastructure.controller.dto.LoginRequest;
import com.tfm.tennis_platform.infrastructure.controller.dto.LoginResponse;
import com.tfm.tennis_platform.infrastructure.controller.dto.ProfileRequest;
import com.tfm.tennis_platform.infrastructure.controller.dto.ProfileResponse;
import com.tfm.tennis_platform.infrastructure.controller.dto.RefreshTokenRequest;
import com.tfm.tennis_platform.infrastructure.controller.dto.RefreshTokenResponse;
import com.tfm.tennis_platform.infrastructure.controller.dto.RegisterRequest;
import com.tfm.tennis_platform.infrastructure.controller.dto.RegisterResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequiredArgsConstructor
@Slf4j
public class LoginController {
    private final AuthService authService;

    @PostMapping({"/api/auth/login"})
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest loginRequest) {
        AuthService.AuthTokens tokens = authService.login(loginRequest.email(), loginRequest.password());
        log.info("User logged in: {}", loginRequest.email());
        return ResponseEntity.ok(new LoginResponse(tokens.accessToken(), tokens.refreshToken()));
    }

    @PostMapping("/api/auth/register")
    public ResponseEntity<RegisterResponse> register(@RequestBody RegisterRequest registerRequest) {
        AuthService.RegistrationResult result = authService.register(
                registerRequest.email(),
                registerRequest.password(),
                registerRequest.name()
        );
        log.info("User registered: {}", registerRequest.email());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new RegisterResponse(result.emailVerificationRequired(), result.message()));
    }

    @GetMapping("/api/auth/confirm-email")
    public ResponseEntity<RegisterResponse> confirmEmail(@RequestParam String token) {
        AuthService.EmailConfirmationResult result = authService.confirmEmail(token);
        return ResponseEntity.ok(new RegisterResponse(false, result.message()));
    }

    @PostMapping("/api/auth/resend-confirmation")
    public ResponseEntity<RegisterResponse> resendConfirmation(@RequestBody RegisterRequest request) {
        AuthService.EmailConfirmationResult result = authService.resendEmailConfirmation(request.email());
        return ResponseEntity.ok(new RegisterResponse(result.success(), result.message()));
    }

    @PostMapping("/api/auth/refresh")
    public ResponseEntity<RefreshTokenResponse> refresh(@RequestBody RefreshTokenRequest request) {
        AuthService.AuthTokens tokens = authService.refresh(request.refreshToken());
        return ResponseEntity.ok(new RefreshTokenResponse(tokens.accessToken(), tokens.refreshToken()));
    }

    @PostMapping("/api/auth/logout")
    public ResponseEntity<Void> logout(@RequestBody(required = false) RefreshTokenRequest request) {
        authService.logout(request != null ? request.refreshToken() : null);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/auth/profile")
    public ResponseEntity<ProfileResponse> getProfile(Principal principal) {
        AuthService.UserProfile profile = authService.getProfile(principal.getName());
        return ResponseEntity.ok(toProfileResponse(profile));
    }

    @PutMapping("/api/auth/profile")
    public ResponseEntity<ProfileResponse> completeProfile(Principal principal, @RequestBody ProfileRequest request) {
        CompleteProfileCommand command = new CompleteProfileCommand(
                request.firstName(),
                request.lastName(),
                request.gender(),
                request.birthDate(),
                request.nationality(),
                request.federationLicense()
        );
        AuthService.UserProfile profile = authService.completeProfile(principal.getName(), command);
        return ResponseEntity.ok(toProfileResponse(profile));
    }

    private ProfileResponse toProfileResponse(AuthService.UserProfile profile) {
        return new ProfileResponse(
                profile.memberId(),
                profile.email(),
                profile.tier(),
                profile.registeredAt(),
                profile.personId(),
                profile.firstName(),
                profile.lastName(),
                profile.gender(),
                profile.birthDate(),
                profile.nationality(),
                profile.federationLicense()
        );
    }
}
