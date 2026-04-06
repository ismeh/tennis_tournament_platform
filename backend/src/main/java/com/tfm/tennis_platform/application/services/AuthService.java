package com.tfm.tennis_platform.application.services;

import com.tfm.tennis_platform.domain.port.out.MemberRepository;
import com.tfm.tennis_platform.domain.models.Member;
import com.tfm.tennis_platform.domain.models.enums.MemberTier;
import com.tfm.tennis_platform.domain.exceptions.DuplicateResourceException;
import com.tfm.tennis_platform.domain.exceptions.UnauthorizedException;
import com.tfm.tennis_platform.infrastructure.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserDetailsService userDetailsService;

    public AuthTokens login(String email, String password) {
        Authentication authenticationRequest =
                UsernamePasswordAuthenticationToken.unauthenticated(email, password);
        Authentication authenticationResponse = authenticationManager.authenticate(authenticationRequest);

        if (!authenticationResponse.isAuthenticated()) {
            throw new UnauthorizedException("Credenciales inválidas");
        }

        UserDetails userDetails = (UserDetails) authenticationResponse.getPrincipal();
        Member member = memberRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new UnauthorizedException("Credenciales inválidas"));
        return issueTokens(userDetails, member.getId());
    }

    public AuthTokens register(String email, String password, String name) {
        memberRepository.findByEmail(email).ifPresent(existing -> {
            throw new DuplicateResourceException("Member", "email", email);
        });

        MemberTier tier = MemberTier.FREE;
        String encodedPassword = passwordEncoder.encode(password);

        Member member = Member.builder()
                .email(email)
                .username(name)
                .password(encodedPassword)
                .tier(tier)
                .build();

        Member savedMember = memberRepository.save(member);

        User registeredUser = new User(
                savedMember.getEmail(),
                encodedPassword,
                List.of(new SimpleGrantedAuthority("ROLE_" + tier))
        );

        if (savedMember.getId() == null) {
            throw new UnauthorizedException("No se pudo registrar el usuario");
        }

        return issueTokens(registeredUser, savedMember.getId());
    }

    public AuthTokens refresh(String refreshToken) {
        final String userEmail;
        try {
            userEmail = jwtService.extractUsername(refreshToken);
        } catch (RuntimeException ex) {
            throw new UnauthorizedException("Token de actualización inválido", ex);
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);

        if (!jwtService.isRefreshTokenValid(refreshToken, userDetails)) {
            throw new UnauthorizedException("Token de actualización inválido");
        }

        Member member = memberRepository.findByEmail(userEmail)
            .orElseThrow(() -> new UnauthorizedException("Credenciales inválidas"));
        String refreshTokenHash = hashToken(refreshToken);
        if (!refreshTokenHash.equals(member.getTokenHash())) {
            throw new UnauthorizedException("Token de actualización inválido");
        }

        return issueTokens(userDetails, member.getId());
    }

    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }

        final String userEmail;
        try {
            userEmail = jwtService.extractUsername(refreshToken);
        } catch (RuntimeException ex) {
            return;
        }

        memberRepository.findByEmail(userEmail)
                .map(Member::getId)
                .ifPresent(memberId -> memberRepository.updateTokenHash(memberId, null));
    }

    private AuthTokens issueTokens(UserDetails userDetails, UUID memberId) {
        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        String refreshTokenHash = hashToken(refreshToken);
        persistTokenHash(memberId, refreshTokenHash);

        return new AuthTokens(accessToken, refreshToken);
    }

    private void persistTokenHash(UUID memberId, String tokenHash) {
        memberRepository.updateTokenHash(memberId, tokenHash);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new UnauthorizedException("Algoritmo de hash de token no disponible", ex);
        }
    }

    public record AuthTokens(String accessToken, String refreshToken) {
    }
}
