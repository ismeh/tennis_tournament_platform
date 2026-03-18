package com.tfm.tennis_platform.infrastructure.controller;

import com.tfm.tennis_platform.application.services.AuthService;
import com.tfm.tennis_platform.infrastructure.controller.dto.LoginRequest;
import com.tfm.tennis_platform.infrastructure.controller.dto.LoginResponse;
import com.tfm.tennis_platform.infrastructure.controller.dto.RefreshTokenRequest;
import com.tfm.tennis_platform.infrastructure.controller.dto.RefreshTokenResponse;
import com.tfm.tennis_platform.infrastructure.controller.dto.RegisterRequest;
import com.tfm.tennis_platform.infrastructure.controller.dto.RegisterResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
public class LoginController {
    private final AuthService authService;

    @PostMapping({"/api/login", "/api/auth/login"})
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest loginRequest) {
        AuthService.AuthTokens tokens = authService.login(loginRequest.email(), loginRequest.password());
        log.info("User logged in: {}", loginRequest.email());
        return ResponseEntity.ok(new LoginResponse(tokens.accessToken(), tokens.refreshToken()));
    }

    @PostMapping("/api/auth/register")
    public ResponseEntity<RegisterResponse> register(@RequestBody RegisterRequest registerRequest) {
        AuthService.AuthTokens tokens = authService.register(
                registerRequest.email(),
                registerRequest.password(),
                registerRequest.name()
        );
        log.info("User registered: {}", registerRequest.email());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new RegisterResponse(tokens.accessToken(), tokens.refreshToken()));
    }

    @PostMapping("/api/auth/refresh")
    public ResponseEntity<RefreshTokenResponse> refresh(@RequestBody RefreshTokenRequest request) {
        AuthService.AuthTokens tokens = authService.refresh(request.refreshToken());
        return ResponseEntity.ok(new RefreshTokenResponse(tokens.accessToken(), tokens.refreshToken()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> handleConflict(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleUnauthorized(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ex.getMessage());
    }
}
