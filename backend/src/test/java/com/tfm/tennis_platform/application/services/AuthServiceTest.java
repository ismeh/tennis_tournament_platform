package com.tfm.tennis_platform.application.services;

import com.tfm.tennis_platform.domain.contracts.MemberRepository;
import com.tfm.tennis_platform.domain.models.Member;
import com.tfm.tennis_platform.domain.models.enums.MemberTier;
import com.tfm.tennis_platform.infrastructure.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserDetailsService userDetailsService;

    @InjectMocks
    private AuthService authService;

    @Test
    void loginShouldReturnJwtTokenForValidCredentials() {
        Authentication authenticationResponse = org.mockito.Mockito.mock(Authentication.class);
        User principal = new User("test@example.com", "hashed", List.of());
        UUID memberId = UUID.randomUUID();
        Member member = Member.builder().id(memberId).email("test@example.com").build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authenticationResponse);
        when(authenticationResponse.isAuthenticated()).thenReturn(true);
        when(authenticationResponse.getPrincipal()).thenReturn(principal);
        when(memberRepository.findByEmail("test@example.com")).thenReturn(Optional.of(member));
        when(jwtService.generateAccessToken(principal)).thenReturn("jwt-token");
        when(jwtService.generateRefreshToken(principal)).thenReturn("refresh-token");

        AuthService.AuthTokens token = authService.login("test@example.com", "secret123");

        assertEquals("jwt-token", token.accessToken());
        assertEquals("refresh-token", token.refreshToken());
    }

    @Test
    void registerShouldEncodePasswordPersistMemberAndReturnToken() {
        UUID memberId = UUID.randomUUID();

        when(memberRepository.findByEmail("new@example.com"))
            .thenReturn(Optional.empty());
        when(passwordEncoder.encode("secret123")).thenReturn("encoded-password");

        Member persistedMember = Member.builder()
                .id(memberId)
                .email("new@example.com")
                .password("encoded-password")
                .tier(MemberTier.FREE)
                .build();

        when(memberRepository.save(any(Member.class))).thenReturn(persistedMember);
        when(jwtService.generateAccessToken(any(User.class))).thenReturn("new-user-token");
        when(jwtService.generateRefreshToken(any(User.class))).thenReturn("new-user-refresh-token");

        AuthService.AuthTokens token = authService.register("new@example.com", "secret123", "New User");

        assertEquals("new-user-token", token.accessToken());
        assertEquals("new-user-refresh-token", token.refreshToken());

        ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.forClass(Member.class);
        verify(memberRepository).save(memberCaptor.capture());
        verify(memberRepository).updateTokenHash(eq(memberId), any());
        assertEquals("new@example.com", memberCaptor.getValue().getEmail());
        assertEquals("encoded-password", memberCaptor.getValue().getPassword());
        assertEquals(MemberTier.FREE, memberCaptor.getValue().getTier());
    }

    @Test
    void registerShouldFailWhenEmailAlreadyExists() {
        when(memberRepository.findByEmail("used@example.com")).thenReturn(Optional.of(Member.builder().build()));

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> authService.register("used@example.com", "secret123", "Used User")
        );

        assertEquals("Email already registered", ex.getMessage());
    }

    @Test
    void refreshShouldReturnNewTokenPairWhenRefreshTokenIsValid() {
        User principal = new User("test@example.com", "hashed", List.of());
        UUID memberId = UUID.randomUUID();
        String validRefreshToken = "valid-refresh-token";
        String validRefreshTokenHash = hashToken(validRefreshToken);
        Member member = Member.builder().id(memberId).email("test@example.com").build();
        Member memberWithTokenHash = Member.builder()
            .id(memberId)
            .email("test@example.com")
            .password("hashed")
            .tier(MemberTier.FREE)
            .tokenHash(validRefreshTokenHash)
            .build();

        when(jwtService.extractUsername(validRefreshToken)).thenReturn("test@example.com");
        when(userDetailsService.loadUserByUsername("test@example.com")).thenReturn(principal);
        when(jwtService.isRefreshTokenValid(validRefreshToken, principal)).thenReturn(true);
        when(memberRepository.findByEmail("test@example.com")).thenReturn(Optional.of(memberWithTokenHash));
        when(jwtService.generateAccessToken(principal)).thenReturn("new-access-token");
        when(jwtService.generateRefreshToken(principal)).thenReturn("new-refresh-token");

        AuthService.AuthTokens tokens = authService.refresh(validRefreshToken);

        assertEquals("new-access-token", tokens.accessToken());
        assertEquals("new-refresh-token", tokens.refreshToken());
    }

    @Test
    void refreshShouldFailWhenRefreshTokenIsInvalid() {
        User principal = new User("test@example.com", "hashed", List.of());

        when(jwtService.extractUsername("invalid-refresh-token")).thenReturn("test@example.com");
        when(userDetailsService.loadUserByUsername("test@example.com")).thenReturn(principal);
        when(jwtService.isRefreshTokenValid("invalid-refresh-token", principal)).thenReturn(false);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> authService.refresh("invalid-refresh-token")
        );

        assertEquals("Invalid refresh token", ex.getMessage());
    }

    @Test
    void refreshShouldFailWhenTokenHashIsNotPersisted() {
        User principal = new User("test@example.com", "hashed", List.of());
        UUID memberId = UUID.randomUUID();
        Member member = Member.builder()
            .id(memberId)
            .email("test@example.com")
            .password("hashed")
            .tier(MemberTier.FREE)
            .tokenHash("different-hash")
            .build();

        when(jwtService.extractUsername("valid-refresh-token")).thenReturn("test@example.com");
        when(userDetailsService.loadUserByUsername("test@example.com")).thenReturn(principal);
        when(jwtService.isRefreshTokenValid("valid-refresh-token", principal)).thenReturn(true);
        when(memberRepository.findByEmail("test@example.com")).thenReturn(Optional.of(member));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> authService.refresh("valid-refresh-token")
        );

        assertEquals("Invalid refresh token", ex.getMessage());
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
